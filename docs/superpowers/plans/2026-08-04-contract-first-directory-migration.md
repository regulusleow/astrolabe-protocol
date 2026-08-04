# Contract-first Directory Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Move `astrolabe-protocol` to the approved Contract-first top-level layout without changing public APIs, wire semantics, artifact coordinates, or release behavior.

**Architecture:** `Contract/` becomes the normative location for protocol documents, schemas, fixtures, and the V2 manifest. Swift and Kotlin move under symmetric `Implementations/` directories, while validation, release, and test scripts move under `Tooling/`; root build manifests explicitly map these physical paths.

**Tech Stack:** Swift 5.9 / SwiftPM, Kotlin/JVM / Gradle, Node.js 22, JSON Schema Draft 2020-12, GitHub Actions.

## Global Constraints

- Do not add compatibility directories, symlinks, duplicated schemas, or forwarding files.
- Keep Swift module/product `AstrolabeProtocol` unchanged.
- Keep Kotlin package `dev.astrolabe.protocol`, Gradle project `:AstrolabeProtocolKotlin`, Maven coordinates, and protocol version unchanged.
- Keep every Schema `$id`, message type, JSON field, and Fixture validity expectation unchanged.
- This plan performs the mechanical top-level migration only; it does not split Schema domains, reorganize implementation source domains, or add UI Graph models.
- Never commit automatically; leave all changes uncommitted for user review.
- Run `release-prepare.mjs` tests, but do not invoke the release command because it commits and tags by design.

---

### Task 1: Capture the pre-migration regression baseline

**Files:**
- Verify only; no files changed.

**Interfaces:**
- Consumes: current root paths and build manifests.
- Produces: passing baseline for comparison after the move.

- [x] **Step 1: Run the aggregate contract and Tooling tests**

Run: `npm test`

Expected: version check, schema/fixture validation, Swift contract tests, and eight Node tests pass.

- [x] **Step 2: Run the full Swift suite and release build**

Run: `swift test --parallel && swift build -c release`

Expected: 20 Swift tests pass and the release product builds.

- [x] **Step 3: Run Kotlin tests and local publication build**

Run: `./gradlew :AstrolabeProtocolKotlin:test :AstrolabeProtocolKotlin:publishToMavenLocal`

Expected: both Gradle tasks complete successfully.

### Task 2: Move normative Contract assets

**Files:**
- Move: `PROTOCOL.md` → `Contract/v1/PROTOCOL.md`
- Move: `PROTOCOL-2.0.md` → `Contract/v2/PROTOCOL.md`
- Move: `Schemas/v1/` → `Contract/v1/Schemas/`
- Move: `Schemas/v2/` → `Contract/v2/Schemas/`
- Move: `Fixtures/v1/` → `Contract/v1/Fixtures/`
- Move: `Fixtures/v2/manifest.json` → `Contract/v2/manifest.json`
- Move: `Fixtures/v2/valid/` → `Contract/v2/Fixtures/valid/`
- Move: `Fixtures/v2/invalid/` → `Contract/v2/Fixtures/invalid/`
- Modify: `Contract/v2/manifest.json`
- Modify: `Contract/v2/Schemas/conformance-manifest.schema.json`
- Modify: `Contract/v2/PROTOCOL.md`

**Interfaces:**
- Consumes: existing Schema IDs and fixture case registration.
- Produces: `Contract/v1` and `Contract/v2` as the only normative asset roots.

- [x] **Step 1: Move assets with history-preserving Git moves**

```bash
mkdir -p Contract/v1 Contract/v2/Fixtures
git mv PROTOCOL.md Contract/v1/PROTOCOL.md
git mv PROTOCOL-2.0.md Contract/v2/PROTOCOL.md
git mv Schemas/v1 Contract/v1/Schemas
git mv Schemas/v2 Contract/v2/Schemas
git mv Fixtures/v1 Contract/v1/Fixtures
git mv Fixtures/v2/manifest.json Contract/v2/manifest.json
git mv Fixtures/v2/valid Contract/v2/Fixtures/valid
git mv Fixtures/v2/invalid Contract/v2/Fixtures/invalid
```

- [x] **Step 2: Rewrite only repository-relative fixture paths in the manifest**

