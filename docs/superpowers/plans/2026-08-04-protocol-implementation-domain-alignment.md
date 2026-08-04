# Protocol Implementation Domain Alignment Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Align Swift and Kotlin implementation files with the approved protocol domains without changing declarations, packages, modules, wire behavior, schemas, fixtures, or published coordinates.

**Architecture:** Attribute values and their supporting types form an `Attributes` domain consumed by Inspection and Patching. Framing contains only byte framing, Messaging owns JSON message encoding, and Swift Inspection uses Application, Hierarchy, and NodeDetail subdirectories while Kotlin retains a flat `inspection` package directory because its public package remains `dev.astrolabe.protocol`.

**Tech Stack:** Swift 5.9 / SwiftPM, Kotlin/JVM / Gradle, Kotlin Serialization, XCTest, Kotlin Test.

## Global Constraints

- Do not change any public type, initializer, property, method, Codable shape, serializer, package, module, product, artifact, Schema, Fixture, or Wire Version.
- Do not create compatibility directories, symlinks, forwarding types, or duplicate source files.
- Keep one Swift module named `AstrolabeProtocol` and one Kotlin package named `dev.astrolabe.protocol`.
- Keep `RuntimePatchableAttribute` and `RuntimeAttributePatch` in Patching because they define patch discovery and lifecycle, not general attribute values.
- Put `RuntimeLayoutRelation` in Attributes because `RuntimeAttributeValue` directly owns the `layoutRelations` case and should not depend conceptually on Inspection.
- Do not split Kotlin `inspection` into package-like subdirectories while declarations retain a flat package.
- Never commit automatically; leave the completed change for user review.

---

### Task 1: Record the clean baseline

**Files:**
- Verify only.

**Interfaces:**
- Consumes: commit `b7453fb` on `develop`.
- Produces: pre-move Swift and Kotlin regression evidence.

- [x] **Step 1: Run Swift tests**

Run: `swift test --parallel`

Expected: 20 tests pass.

- [x] **Step 2: Run Kotlin tests**

Run: `./gradlew :AstrolabeProtocolKotlin:test`

Expected: Gradle exits zero.

### Task 2: Align Swift source domains

**Files:**
- Move: `Implementations/Swift/Sources/AstrolabeProtocol/Framing/RuntimeMessageCodec.swift` → `Implementations/Swift/Sources/AstrolabeProtocol/Messaging/RuntimeMessageCodec.swift`
- Move: `Implementations/Swift/Sources/AstrolabeProtocol/Inspection/RuntimeAttributeValue.swift` → `Implementations/Swift/Sources/AstrolabeProtocol/Attributes/RuntimeAttributeValue.swift`
- Move: `Implementations/Swift/Sources/AstrolabeProtocol/Inspection/RuntimeAttributedTextRun.swift` → `Implementations/Swift/Sources/AstrolabeProtocol/Attributes/RuntimeAttributedTextRun.swift`
- Move: `Implementations/Swift/Sources/AstrolabeProtocol/Inspection/RuntimeLayoutRelation.swift` → `Implementations/Swift/Sources/AstrolabeProtocol/Attributes/RuntimeLayoutRelation.swift`
- Move: `Implementations/Swift/Sources/AstrolabeProtocol/Inspection/RuntimeApplication.swift` → `Implementations/Swift/Sources/AstrolabeProtocol/Inspection/Application/RuntimeApplication.swift`
- Move: `Implementations/Swift/Sources/AstrolabeProtocol/Inspection/RuntimeHierarchy.swift` → `Implementations/Swift/Sources/AstrolabeProtocol/Inspection/Hierarchy/RuntimeHierarchy.swift`
- Move: `Implementations/Swift/Sources/AstrolabeProtocol/Inspection/RuntimeNodeDetail.swift` → `Implementations/Swift/Sources/AstrolabeProtocol/Inspection/NodeDetail/RuntimeNodeDetail.swift`

**Interfaces:**
- Consumes: SwiftPM recursive source discovery under the explicit target path.
- Produces: `Core`, `Attributes`, `Framing`, `Messaging`, `Negotiation`, `Inspection`, and `Patching` source ownership with unchanged declarations.

