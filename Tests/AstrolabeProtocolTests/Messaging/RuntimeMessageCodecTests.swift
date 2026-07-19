//
//  RuntimeMessageCodecTests.swift
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/16.
//

import AstrolabeProtocol
import Foundation
import XCTest

final class RuntimeMessageCodecTests: XCTestCase {
    func testResponseRepeatsMethodAndRejectsMismatchedOutcomeMembers() throws {
        let requestID = UUID()
        let response = try RuntimeResponse<RuntimeJSONObject>.failure(
            requestID: requestID,
            method: .nodeDetail,
            error: RuntimeError(
                code: .nodeNotFound,
                message: "Node not found",
                recoverySuggestion: nil
            )
        )
        let codec = RuntimeMessageCodec()
        let decoded = try codec.decode(
            RuntimeResponse<RuntimeJSONObject>.self,
            from: codec.encode(response)
        )

        XCTAssertEqual(decoded.method, .nodeDetail)
        XCTAssertEqual(decoded.status, .failure)
    }

    func testUnknownMethodRequestPreservesParameters() throws {
        let payload = Data(#"{"requestID":"00000000-0000-4000-8000-000000000011","protocolVersion":{"major":2,"minor":0},"method":"vendor.experimentalInspection","parameters":{"mode":"compact"}}"#.utf8)
        let request = try RuntimeMessageCodec().decode(
            RuntimeRequest<RuntimeJSONObject>.self,
            from: payload
        )

        XCTAssertEqual(request.method.rawValue, "vendor.experimentalInspection")
        XCTAssertEqual(request.parameters.values["mode"], .string("compact"))
    }

    func testTypedMessagesRejectMismatchedMethodsAndNonEmptyParameters() {
        let codec = RuntimeMessageCodec()
        let mismatchedRequest = Data(#"{"requestID":"00000000-0000-4000-8000-000000000002","protocolVersion":{"major":2,"minor":0},"method":"hierarchySnapshot","parameters":{}}"#.utf8)
        let nonEmptyParameters = Data(#"{"requestID":"00000000-0000-4000-8000-000000000002","protocolVersion":{"major":2,"minor":0},"method":"applicationInfo","parameters":{"unexpected":true}}"#.utf8)
        let mismatchedFailure = Data(#"{"requestID":"00000000-0000-4000-8000-000000000002","protocolVersion":{"major":2,"minor":0},"method":"nodeDetail","status":"failure","error":{"code":"nodeNotFound","message":"Node not found","recoverySuggestion":null}}"#.utf8)

        XCTAssertThrowsError(
            try codec.decode(RuntimeRequest<RuntimeApplicationInfoParameters>.self, from: mismatchedRequest)
        )
        XCTAssertThrowsError(
            try codec.decode(RuntimeRequest<RuntimeApplicationInfoParameters>.self, from: nonEmptyParameters)
        )
        XCTAssertThrowsError(
            try codec.decode(
                RuntimeResponse<RuntimeApplicationInfoPayload>.self,
                from: mismatchedFailure
            )
        )
    }

    func testCodecRejectsDuplicateKeysTrailingDataAndUnsafeInteger() {
        let codec = RuntimeMessageCodec()
        let invalidPayloads = [
            Data(#"{"method":"handshake","method":"nodeDetail"}"#.utf8),
            Data(#"{"method":"handshake","\u006dethod":"nodeDetail"}"#.utf8),
            Data(#"{"method":"handshake"} true"#.utf8),
            Data(#"{"value":9007199254740992}"#.utf8)
        ]

        for payload in invalidPayloads {
            XCTAssertThrowsError(try codec.decode(RuntimeJSONObject.self, from: payload))
        }
    }

    func testCodecRejectsScalarRootAndByteOrderMark() {
        let codec = RuntimeMessageCodec()
        var byteOrderMarked = Data([0xEF, 0xBB, 0xBF])
        byteOrderMarked.append(Data(#"{"value":1}"#.utf8))

        XCTAssertThrowsError(try codec.decode(RuntimeJSONObject.self, from: Data("[]".utf8)))
        XCTAssertThrowsError(try codec.decode(RuntimeJSONObject.self, from: byteOrderMarked))
    }

    func testRequiredNullableMembersDistinguishMissingFromNull() throws {
        let codec = RuntimeMessageCodec()
        let missing = Data(#"{"code":"internalFailure","message":"Failure"}"#.utf8)
        let explicitNull = Data(#"{"code":"internalFailure","message":"Failure","recoverySuggestion":null}"#.utf8)

        XCTAssertThrowsError(try codec.decode(RuntimeError.self, from: missing))
        let error = try codec.decode(RuntimeError.self, from: explicitNull)
        XCTAssertNil(error.recoverySuggestion)
        XCTAssertEqual(try codec.encode(error), explicitNull)
    }

    func testCodecHandlesDeepJSONWithoutRecursivePrevalidationFailure() throws {
        let acceptedDepth = 400
        let acceptedPayload = Data(
            (String(repeating: #"{"value":"#, count: acceptedDepth) +
             "null" +
             String(repeating: "}", count: acceptedDepth)).utf8
        )
        XCTAssertNoThrow(
            try RuntimeMessageCodec().decode(RuntimeJSONObject.self, from: acceptedPayload)
        )

        let rejectedDepth = 1_024
        let payload = Data(
            (String(repeating: #"{"value":"#, count: rejectedDepth) +
             "null" +
             String(repeating: "}", count: rejectedDepth)).utf8
        )
        XCTAssertThrowsError(
            try RuntimeMessageCodec().decode(RuntimeJSONObject.self, from: payload)
        ) {
            XCTAssertTrue($0 is DecodingError)
        }
    }
}
