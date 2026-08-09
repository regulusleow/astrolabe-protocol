//
//  RuntimeProtocolRangeTests.swift
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/16.
//

import AstrolabeProtocol
import XCTest

final class RuntimeProtocolRangeTests: XCTestCase {
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
}
