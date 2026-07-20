# Astrolabe Protocol

English | [简体中文](README.zh-CN.md)

Astrolabe Protocol defines the platform-neutral wire contract shared by the
Astrolabe Host and platform Runtime SDKs.

## Contents

- Swift DTOs and typed protocol models in the `AstrolabeProtocol` product.
- Request and response envelopes, errors, version negotiation, and frame
  codecs.
- Versioned JSON Schemas under `Schemas/`.
- Valid and invalid cross-language examples under `Fixtures/`.
- The Wire Protocol 2.0 specification in
  [PROTOCOL-2.0.md](PROTOCOL-2.0.md).
- Archived Wire Protocol 1.0 documentation in [PROTOCOL.md](PROTOCOL.md).

UIKit, Android View, transport listeners, device discovery, screenshots, CLI
commands, and MCP tools are outside this repository.

## Installation

Add the package through Swift Package Manager:

```swift
.package(
    url: "https://github.com/regulusleow/astrolabe-protocol.git",
    exact: "1.0.0"
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
