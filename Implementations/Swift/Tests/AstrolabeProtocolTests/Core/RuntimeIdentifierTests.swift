//
//  RuntimeIdentifierTests.swift
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/16.
//

import AstrolabeProtocol
import Foundation
import XCTest

final class RuntimeIdentifierTests: XCTestCase {
    func testOpaqueIdentifiersEncodeAsStrings() throws {
        let identifier = try RuntimeOpaqueIdentifier(rawValue: "node:label:1")
        let payload = try RuntimeMessageCodec().encode(
            RuntimeJSONObject(values: ["nodeID": .string(identifier.rawValue)])
        )

        XCTAssertTrue(String(decoding: payload, as: UTF8.self).contains(#""node:label:1""#))
    }
}
