//
//  RuntimeResponse.swift
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/16.
//

import Foundation

public enum RuntimeResponseStatus: String, Codable, Sendable {
    case success
    case failure
}

public enum RuntimeResponseOutcome<Payload: Codable & Equatable & Sendable>: Equatable, Sendable {
    case success(Payload)
    case failure(RuntimeError)
}

public struct RuntimeResponseHeader: Decodable, Equatable, Sendable {
    /// Request identifier copied from the request.
    public let requestID: UUID

    /// Protocol version used by this message.
    public let protocolVersion: RuntimeProtocolVersion

    /// Method copied from the request.
    public let method: RuntimeMethod

    /// Selects the payload or error outcome.
    public let status: RuntimeResponseStatus

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        requestID = try container.decode(UUID.self, forKey: .requestID)
        protocolVersion = try container.decode(RuntimeProtocolVersion.self, forKey: .protocolVersion)
        guard protocolVersion == .v2 else {
            throw RuntimeProtocolVersionError.unsupportedVersion(protocolVersion)
        }
        method = try container.decode(RuntimeMethod.self, forKey: .method)
        status = try container.decode(RuntimeResponseStatus.self, forKey: .status)
    }

    private enum CodingKeys: String, CodingKey {
        case requestID
        case protocolVersion
        case method
        case status
    }
}

public struct RuntimeResponse<Payload: Codable & Equatable & Sendable>: Codable, Equatable, Sendable {
    /// Request identifier copied from the request.
    public let requestID: UUID

    /// Protocol version used by this message.
    public let protocolVersion: RuntimeProtocolVersion

    /// Method copied from the request.
    public let method: RuntimeMethod

    /// Typed success or failure result.
    public let outcome: RuntimeResponseOutcome<Payload>

    public var status: RuntimeResponseStatus {
        switch outcome {
        case .success: return .success
        case .failure: return .failure
        }
    }

    public static func success(
        requestID: UUID,
        method: RuntimeMethod,
        payload: Payload
    ) throws -> Self {
        try validateMethod(method)
        return Self(requestID: requestID, method: method, outcome: .success(payload))
    }

    public static func failure(
        requestID: UUID,
        method: RuntimeMethod,
        error: RuntimeError
    ) throws -> Self {
        try validateMethod(method)
        return Self(requestID: requestID, method: method, outcome: .failure(error))
    }

    private init(
        requestID: UUID,
        method: RuntimeMethod,
        outcome: RuntimeResponseOutcome<Payload>
    ) {
        self.requestID = requestID
        protocolVersion = .v2
        self.method = method
        self.outcome = outcome
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        requestID = try container.decode(UUID.self, forKey: .requestID)
        let protocolVersion = try container.decode(RuntimeProtocolVersion.self, forKey: .protocolVersion)
        guard protocolVersion == .v2 else {
            throw RuntimeProtocolVersionError.unsupportedVersion(protocolVersion)
        }
        self.protocolVersion = protocolVersion
        method = try container.decode(RuntimeMethod.self, forKey: .method)
        try Self.validateMethod(method)
        let status = try container.decode(RuntimeResponseStatus.self, forKey: .status)

        switch status {
        case .success:
            guard !container.contains(.error) else {
                throw DecodingError.dataCorruptedError(
                    forKey: .error,
                    in: container,
                    debugDescription: "A successful response cannot contain an error"
                )
            }
            outcome = .success(try container.decode(Payload.self, forKey: .payload))
        case .failure:
            guard !container.contains(.payload) else {
                throw DecodingError.dataCorruptedError(
                    forKey: .payload,
                    in: container,
                    debugDescription: "A failed response cannot contain a payload"
                )
            }
            outcome = .failure(try container.decode(RuntimeError.self, forKey: .error))
        }
    }

    public func encode(to encoder: Encoder) throws {
        try Self.validateMethod(method)
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(requestID, forKey: .requestID)
        try container.encode(protocolVersion, forKey: .protocolVersion)
        try container.encode(method, forKey: .method)
        switch outcome {
        case let .success(payload):
            try container.encode(RuntimeResponseStatus.success, forKey: .status)
            try container.encode(payload, forKey: .payload)
        case let .failure(error):
            try container.encode(RuntimeResponseStatus.failure, forKey: .status)
            try container.encode(error, forKey: .error)
        }
    }

    private static func validateMethod(_ method: RuntimeMethod) throws {
        guard let boundType = Payload.self as? any RuntimeMethodBound.Type else { return }
        guard method == boundType.runtimeMethod else {
            throw RuntimeMessageRoutingError.methodMismatch(
                expected: boundType.runtimeMethod,
                actual: method
            )
        }
    }

    private enum CodingKeys: String, CodingKey {
        case requestID
        case protocolVersion
        case method
        case status
        case payload
        case error
    }
}
