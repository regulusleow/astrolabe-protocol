//
//  ProtocolV2ContractTests.swift
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/15.
//

import Foundation
import XCTest

final class ProtocolV2ContractTests: XCTestCase {
    func testV2ContractAssetsCoverEveryMethod() throws {
        let repositoryURL = try repositoryRootURL()
        let manifest = try jsonObject(
            at: repositoryURL.appendingPathComponent("Fixtures/v2/manifest.json")
        )
        let methods = try XCTUnwrap(manifest["methods"] as? [[String: Any]])
        let methodNames = Set(try methods.map { method in
            try XCTUnwrap(method["name"] as? String)
        })

        XCTAssertEqual(methods.count, Self.expectedMethods.count)
        XCTAssertEqual(methodNames, Set(Self.expectedMethods))
        XCTAssertTrue(
            FileManager.default.fileExists(
                atPath: repositoryURL.appendingPathComponent("PROTOCOL-2.0.md").path
            )
        )

        for method in methods {
            for key in ["requestSchema", "successResponseSchema", "failureResponseSchema"] {
                let schemaReference = try XCTUnwrap(method[key] as? String)
                try assertSchemaReferenceExists(schemaReference, repositoryURL: repositoryURL)
            }
        }
    }

    func testV2ConformanceCasesReferenceExistingFixturesAndSchemas() throws {
        let repositoryURL = try repositoryRootURL()
        let manifest = try jsonObject(
            at: repositoryURL.appendingPathComponent("Fixtures/v2/manifest.json")
        )
        let cases = try XCTUnwrap(manifest["cases"] as? [[String: Any]])
        let expectations = Set(cases.compactMap { $0["expectation"] as? String })

        XCTAssertEqual(expectations, ["valid", "invalid"])
        XCTAssertFalse(cases.isEmpty)

        for contractCase in cases {
            let fixturePath = try XCTUnwrap(contractCase["fixture"] as? String)
            let schemaReference = try XCTUnwrap(contractCase["schema"] as? String)
            XCTAssertTrue(
                FileManager.default.fileExists(
                    atPath: repositoryURL.appendingPathComponent(fixturePath).path
                ),
                "Fixture does not exist: \(fixturePath)"
            )
            try assertSchemaReferenceExists(schemaReference, repositoryURL: repositoryURL)
        }
    }

    private func assertSchemaReferenceExists(
        _ reference: String,
        repositoryURL: URL
    ) throws {
        let schemaIdentifier = try XCTUnwrap(reference.split(separator: "#").first)
        let schemaPrefix = "https://astrolabe.dev/schemas/v2/"
        XCTAssertTrue(reference.hasPrefix(schemaPrefix), "Schema ID does not belong to V2: \(reference)")
        let fileName = String(schemaIdentifier.dropFirst(schemaPrefix.count))
        XCTAssertTrue(
            FileManager.default.fileExists(
                atPath: repositoryURL
                    .appendingPathComponent("Schemas/v2")
                    .appendingPathComponent(fileName)
                    .path
            ),
            "Schema does not exist: \(reference)"
        )
    }

    private func jsonObject(at url: URL) throws -> [String: Any] {
        let data = try Data(contentsOf: url)
        return try XCTUnwrap(
            JSONSerialization.jsonObject(with: data) as? [String: Any]
        )
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

        throw ProtocolV2ContractTestError.repositoryRootNotFound
    }

    private static let expectedMethods = [
        "handshake",
        "applicationInfo",
        "hierarchySnapshot",
        "nodeDetail",
        "patchableAttributes",
        "applyAttributePatch",
        "listAttributePatches",
        "revertAttributePatch",
        "clearAttributePatches",
        "cancelRequest"
    ]
}

private enum ProtocolV2ContractTestError: Error {
    case repositoryRootNotFound
}
