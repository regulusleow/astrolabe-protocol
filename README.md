# Astrolabe Protocol

English | [简体中文](README.zh-CN.md)

Astrolabe Protocol defines the platform-neutral wire contract shared by the
Astrolabe Host and platform Runtime SDKs.

## Contents

- The normative, versioned wire contract under `Contract/`.
- Equal Swift and Kotlin implementations under `Implementations/`.
- Request and response envelopes, errors, version negotiation, and frame
  codecs.
- Versioned JSON Schemas and cross-language Fixtures under each Contract version.
- The Wire Protocol 2.0 specification in
  [Contract/v2/PROTOCOL.md](Contract/v2/PROTOCOL.md).
- Archived Wire Protocol 1.0 documentation in
  [Contract/v1/PROTOCOL.md](Contract/v1/PROTOCOL.md).

UIKit, Android View, transport listeners, device discovery, screenshots, CLI
commands, and MCP tools are outside this repository.

## Installation

Add the package through Swift Package Manager:

```swift
.package(
    url: "https://github.com/regulusleow/astrolabe-protocol.git",
    exact: "2.0.0"
)
```

Depend on the `AstrolabeProtocol` product and import it with:

```swift
import AstrolabeProtocol
```

## Wire Format

Each message contains a four-byte unsigned payload length in network byte order
followed by a UTF-8 JSON object. The current Wire Protocol version is `2.0`.

Swift types are one implementation of the contract. Other implementations use
the specification, Schemas, Fixtures, and documented wire behavior as their
compatibility source of truth.

## Repository Layout

```text
Contract/          Normative protocol documents, Schemas, and Fixtures
Implementations/   Equal Swift and Kotlin protocol implementations
Tooling/           Contract validation, release automation, and tests
```

Root SwiftPM, Gradle, and npm manifests are ecosystem entrypoints. They map to
the implementation and Tooling directories without making one language the
repository's primary implementation.

Both language implementations use the same conceptual domains: `Core`,
`Attributes`, `Framing`, `Messaging`, `Negotiation`, `Inspection`, and
`Patching`. Swift uses nested `Application`, `Hierarchy`, and `NodeDetail`
directories inside Inspection; Kotlin keeps the flat public package
`dev.astrolabe.protocol` and does not manufacture package boundaries for
physical directory symmetry.

## Development

Install the contract validator and run all checks:

```bash
npm ci
npm test
swift test --parallel
swift build -c release
```

Validation compiles every Draft 2020-12 Schema, checks valid and invalid
Fixtures, applies semantic rules that JSON Schema cannot express, and verifies
that the Swift DTOs accept and reject the same payloads.

## License

Astrolabe Protocol is available under the [Apache License 2.0](LICENSE).