- [x] **Step 1: Create domain directories and move files with Git**

```bash
mkdir -p Implementations/Swift/Sources/AstrolabeProtocol/Attributes
mkdir -p Implementations/Swift/Sources/AstrolabeProtocol/Inspection/Application
mkdir -p Implementations/Swift/Sources/AstrolabeProtocol/Inspection/Hierarchy
mkdir -p Implementations/Swift/Sources/AstrolabeProtocol/Inspection/NodeDetail
git mv Implementations/Swift/Sources/AstrolabeProtocol/Framing/RuntimeMessageCodec.swift Implementations/Swift/Sources/AstrolabeProtocol/Messaging/RuntimeMessageCodec.swift
git mv Implementations/Swift/Sources/AstrolabeProtocol/Inspection/RuntimeAttributeValue.swift Implementations/Swift/Sources/AstrolabeProtocol/Attributes/RuntimeAttributeValue.swift
git mv Implementations/Swift/Sources/AstrolabeProtocol/Inspection/RuntimeAttributedTextRun.swift Implementations/Swift/Sources/AstrolabeProtocol/Attributes/RuntimeAttributedTextRun.swift
git mv Implementations/Swift/Sources/AstrolabeProtocol/Inspection/RuntimeLayoutRelation.swift Implementations/Swift/Sources/AstrolabeProtocol/Attributes/RuntimeLayoutRelation.swift
git mv Implementations/Swift/Sources/AstrolabeProtocol/Inspection/RuntimeApplication.swift Implementations/Swift/Sources/AstrolabeProtocol/Inspection/Application/RuntimeApplication.swift
git mv Implementations/Swift/Sources/AstrolabeProtocol/Inspection/RuntimeHierarchy.swift Implementations/Swift/Sources/AstrolabeProtocol/Inspection/Hierarchy/RuntimeHierarchy.swift
git mv Implementations/Swift/Sources/AstrolabeProtocol/Inspection/RuntimeNodeDetail.swift Implementations/Swift/Sources/AstrolabeProtocol/Inspection/NodeDetail/RuntimeNodeDetail.swift
```

- [x] **Step 2: Verify the move is declaration-preserving**

Run: `git diff --staged --summary && git diff --staged --numstat`

Expected: all seven files are 100% renames with zero added or deleted source lines.

- [x] **Step 3: Run Swift tests**

Run: `swift test --parallel`

Expected: 20 tests still pass.

### Task 3: Mirror Swift tests by domain

**Files:**
- Delete after extraction: `Implementations/Swift/Tests/AstrolabeProtocolTests/Inspection/RuntimeModelTests.swift`
- Create: `Implementations/Swift/Tests/AstrolabeProtocolTests/Attributes/RuntimeAttributeValueTests.swift`
- Create: `Implementations/Swift/Tests/AstrolabeProtocolTests/Core/RuntimeIdentifierTests.swift`
- Create: `Implementations/Swift/Tests/AstrolabeProtocolTests/Negotiation/RuntimeProtocolRangeTests.swift`

**Interfaces:**
- Consumes: the three existing `RuntimeModelTests` methods unchanged.
- Produces: one test class per owning source domain.

- [x] **Step 1: Move the attribute round-trip test**

Create `RuntimeAttributeValueTests` containing only `testTypedAttributeValuesRoundTrip`, preserving its existing value list, codec calls, and assertions byte-for-byte.

- [x] **Step 2: Move the identifier test**

Create `RuntimeIdentifierTests` containing only `testOpaqueIdentifiersEncodeAsStrings`, preserving its payload and assertion.

- [x] **Step 3: Move the protocol range test**

Create `RuntimeProtocolRangeTests` containing only `testProtocolRangeRejectsDescendingAndCrossMajorRanges`, preserving both invalid ranges.

- [x] **Step 4: Remove the mixed-domain test file and verify test count**

Run: `swift test --parallel`

Expected: the same 20 tests pass under the three domain-specific test classes.

### Task 4: Align Kotlin source and test domains