Replace every `Fixtures/v2/valid/` and `Fixtures/v2/invalid/` prefix with `Contract/v2/Fixtures/valid/` and `Contract/v2/Fixtures/invalid/`. Leave Schema URLs and fixture JSON content unchanged.

Update the conformance manifest Schema path pattern and V2 protocol document's normative asset paths to the same Contract-first locations.

- [x] **Step 3: Verify contract inventory**

Run: `find Contract -type f | sort`

Expected: both protocol versions exist under `Contract/`, with V2 manifest, schemas, valid fixtures, and invalid fixtures present exactly once.

### Task 3: Move symmetric language implementations

**Files:**
- Move: `Sources/` → `Implementations/Swift/Sources/`
- Move: `Tests/` → `Implementations/Swift/Tests/`
- Move: `AstrolabeProtocolKotlin/` → `Implementations/Kotlin/`
- Modify: `Package.swift`
- Modify: `settings.gradle.kts`
- Modify: `Implementations/Kotlin/build.gradle.kts`
- Modify: `Implementations/Swift/Tests/AstrolabeProtocolTests/Contract/ProtocolFixtureTests.swift`
- Modify: `Implementations/Swift/Tests/AstrolabeProtocolTests/Contract/ProtocolV2ContractTests.swift`
- Modify: `Implementations/Kotlin/src/test/kotlin/dev/astrolabe/protocol/contract/ProtocolFixtureConformanceTest.kt`
- Modify: all Kotlin tests that load `v2/valid` or `v2/invalid` classpath resources

**Interfaces:**
- Consumes: Contract paths produced by Task 2.
- Produces: unchanged Swift and Kotlin public APIs from symmetric physical roots.

- [x] **Step 1: Move both implementations**

```bash
mkdir -p Implementations/Swift Implementations
git mv Sources Implementations/Swift/Sources
git mv Tests Implementations/Swift/Tests
git mv AstrolabeProtocolKotlin Implementations/Kotlin
```

- [x] **Step 2: Add explicit SwiftPM target paths**

```swift
.target(
    name: "AstrolabeProtocol",
    path: "Implementations/Swift/Sources/AstrolabeProtocol"
),
.testTarget(
    name: "AstrolabeProtocolTests",
    dependencies: ["AstrolabeProtocol"],
    path: "Implementations/Swift/Tests/AstrolabeProtocolTests"
)
```

- [x] **Step 3: Map the existing Gradle project to its new directory**

```kotlin
include(":AstrolabeProtocolKotlin")
project(":AstrolabeProtocolKotlin").projectDir = file("Implementations/Kotlin")
```

- [x] **Step 4: Point Kotlin test resources at Contract**

```kotlin
sourceSets {
    test {
        resources.srcDir(rootProject.layout.projectDirectory.dir("Contract"))
    }
}
```

Update every Kotlin fixture lookup from `v2/valid` and `v2/invalid` to `v2/Fixtures/valid` and `v2/Fixtures/invalid`.

- [x] **Step 5: Update Swift Contract test paths**

Use `Contract/v2/manifest.json`, `Contract/v2/PROTOCOL.md`, and `Contract/v2/Schemas` while keeping repository-root discovery based on `Package.swift`.

- [x] **Step 6: Run language-specific regressions**

Run: `swift test --parallel && ./gradlew :AstrolabeProtocolKotlin:test`

Expected: the same 20 Swift tests and all Kotlin tests pass from the new roots.

### Task 4: Move Tooling and update root entrypoints

**Files:**
- Move: `scripts/validate-contract.mjs` → `Tooling/ContractValidation/validate-contract.mjs`
- Move: `scripts/versioning.mjs` → `Tooling/Release/versioning.mjs`
- Move: `scripts/version-sync.mjs` → `Tooling/Release/version-sync.mjs`
- Move: `scripts/release-prepare.mjs` → `Tooling/Release/release-prepare.mjs`
- Move: `test/*.test.mjs` → `Tooling/Tests/`
- Modify: `package.json`
- Modify: `Tooling/ContractValidation/validate-contract.mjs`
- Modify: `Tooling/Release/versioning.mjs`
- Modify: `Tooling/Release/version-sync.mjs`
- Modify: `Tooling/Release/release-prepare.mjs`
- Modify: `Tooling/Tests/release-prepare.test.mjs`
- Modify: `Tooling/Tests/versioning.test.mjs`

