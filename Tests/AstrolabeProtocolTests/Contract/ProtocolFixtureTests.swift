//
//  ProtocolFixtureTests.swift
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/16.
//

import AstrolabeProtocol
import Foundation
import XCTest

final class ProtocolFixtureTests: XCTestCase {
    func testEveryValidV2FixtureRoundTripsThroughSwiftModels() throws {
        for contractCase in try manifestCases(expectation: "valid") {
            let data = try fixtureData(at: contractCase.fixture)
            try assertSwiftRoundTrip(
                data,
                schema: contractCase.schema,
                fixture: contractCase.fixture
            )
        }
    }

    func testEveryInvalidV2FixtureIsRejectedBySwiftModels() throws {
        for contractCase in try manifestCases(expectation: "invalid") {
            let data = try fixtureData(at: contractCase.fixture)
            XCTAssertThrowsError(
                try decodeSwiftModel(data, schema: contractCase.schema),
                "Swift did not reject invalid fixture: \(contractCase.fixture)"
            )
        }
    }

    private func assertSwiftRoundTrip(
        _ data: Data,
        schema: String,
        fixture: String
    ) throws {
        let decoded = try decodeSwiftModel(data, schema: schema)
        let encoded = try decoded.encode(with: RuntimeMessageCodec())
        XCTAssertEqual(
            try canonicalJSON(data),
            try canonicalJSON(encoded),
            "Swift round-trip changed fixture: \(fixture)"
        )
        let roundTripped = try decodeSwiftModel(encoded, schema: schema)
        XCTAssertEqual(encoded, try roundTripped.encode(with: RuntimeMessageCodec()))
    }

    private func canonicalJSON(_ data: Data) throws -> Data {
        let object = try JSONSerialization.jsonObject(with: data)
        return try JSONSerialization.data(withJSONObject: object, options: [.sortedKeys])
    }

    private func decodeSwiftModel(_ data: Data, schema: String) throws -> AnyFixtureValue {
        let codec = RuntimeMessageCodec()

        switch schemaFragment(in: schema) {
        case "handshakeRequest":
            return try wrap(RuntimeRequest<RuntimeHandshakeParameters>.self, data, codec)
        case "applicationInfoRequest":
            return try wrap(RuntimeRequest<RuntimeApplicationInfoParameters>.self, data, codec)
        case "hierarchySnapshotRequest":
            return try wrap(RuntimeRequest<RuntimeHierarchySnapshotParameters>.self, data, codec)
        case "patchableAttributesRequest":
            return try wrap(RuntimeRequest<RuntimePatchableAttributesParameters>.self, data, codec)
        case "listAttributePatchesRequest":
            return try wrap(RuntimeRequest<RuntimeListAttributePatchesParameters>.self, data, codec)
        case "clearAttributePatchesRequest":
            return try wrap(RuntimeRequest<RuntimeClearAttributePatchesParameters>.self, data, codec)
        case "nodeDetailRequest":
            return try wrap(RuntimeRequest<RuntimeNodeDetailParameters>.self, data, codec)
        case "applyAttributePatchRequest":
            return try wrap(RuntimeRequest<RuntimeApplyAttributePatchParameters>.self, data, codec)
        case "revertAttributePatchRequest":
            return try wrap(RuntimeRequest<RuntimeRevertAttributePatchParameters>.self, data, codec)
        case "cancelRequestRequest":
            return try wrap(RuntimeRequest<RuntimeCancelRequestParameters>.self, data, codec)
        case "unknownMethodRequest":
            return try wrap(RuntimeRequest<RuntimeJSONObject>.self, data, codec)
        case "handshakeSuccessResponse":
            return try wrap(RuntimeResponse<RuntimeHandshakePayload>.self, data, codec)
        case "applicationInfoSuccessResponse":
            return try wrap(RuntimeResponse<RuntimeApplicationInfoPayload>.self, data, codec)
        case "hierarchySnapshotSuccessResponse":
            return try wrap(RuntimeResponse<RuntimeHierarchySnapshotPayload>.self, data, codec)
        case "nodeDetailSuccessResponse":
            return try wrap(RuntimeResponse<RuntimeNodeDetailPayload>.self, data, codec)
        case "patchableAttributesSuccessResponse":
            return try wrap(RuntimeResponse<RuntimePatchableAttributesPayload>.self, data, codec)
        case "applyAttributePatchSuccessResponse":
            return try wrap(RuntimeResponse<RuntimeAttributePatch>.self, data, codec)
        case "listAttributePatchesSuccessResponse":
            return try wrap(RuntimeResponse<RuntimeAttributePatchListPayload>.self, data, codec)
        case "revertAttributePatchSuccessResponse":
            return try wrap(RuntimeResponse<RuntimeRevertAttributePatchPayload>.self, data, codec)
        case "clearAttributePatchesSuccessResponse":
            return try wrap(RuntimeResponse<RuntimeClearAttributePatchesPayload>.self, data, codec)
        case "cancelRequestSuccessResponse":
            return try wrap(RuntimeResponse<RuntimeCancelRequestPayload>.self, data, codec)
        case "nodeDetailFailureResponse", "unknownMethodFailureResponse":
            return try wrap(RuntimeResponse<RuntimeJSONObject>.self, data, codec)
        case "attributeValue":
            return try wrap(RuntimeAttributeValue.self, data, codec)
        case "patchableAttribute":
            return try wrap(RuntimePatchableAttribute.self, data, codec)
        case "coordinateRect":
            return try wrap(RuntimeCoordinateRect.self, data, codec)
        case "displayInfo":
            return try wrap(RuntimeDisplayInfo.self, data, codec)
        case "extensionMap":
            return try wrap(RuntimeExtensionMap.self, data, codec)
        case "visibility":
            return try wrap(RuntimeNodeVisibility.self, data, codec)
        case "response.schema.json":
            return try wrap(RuntimeResponse<RuntimeJSONObject>.self, data, codec)
        default:
            throw FixtureTestError.unsupportedSchema(schema)
        }
    }