**Files:**
- Move: `Implementations/Kotlin/src/main/kotlin/dev/astrolabe/protocol/inspection/RuntimeAttributeValue.kt` → `Implementations/Kotlin/src/main/kotlin/dev/astrolabe/protocol/attributes/RuntimeAttributeValue.kt`
- Move: `Implementations/Kotlin/src/main/kotlin/dev/astrolabe/protocol/inspection/RuntimeAttributedTextRun.kt` → `Implementations/Kotlin/src/main/kotlin/dev/astrolabe/protocol/attributes/RuntimeAttributedTextRun.kt`
- Move: `Implementations/Kotlin/src/main/kotlin/dev/astrolabe/protocol/inspection/RuntimeLayoutRelation.kt` → `Implementations/Kotlin/src/main/kotlin/dev/astrolabe/protocol/attributes/RuntimeLayoutRelation.kt`
- Modify: `Implementations/Kotlin/src/test/kotlin/dev/astrolabe/protocol/inspection/RuntimeNodeDetailModelTest.kt`
- Create: `Implementations/Kotlin/src/test/kotlin/dev/astrolabe/protocol/attributes/RuntimeAttributeValueTest.kt`

**Interfaces:**
- Consumes: flat Kotlin package declarations and recursive Gradle source discovery.
- Produces: a physical `attributes` domain without changing `package dev.astrolabe.protocol`.

- [x] **Step 1: Move Kotlin attribute source files**

```bash
mkdir -p Implementations/Kotlin/src/main/kotlin/dev/astrolabe/protocol/attributes
git mv Implementations/Kotlin/src/main/kotlin/dev/astrolabe/protocol/inspection/RuntimeAttributeValue.kt Implementations/Kotlin/src/main/kotlin/dev/astrolabe/protocol/attributes/RuntimeAttributeValue.kt
git mv Implementations/Kotlin/src/main/kotlin/dev/astrolabe/protocol/inspection/RuntimeAttributedTextRun.kt Implementations/Kotlin/src/main/kotlin/dev/astrolabe/protocol/attributes/RuntimeAttributedTextRun.kt
git mv Implementations/Kotlin/src/main/kotlin/dev/astrolabe/protocol/inspection/RuntimeLayoutRelation.kt Implementations/Kotlin/src/main/kotlin/dev/astrolabe/protocol/attributes/RuntimeLayoutRelation.kt
```

- [x] **Step 2: Extract attribute-value tests from node detail**

Move these four methods unchanged into `RuntimeAttributeValueTest`:

```text
standalone vector attribute values decode with signed components
attribute value discriminator must be a string
integer attribute value must contain a JSON number
common and extension attribute values round trip symmetrically
```

Keep `node detail fixtures decode through typed contracts` and `node identifiers and attribute identifiers retain wire constraints` in `RuntimeNodeDetailModelTest`.

- [x] **Step 3: Run Kotlin tests**

Run: `./gradlew :AstrolabeProtocolKotlin:test`

Expected: Gradle exits zero with the same test behavior.

### Task 5: Document and verify the domain boundary

**Files:**
- Modify: `README.md`
- Modify: `README.zh-CN.md`

**Interfaces:**
- Consumes: final implementation directory tree.
- Produces: a discoverable ownership rule for future UI Graph work.

- [x] **Step 1: Add the implementation-domain list to both READMEs**

Document the seven shared domains:

```text
Core / Attributes / Framing / Messaging / Negotiation / Inspection / Patching
```

State that Swift may use nested Inspection directories while Kotlin keeps the flat public package.

- [x] **Step 2: Run all regression gates**

```bash
npm test
swift test --parallel
swift build -c release
./gradlew :AstrolabeProtocolKotlin:test
./gradlew :AstrolabeProtocolKotlin:publishToMavenLocal
git diff --check
```

Expected: every command exits zero.

- [x] **Step 3: Audit scope**

Run current-tree searches for attribute files under Inspection and `RuntimeMessageCodec.swift` under Framing.

Expected: no misplaced file remains, no public declaration changes appear, and no compatibility path exists.

- [x] **Step 4: Leave all changes uncommitted**

Run: `git status --short --branch`

Expected: only planned domain moves, test splits, README updates, and this plan are present.
