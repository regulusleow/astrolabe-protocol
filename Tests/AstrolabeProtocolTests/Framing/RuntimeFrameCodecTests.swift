//
//  RuntimeFrameCodecTests.swift
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/10.
//

import AstrolabeProtocol
import Foundation
import XCTest

final class RuntimeFrameCodecTests: XCTestCase {
    func testFrameUsesNetworkByteOrder() throws {
        let payload = Data([0xAA, 0xBB, 0xCC])

        let frame = try RuntimeFrameCodec().encode(payload: payload)

        XCTAssertEqual(Array(frame.prefix(4)), [0x00, 0x00, 0x00, 0x03])
        XCTAssertEqual(frame.dropFirst(4), payload)
    }

    func testStreamDecoderHandlesFragmentedAndConsecutiveFrames() throws {
        let codec = RuntimeFrameCodec()
        let firstPayload = Data("first".utf8)
        let secondPayload = Data("second".utf8)
        let firstFrame = try codec.encode(payload: firstPayload)
        let secondFrame = try codec.encode(payload: secondPayload)
        var decoder = codec.makeStreamDecoder()

        XCTAssertTrue(try decoder.append(firstFrame.prefix(2)).isEmpty)
        XCTAssertEqual(decoder.pendingByteCount, 2)

        var remainingData = Data(firstFrame.dropFirst(2))
        remainingData.append(secondFrame)
        let payloads = try decoder.append(remainingData)

        XCTAssertEqual(payloads, [firstPayload, secondPayload])
        XCTAssertEqual(decoder.pendingByteCount, 0)
    }

    func testFrameCodecRejectsOversizedPayload() throws {
        let codec = try RuntimeFrameCodec(maximumPayloadSize: 3)

        XCTAssertThrowsError(try codec.encode(payload: Data(count: 4))) { error in
            XCTAssertEqual(
                error as? RuntimeFrameError,
                .payloadTooLarge(actual: 4, maximum: 3)
            )
        }
    }

    func testFrameCodecRejectsEmptyPayload() {
        XCTAssertThrowsError(try RuntimeFrameCodec().encode(payload: Data())) { error in
            XCTAssertEqual(error as? RuntimeFrameError, .emptyPayload)
        }
    }

    func testStreamDecoderRejectsOversizedDeclaredPayload() throws {
        let codec = try RuntimeFrameCodec(maximumPayloadSize: 3)
        var decoder = codec.makeStreamDecoder()

        XCTAssertThrowsError(try decoder.append(Data([0, 0, 0, 4]))) { error in
            XCTAssertEqual(
                error as? RuntimeFrameError,
                .payloadTooLarge(actual: 4, maximum: 3)
            )
        }
    }


    func testStreamDecoderRejectsZeroDeclaredPayload() {
        var decoder = RuntimeFrameCodec().makeStreamDecoder()

        XCTAssertThrowsError(try decoder.append(Data([0, 0, 0, 0]))) { error in
            XCTAssertEqual(error as? RuntimeFrameError, .emptyPayload)
        }
    }
}
