# Astrolabe Wire Protocol 2.0

## 1. Scope

Astrolabe Wire Protocol 2.0 defines the language-independent contract between
an Astrolabe Host and an in-process platform Runtime. It defines framing,
session negotiation, requests, responses, normalized UI facts, temporary
attribute patches, compatibility rules, and extension namespaces.

The protocol does not define Runtime discovery, TCP ports, USB forwarding,
ADB, screenshots, CLI commands, MCP tools, UIKit, Auto Layout, Android View, or
Compose implementation details. A transport only needs to provide a reliable,
ordered byte stream.

`Contract/v2/PROTOCOL.md`, `Contract/v2/Schemas`, and `Contract/v2/manifest.json` are normative.
Swift and Kotlin source code are implementations of this contract, not its
definition.

## 2. Framing and JSON

Each message is one UTF-8 JSON object prefixed by a four-byte unsigned payload
length in network byte order:

```text
0                   1                   2                   3
0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|          UTF-8 JSON payload length, UInt32 big-endian        |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                         JSON payload ...                      |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

- The length excludes the four-byte prefix.
- Empty payloads are invalid.
- A peer must reject a declared payload larger than 16 MiB before allocating
  the payload buffer.
- Payloads must be valid UTF-8 JSON objects without a byte-order mark.
- Duplicate object member names, non-finite numbers, and trailing data are
  invalid.
- JSON member names and string identifiers are case-sensitive.
- Integers that cross the JavaScript boundary are limited to the safe integer
  range `[-9007199254740991, 9007199254740991]`.

The frame format does not identify requests and responses. Direction does:
the Host sends requests and the Runtime sends responses.

## 3. Session lifecycle

1. The Host opens an ordered byte stream.
2. The first request must be `handshake`, encoded as protocol `2.0`.
3. The Runtime selects the highest version in the intersection of both peers'
   ranges. Version ranges must not span multiple major versions.
4. The Runtime returns its identity, platform, selected protocol version, and
   capabilities.
5. The Host invokes only methods covered by advertised capabilities.
6. Either peer may close the stream. Closing the stream ends in-flight work,
   but temporary patches remain active until reverted, cleared, or the Runtime
   process ends.

Requests before a successful handshake fail with `handshakeRequired`. A failed
handshake does not establish a session. Protocol 2.0 has no Protocol 1.0
fallback or dual-version DTO.

## 4. Message envelopes

### 4.1 Request

| Field | Type | Meaning |
| --- | --- | --- |
| `requestID` | UUID string | Correlates request, response, and cancellation |
| `protocolVersion` | `{major, minor}` | Encoding contract used by this message |
| `method` | String | Case-sensitive operation identifier |
| `parameters` | Object | Method-specific parameters |

### 4.2 Response

Responses repeat `method` so a decoder can validate and route a response
without retaining the original request payload.

| Field | Type | Meaning |
| --- | --- | --- |
| `requestID` | UUID string | Identifier copied from the request |
| `protocolVersion` | `{major, minor}` | Negotiated encoding contract |
| `method` | String | Method copied from the request |
| `status` | `success` or `failure` | Selects payload or error outcome |
| `payload` | Object | Required only for success |
| `error` | Error object | Required only for failure |

A success response must not contain `error`. A failure response must not
contain `payload`. A response with a different request ID or method is not a
response to the in-flight request.

## 5. Methods and capabilities

| Method | Parameters | Success payload | Required capability |
| --- | --- | --- | --- |
| `handshake` | Handshake parameters | Handshake result | Always available |
| `applicationInfo` | Empty object | Application, target, environment | `applicationInfo` |
| `hierarchySnapshot` | Empty object | Immutable hierarchy snapshot | `hierarchySnapshot` |
| `nodeDetail` | Node ID | Detail sections | `nodeDetail` |
| `patchableAttributes` | Empty object | Runtime patch catalog | `attributePatchDiscovery` |
| `applyAttributePatch` | Node, attribute, value | Active patch | `attributePatching` |
| `listAttributePatches` | Empty object | Active patches | `attributePatching` |
| `revertAttributePatch` | Patch ID | Revert result | `attributePatching` |
| `clearAttributePatches` | Empty object | Clear result | `attributePatching` |
| `cancelRequest` | Target request ID | Cancellation result | `requestCancellation` |

Capabilities describe Runtime wire operations only. Host-local screenshots,
baselines, visual diff, semantic compression, and snapshot storage are not
Runtime capabilities.

## 6. Identity and lifecycle

- `target.identifier` identifies the inspected process instance. It is opaque
  and changes when the App process restarts.
- `nodeID` is an opaque, non-empty string scoped to one Runtime process. It is
  not a memory address and must not be parsed as a number.
- `snapshotID` identifies one immutable hierarchy capture. Child ordering,
  sibling indices, and node facts are stable inside that snapshot.
- `patchID` identifies one active temporary patch in the Runtime process.
- Host page snapshot IDs, pagination IDs, app IDs, and baseline identities are
  Host concepts and do not cross this wire contract.

## 7. Application, target, and environment

`applicationInfo` separates four facts:

- `application`: platform-neutral application identifier, display name,
  release version, and build version.
- `target`: opaque process-instance identity, process identifier, target kind,
  and primary-target status.
- `environment`: platform, OS, device category/model, virtual-device status,
  locale, layout direction, and display metadata.
- `extensions`: optional namespaced platform facts.

Connection kind, TCP host, port, usbmux device ID, ADB serial, and discovery
diagnostics are excluded.

## 8. Hierarchy and node facts

Every node contains:

- opaque node and parent identifiers, sibling index, and ordered children;
- normalized semantic role and platform-reported runtime type names;
- local bounds, parent frame, and screen frame;
- explicit visibility causes and final onscreen result;
- clipping, background, text preview, accessibility, interaction state;
- available detail categories and namespaced extensions.

`runtimeType` values may contain names such as `UILabel` or an Android class,
but these are observed strings, not protocol vocabulary. Common Host logic
must not infer platform-independent behavior solely from those values.

The normalized `role` vocabulary starts with:

`window`, `container`, `text`, `image`, `button`, `input`, `scroll`, `list`,
`listItem`, `toggle`, `slider`, `progress`, `tab`, `toolbar`, `navigationBar`,
`alert`, and `other`.

Roles are open strings. A Runtime maps native objects or semantic nodes to the
closest common role and uses `other` only when no common role is accurate. New
roles added in a later minor version must not make an older decoder fail.

### 8.1 Coordinates and units

Every rectangle includes `coordinateSpace` and `unit`.
`frameInParent` is explicitly `null` for hierarchy roots because no parent
coordinate space exists; implementations must not substitute screen or viewport
coordinates under the parent label.

| Coordinate space | Meaning |
| --- | --- |
| `local` | Node-local coordinates |
| `parent` | Direct parent coordinates |
| `screen` | Full display coordinates |
| `viewport` | Inspected App viewport coordinates |

| Unit | Meaning |
| --- | --- |
| `logical` | Platform logical layout unit, such as iOS point or Android dp |
| `pixel` | Physical screenshot/display pixel |

The snapshot display metadata provides logical size, pixel size, and X/Y
logical-to-pixel scale. Consumers must use this metadata instead of device
model heuristics. Width and height cannot be negative.

### 8.2 Visibility

Visibility keeps independent causes instead of overloading one Boolean:

- `hidden`: the node explicitly hides itself.
- `hiddenByAncestor`: an ancestor explicitly hides the node.
- `opacity`: node opacity in `[0, 1]`.
- `effectiveOpacity`: opacity after ancestor effects in `[0, 1]`.
- `intersectsViewport`: the screen frame intersects the App viewport.
- `fullyClippedByAncestor`: ancestor clipping fully removes the node.
- `onscreen`: final Runtime result.

`onscreen` is true only when the node is not hidden, is not hidden by an
ancestor, has effective opacity greater than `0.01`, intersects the viewport,
and is not fully clipped by an ancestor.

## 9. Node details and attributes

Node detail is grouped into sections. Section categories and attribute
identifiers are open, namespaced strings:

- Protocol-owned normalized identifiers start with `common.*`.
- iOS extensions start with `ios.*`, normally `ios.uikit.*`.
- Android View extensions start with `android.view.*`.
- Android Compose extensions start with `android.compose.*`.
- A platform Runtime owns its platform vocabulary. This repository owns only
  namespace syntax and normalized `common.*` identifiers.

Unknown namespaced identifiers must be preserved. Consumers may ignore an
unknown attribute value for semantic checks, but must not reject the complete
node detail solely because the identifier is unknown.

### 9.1 Common detail vocabulary

Protocol-owned detail categories are:

| Category | Scope |
| --- | --- |
| `common.layout` | Geometry, size, insets, normalized layout relations |
| `common.visibility` | Hidden, opacity, clipping, viewport and onscreen facts |
| `common.visual` | Color, content scaling, corner, border and shadow |
| `common.text` | Text content, font, line and alignment facts |
| `common.image` | Image presence and normalized image metadata |
| `common.interaction` | Enabled, selected, highlighted and focus state |
| `common.input` | Editable text and input configuration |
| `common.scroll` | Scroll content geometry and paging/indicator state |
| `common.container` | Axis, alignment, distribution and spacing |
| `common.accessibility` | Accessibility identity, text, traits and state |

The initial normalized attribute vocabulary is:

| Identifier | Value type | Notes |
| --- | --- | --- |
| `common.layout.bounds` | `rect` | Local/logical rectangle |
| `common.layout.frameInParent` | `rect` or `null` | Parent/logical; null for roots |
| `common.layout.frameInScreen` | `rect` | Screen/logical rectangle |
| `common.layout.safeAreaInsets` | `insets` | Logical unit |
| `common.layout.intrinsicContentSize` | `size` | Logical unit |
| `common.layout.relations` | `layoutRelations` | Array of normalized layout relations |
| `common.visibility.hidden` | `boolean` | Explicit node state |
| `common.visibility.hiddenByAncestor` | `boolean` | Ancestor-derived state |
| `common.visibility.opacity` | `number` | Range `[0, 1]` |
| `common.visibility.effectiveOpacity` | `number` | Range `[0, 1]` |
| `common.visibility.intersectsViewport` | `boolean` | Geometry-derived fact |
| `common.visibility.fullyClippedByAncestor` | `boolean` | Ancestor clipping result |
| `common.visibility.onscreen` | `boolean` | Final derived visibility |
| `common.visibility.clipsContent` | `boolean` | Descendant clipping policy |
| `common.visual.backgroundColor` | `color` | Resolved color |
| `common.visual.tintColor` | `color` | Resolved tint when meaningful |
| `common.visual.contentMode` | `string` | `fit`, `fill`, `stretch`, `center`, or open future value |
| `common.visual.cornerRadius` | `measurement` | Logical unit |
| `common.visual.borderColor` | `color` | Resolved border color |
| `common.visual.borderWidth` | `measurement` | Logical unit |
| `common.visual.shadowColor` | `color` | Resolved shadow color |
| `common.visual.shadowOpacity` | `number` | Range `[0, 1]` |
| `common.visual.shadowRadius` | `measurement` | Logical unit |
| `common.visual.shadowOffset` | `vector` | Logical signed offset; negative components are allowed |
| `common.text.content` | `string` | Plain-text projection |
| `common.text.fontName` | `string` | Platform-reported font name |
| `common.text.fontFamilyName` | `string` | Platform-reported family |
| `common.text.fontSize` | `measurement` | `scaledLogical` unit |
| `common.text.numberOfLines` | `integer` | Zero may mean unlimited |
| `common.text.color` | `color` | Resolved text color |
| `common.text.alignment` | `string` | Normalized/open alignment value |
| `common.text.lineBreakMode` | `string` | Normalized/open wrapping value |
| `common.text.adjustsSizeToFit` | `boolean` | Text auto-scaling state |
| `common.text.runs` | `textRuns` | Range, text, font and color per run |
| `common.image.present` | `boolean` | Whether image content exists |
| `common.image.name` | `string` or `null` | Runtime-known resource name |
| `common.image.size` | `size` | Logical unit |
| `common.image.scale` | `number` | Source image pixel scale |
| `common.interaction.enabled` | `boolean` | Control/semantic enabled state |
| `common.interaction.selected` | `boolean` | Selected state |
| `common.interaction.highlighted` | `boolean` | Highlighted/pressed state |
| `common.interaction.focused` | `boolean` | Current focus state |
| `common.input.text` | `string` | Current editable text when permitted |
| `common.input.placeholder` | `string` or `null` | Placeholder text |
| `common.input.secure` | `boolean` | Secure-entry state; content may be omitted |
| `common.input.keyboardType` | `string` | Open normalized input type |
| `common.scroll.contentSize` | `size` | Logical unit |
| `common.scroll.contentOffset` | `point` | Local/logical coordinate |
| `common.scroll.contentInsets` | `insets` | Logical unit |
| `common.scroll.adjustedContentInsets` | `insets` | Logical unit |
| `common.scroll.enabled` | `boolean` | Scrollability |
| `common.scroll.pagingEnabled` | `boolean` | Paging state |
| `common.scroll.showsHorizontalIndicator` | `boolean` | Indicator state |
| `common.scroll.showsVerticalIndicator` | `boolean` | Indicator state |
| `common.container.axis` | `string` | Horizontal, vertical, or open future value |
| `common.container.alignment` | `string` | Open normalized alignment |
| `common.container.distribution` | `string` | Open normalized distribution |
| `common.container.spacing` | `measurement` | Logical unit |
| `common.accessibility.element` | `boolean` | Accessibility participation |
| `common.accessibility.identifier` | `string` or `null` | Developer identifier |
| `common.accessibility.label` | `string` or `null` | Assistive label |
| `common.accessibility.value` | `string` or `null` | Assistive value |
| `common.accessibility.hint` | `string` or `null` | Assistive hint |
| `common.accessibility.traits` | `stringList` | Open normalized traits |

V1 facts without a sound cross-platform meaning remain platform extensions.
For iOS these include content-hugging/compression priorities, autoresizing-mask
translation, raw Auto Layout constraints, UIKit tint-adjustment mode, native
image rendering mode, control alignment/insets, and arbitrary view tags. They
use `ios.uikit.*`; they are not renamed to `common.*` merely to remove an iOS
prefix.

`common.visual.contentMode` has four initial normalized values: `fit` preserves
aspect ratio and keeps all content visible; `fill` preserves aspect ratio and
may crop; `stretch` changes aspect ratio to fill the bounds; `center` keeps the
content's intrinsic size and centers it. Platform-specific rendering modes that
cannot preserve these meanings remain extensions.

### 9.2 Extensible attribute values

Every value uses `{ "type": String, "value": JSON }`. Protocol-owned types are:

`null`, `boolean`, `integer`, `number`, `string`, `stringList`, `measurement`,
`point`, `size`, `vector`, `rect`, `insets`, `color`, `textRuns`,
`layoutRelations`, `array`, and `object`.

`measurement` carries `{value, unit}` and adds `scaledLogical` for text metrics
such as Android sp or an iOS dynamic-type-resolved font size. Standalone
`point`, `size`, `vector`, `rect`, and `insets` values also carry their
coordinate space or unit. `size` dimensions are nonnegative; `vector` uses
signed `dx` and `dy`. No geometric or typographic measurement relies on an
implicit unit.

`textRuns` ranges use zero-based UTF-16 code-unit offsets and lengths. This is
the shared indexing model of Foundation strings and JVM strings; implementations
must not reinterpret ranges as Unicode scalar, grapheme-cluster, or UTF-8 byte
indices.

An extension type must be namespaced. Unknown namespaced types retain their raw
JSON value. Implementations must not coerce integer and floating-point values,
colors, coordinate units, or null into guessed defaults.

### 9.3 Normalized layout relations

`common.layout.relations` may carry relations between node anchors. It expresses
source/target node IDs, normalized anchors, relation, multiplier, logical-unit
offset, normalized strength, and active state. Auto Layout priority,
LayoutParams, and Modifier details remain platform extensions.

## 10. Temporary attribute patches

Patching validates UI hypotheses; it is not a general write channel.

- The Host must fetch the Runtime-owned catalog before applying a patch.
- A patch may target only an advertised attribute pattern and compatible value
  type.
- Numeric catalog bounds apply to a `number`, `integer`, or the inner `value`
  of a `measurement`; they never apply to an encoded object as a whole.
- Catalog applicability uses semantic node roles, not UIKit or Android class
  names. Platform-specific applicability may be namespaced in `extensions`.
- Applying another value to the same node and attribute reuses the active patch
  and preserves the original pre-patch value.
- Revert restores the original value. Clear attempts all active patches.
- Patches survive Host reconnection, but disappear when the Runtime process
  ends. They do not persist to source code, storage, or a future App launch.
- Arbitrary method invocation and business-state mutation are forbidden.

## 11. Errors

Errors contain a stable `code`, diagnostic `message`, nullable
`recoverySuggestion`, optional structured `details`, and optional namespaced
`extensions`. Common codes are:

`malformedFrame`, `frameTooLarge`, `malformedMessage`,
`unsupportedProtocolVersion`, `unsupportedMethod`, `handshakeRequired`,
`capabilityUnavailable`, `invalidParameters`, `nodeNotFound`,
`unsupportedAttribute`, `invalidAttributeValue`, `patchNotFound`,
`patchConflict`, `patchRestorationFailed`, `tooManyRequests`,
`requestCancelled`, `requestTimedOut`, and `internalFailure`.

Platform-specific codes must be namespaced. Platform-native error text or codes
belong in diagnostics, not common control flow.

## 12. Compatibility and unknown data

- Major version changes are incompatible.
- A minor version may add optional fields, capabilities, methods, common
  identifiers, or namespaced extensions without changing existing meaning.
- Unknown object members are ignored when decoding and preserved when an
  implementation supports lossless forwarding.
- A request with an unknown method is decoded as a structurally valid request
  envelope, then answered with `unsupportedMethod`; it is not treated as a
  malformed message or a reason to close the session.
- Unknown capabilities and namespaced identifiers are preserved as strings.
- Unknown namespaced attribute value types preserve their raw JSON value.
- Unknown closed discriminators such as response `status`, coordinate `unit`,
  or a protocol-owned value type are rejected.
- Missing data is represented by an absent optional member. Explicit `null`
  is used only where the Schema declares that null is meaningful.
- Fields removed from V1 are not accepted as V2 aliases.

## 13. V1 fact migration

| V1 fact | V2 representation |
| --- | --- |
| `bundleIdentifier` | `application.identifier` |
| numeric `processIdentifier` | opaque string `target.processIdentifier` |
| numeric `nodeID` | opaque string node ID |
| `kind` | normalized `role`; original type remains in `runtimeType` |
| `className` / `classChain` | `runtimeType.name` / `runtimeType.ancestors` |
| implicit point rectangles | explicit `coordinateSpace` + `unit` rectangles |
| `hidden`, ancestor hidden, alpha | separate V2 visibility causes |
| `isVisible` | `visibility.onscreen` |
| UIKit/Auto Layout identifiers | `ios.uikit.*` extensions or normalized `common.*` facts |
| V1 constraint payload | normalized layout relation plus optional iOS extension |
| closed attribute value enum | open tagged value container |
| Runtime endpoint and port ranges | removed; owned by platform transport modules |

This mapping preserves every V1 product fact needed by hierarchy inspection,
node detail, style/layout checks, temporary patches, and screenshot coordinate
mapping without retaining V1 platform naming.

## 14. Language implementation rules

| Wire value | Swift implementation | Kotlin implementation |
| --- | --- | --- |
| Opaque ID | `String` value object | Inline/value class wrapping `String` |
| Protocol version | Unsigned or range-checked integer fields | Range-checked `Int` fields |
| Safe integer attribute | Range-checked `Int64` | Range-checked `Long` |
| Open identifier | String value object with known constants | String value class with known constants |
| Closed discriminator | Exhaustive enum | Exhaustive enum/sealed type |
| Attribute value | Tagged value plus unknown raw JSON fallback | Tagged sealed type plus unknown `JsonElement` fallback |
| Nullable field | Explicit optional value only where Schema allows null | Nullable type only where Schema allows null |
| Unknown object member | Ignore or preserve for forwarding | Ignore or preserve for forwarding |

Implementations must not model `nodeID` as `UInt64`, `Long`, or JavaScript
`number`. Kotlin conformance is defined by Schema and Fixture results; it does
not reproduce Swift type names or Swift coding behavior.

## 15. Conformance assets

Schemas use JSON Schema Draft 2020-12. `Contract/v2/manifest.json` lists every
method, Schema reference, valid Fixture, invalid Fixture, expected outcome, and
rejection reason. A conforming implementation must:

1. accept every case marked `valid`;
2. reject every case marked `invalid` for the declared contract rule;
3. preserve unknown namespaced identifiers and values;
4. encode output that validates against the same Schema;
5. reject semantic rules that JSON Schema cannot compare, including descending
   version ranges and inconsistent derived visibility.