    private func wrap<Value: Codable & Equatable>(
        _ type: Value.Type,
        _ data: Data,
        _ codec: RuntimeMessageCodec
    ) throws -> AnyFixtureValue {
        AnyFixtureValue(try codec.decode(type, from: data))
    }

    private func schemaFragment(in schema: String) -> String {
        guard let fragment = schema.split(separator: "#", maxSplits: 1).last,
              fragment != schema[...] else {
            return URL(string: schema)?.lastPathComponent ?? schema
        }
        return fragment.split(separator: "/").last.map(String.init) ?? String(fragment)
    }

    private func manifestCases(expectation: String) throws -> [ManifestCase] {
        let data = try Data(contentsOf: repositoryRootURL()
            .appendingPathComponent("Fixtures/v2/manifest.json"))
        return try JSONDecoder().decode(Manifest.self, from: data).cases.filter {
            $0.expectation == expectation
        }
    }

    private func fixtureData(at relativePath: String) throws -> Data {
        try Data(contentsOf: repositoryRootURL().appendingPathComponent(relativePath))
    }

    private func repositoryRootURL() throws -> URL {
        var candidateURL = URL(fileURLWithPath: #filePath).deletingLastPathComponent()
        while candidateURL.pathComponents.count > 1 {
            if FileManager.default.fileExists(
                atPath: candidateURL.appendingPathComponent("Package.swift").path
            ) {
                return candidateURL
            }
            candidateURL.deleteLastPathComponent()
        }
        throw FixtureTestError.repositoryRootNotFound
    }
}

private struct Manifest: Decodable {
    /// Contract cases registered by the V2 conformance manifest.
    let cases: [ManifestCase]
}

private struct ManifestCase: Decodable {
    /// Repository-relative fixture path.
    let fixture: String

    /// JSON Schema reference used to route the Swift DTO.
    let schema: String

    /// Expected contract result, either valid or invalid.
    let expectation: String
}

private struct AnyFixtureValue {
    private let encodeValue: (RuntimeMessageCodec) throws -> Data

    init<Value: Codable & Equatable>(_ value: Value) {
        encodeValue = { codec in try codec.encode(value) }
    }

    func encode(with codec: RuntimeMessageCodec) throws -> Data {
        try encodeValue(codec)
    }
}

private enum FixtureTestError: Error {
    case repositoryRootNotFound
    case unsupportedSchema(String)
}
