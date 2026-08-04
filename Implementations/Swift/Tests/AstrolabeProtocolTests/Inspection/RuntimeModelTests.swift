//
//  RuntimeModelTests.swift
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/16.
//

import AstrolabeProtocol
import Foundation
import XCTest

final class RuntimeModelTests: XCTestCase {
    func testTypedAttributeValuesRoundTrip() throws {
        let values: [RuntimeAttributeValue] = [
            .null,
            .boolean(true),
            .integer(3),
            .number(8.5),
            .string("value"),
            .stringList(["UILabel", "UIView"]),
            .measurement(RuntimeMeasurement(value: 16, unit: .scaledLogical)),
            .point(RuntimeCoordinatePoint(x: 1, y: 2, coordinateSpace: .local, unit: .logical)),
            .size(RuntimeMeasuredSize(width: 3, height: 4, unit: .logical)),
            .vector(RuntimeVector(dx: -2, dy: 4, unit: .logical)),
            .rect(RuntimeCoordinateRect(x: 1, y: 2, width: 3, height: 4, coordinateSpace: .screen, unit: .logical)),
            .insets(RuntimeInsets(top: 1, left: 2, bottom: 3, right: 4, unit: .logical)),
            .color(RuntimeColor(colorSpace: "srgb", red: 1, green: 0, blue: 0, alpha: 0.5)),
            .array([.string("value")]),
            .object(["key": .boolean(true)]),
            .extensionValue(
                type: try RuntimeNamespacedIdentifier(rawValue: "ios.uikit.custom"),
                value: .object(["enabled": .boolean(true)])
            )
        ]
        let codec = RuntimeMessageCodec()

        for value in values {
            XCTAssertEqual(
                try codec.decode(RuntimeAttributeValue.self, from: codec.encode(value)),
                value
            )
        }
    }

    func testProtocolRangeRejectsDescendingAndCrossMajorRanges() {
        XCTAssertThrowsError(
            try RuntimeProtocolRange(
                minimum: RuntimeProtocolVersion(major: 2, minor: 1),
                maximum: RuntimeProtocolVersion(major: 2, minor: 0)
            )
        )
        XCTAssertThrowsError(
            try RuntimeProtocolRange(
                minimum: RuntimeProtocolVersion(major: 1, minor: 9),
                maximum: RuntimeProtocolVersion(major: 2, minor: 0)
            )
        )
    }

    func testOpaqueIdentifiersEncodeAsStrings() throws {
        let identifier = try RuntimeOpaqueIdentifier(rawValue: "node:label:1")
        let payload = try RuntimeMessageCodec().encode(
            RuntimeJSONObject(values: ["nodeID": .string(identifier.rawValue)])
        )

        XCTAssertTrue(String(decoding: payload, as: UTF8.self).contains(#""node:label:1""#))
    }
}