**Interfaces:**
- Consumes: Contract and implementation paths from Tasks 2 and 3.
- Produces: root npm commands with unchanged names and release behavior.

- [x] **Step 1: Move Tooling by responsibility**

```bash
mkdir -p Tooling/ContractValidation Tooling/Release Tooling/Tests
git mv scripts/validate-contract.mjs Tooling/ContractValidation/validate-contract.mjs
git mv scripts/versioning.mjs Tooling/Release/versioning.mjs
git mv scripts/version-sync.mjs Tooling/Release/version-sync.mjs
git mv scripts/release-prepare.mjs Tooling/Release/release-prepare.mjs
git mv test/*.test.mjs Tooling/Tests/
```

- [x] **Step 2: Update Tooling repository-root and asset paths**

Scripts under `Tooling/*` resolve the repository root with `../..`. Use:

```javascript
const repositoryRoot = resolve(dirname(fileURLToPath(import.meta.url)), "../..");
const schemaDirectory = join(repositoryRoot, "Contract/v2/Schemas");
const fixtureDirectory = join(repositoryRoot, "Contract/v2/Fixtures");
const manifest = readJSON(join(repositoryRoot, "Contract/v2/manifest.json"));
```

Change the Swift metadata path in `versioning.mjs` to `Implementations/Swift/Sources/AstrolabeProtocol/Core/RuntimeProtocolMetadata.swift`.

- [x] **Step 3: Update Tooling test imports and temporary Swift metadata path**

```javascript
import { parseReleaseArgs, prepareRelease } from "../Release/release-prepare.mjs";
import { versionedPaths } from "../Release/versioning.mjs";
```

Create temporary metadata under `Implementations/Swift/Sources/AstrolabeProtocol/Core` in the release test.

- [x] **Step 4: Update npm scripts without changing command names**

```json
"test:schema": "node Tooling/ContractValidation/validate-contract.mjs",
"version:check": "node Tooling/Release/version-sync.mjs --check",
"version:set": "node Tooling/Release/version-sync.mjs --set",
"release:prepare": "node Tooling/Release/release-prepare.mjs",
"test": "npm run version:check && npm run test:schema && npm run test:swift-contract && node --test Tooling/Tests/*.test.mjs"
```

- [x] **Step 5: Run aggregate Tooling regression**

Run: `npm test`

Expected: schema/fixture validation, Swift Contract tests, version tests, release tests, and CI workflow tests pass.

### Task 5: Update documentation and run final gates

**Files:**
- Modify: `README.md`
- Modify: `README.zh-CN.md`
- Verify: `.github/workflows/ci.yml`
- Verify: all repository files and Git diff.

**Interfaces:**
- Consumes: completed Contract-first filesystem.
- Produces: reviewable, documented, fully regressed migration.

- [x] **Step 1: Document the Contract-first layout**

Update both READMEs to link to `Contract/v2/PROTOCOL.md` and `Contract/v1/PROTOCOL.md`, and describe:

```text
Contract/          normative protocol documents, schemas, and fixtures
Implementations/   equal Swift and Kotlin implementations
Tooling/           contract validation, release automation, and tests
```

- [x] **Step 2: Confirm CI needs no task-name changes**

The workflow continues to call `npm test`, SwiftPM root commands, and `:AstrolabeProtocolKotlin:*`; explicit root mappings absorb the physical move.

- [x] **Step 3: Run all final gates**

```bash
npm test
swift test --parallel
swift build -c release
./gradlew :AstrolabeProtocolKotlin:test
./gradlew :AstrolabeProtocolKotlin:publishToMavenLocal
git diff --check
```

Expected: all commands exit zero.

- [x] **Step 4: Audit invariants and scope**

Run path searches for `Fixtures/`, `Schemas/`, `PROTOCOL-2.0.md`, `Sources/AstrolabeProtocol`, `Tests/AstrolabeProtocolTests`, `AstrolabeProtocolKotlin/`, `scripts/`, and `test/`.

Expected: references either point to the new Contract-first paths or intentionally retain the Gradle project name; no old physical directory remains.

- [x] **Step 5: Leave the change uncommitted**

Run: `git status --short && git diff --stat`

Expected: only the planned directory migration, path updates, README changes, and this plan are present; no commit or tag is created.
