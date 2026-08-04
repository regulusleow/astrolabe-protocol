# Astrolabe Wire Protocol 1.0

## Transport framing

Astrolabe exchanges independent JSON messages over an ordered byte stream.
Each message uses this frame:

```text
0                   1                   2                   3
0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|          UTF-8 JSON payload length, UInt32 big-endian        |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                         JSON payload ...                      |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

The length excludes the four-byte header. A peer must reject a frame larger
than its configured payload limit before allocating the declared payload.

## Session lifecycle

1. The client opens a transport connection.
2. The first request is `handshake` encoded with protocol `1.0`.
3. Both peers select the highest common protocol version.
4. The server returns its platform, process identifier and capabilities.
5. The client only invokes methods explicitly listed in `capabilities`.
6. Either peer may close the byte stream to end the session.

Requests sent before a successful handshake are rejected. Request IDs are UUIDs
and correlate responses, cancellation and concurrent in-flight operations.

## Request envelope

Every request contains:

| Field | Type | Meaning |
| --- | --- | --- |
| `requestID` | UUID string | Correlates request and response |
| `protocolVersion` | `{major, minor}` | Encoding contract used by the message |
| `method` | String | Extensible operation identifier |
| `parameters` | Object | Method-specific typed payload |

## Response envelope

Every response contains `requestID`, `protocolVersion` and `status`.

- `success` responses contain `payload` and do not contain a non-null `error`.
- `failure` responses contain `error` and do not contain a non-null `payload`.
- Errors contain a stable `code`, a diagnostic `message` and an optional
  `recoverySuggestion`.

## V1 methods

| Method | Parameters | Success payload | Capability |
| --- | --- | --- | --- |
| `handshake` | `RuntimeHandshakeRequest` | `RuntimeHandshakeResponse` | Always available |
| `appInfo` | Empty object | `RuntimeAppInfo` | `appInfo` |
| `hierarchySnapshot` | Empty object | `RuntimeHierarchySnapshot` | `hierarchySnapshot` |
| `nodeDetail` | `RuntimeNodeDetailRequest` | `RuntimeNodeDetail` | `nodeDetail` |
| `patchableAttributes` | Empty object | `RuntimePatchableAttributeCatalog` | `attributePatchDiscovery` |
| `applyAttributePatch` | `RuntimeApplyAttributePatchRequest` | `RuntimeAttributePatch` | `attributePatching` |
| `listAttributePatches` | Empty object | `RuntimeAttributePatchList` | `attributePatching` |
| `revertAttributePatch` | `RuntimeRevertAttributePatchRequest` | `RuntimeRevertAttributePatchResponse` | `attributePatching` |
| `clearAttributePatches` | Empty object | `RuntimeClearAttributePatchesResponse` | `attributePatching` |
| `cancelRequest` | `RuntimeCancelRequest` | `RuntimeCancelResponse` | `requestCancellation` |

Attribute patching is a Debug-only hypothesis-validation mechanism. A client
must discover the Runtime-owned attribute catalog before applying a patch and
must use an attribute identifier present in that catalog. Patches survive
individual Host reconnections but affect only the current Runtime lifecycle;
they are discarded when the Runtime stops or the App process ends and cannot
invoke arbitrary methods or persist business state.

## Compatibility

- Unknown JSON fields are ignored by existing peers.
- Unknown method, capability, platform and semantic string values must be
  preserved when decoded where the model exposes an extensible string value.
- A client must not call an unknown capability.
- Adding optional fields or capabilities is backward compatible within major
  version 1.
- Removing fields, changing field meaning or changing framing requires a new
  major wire protocol version.

## UI data conventions

- Geometry is encoded in logical points unless a field explicitly says pixels.
- Rectangles use `{origin: {x, y}, size: {width, height}}`.
- Physical screenshot mapping uses screen `scale` and `nativeScale`.
- Colors declare a color space and floating-point RGBA components.
- Node IDs are positive, session-scoped UInt64 values and cannot be reused as
  process memory addresses.
- Hierarchy child order and sibling indices are stable within one snapshot.
- `visibility.isVisible` is true only when the node and its ancestors are not
  hidden, `effectiveAlpha` is greater than `0.01`, and the node intersects the
  screen within all ancestor clipping bounds.

## Reference assets

Machine-readable schemas live under `Schemas/v1`. Valid and invalid examples
live under `Fixtures/v1`; every language implementation must decode the valid
fixtures and reject fixtures marked invalid.
