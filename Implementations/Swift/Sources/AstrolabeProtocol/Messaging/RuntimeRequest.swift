//
//  RuntimeRequest.swift
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/16.
//

import Foundation

public struct RuntimeRequestHeader: Decodable, Equatable, Sendable {
    /// Correlates the request with its response.
    public let requestID: UUID

    /// Protocol version used by this message.
    public let protocolVersion: RuntimeProtocolVersion

    /// Operation requested from the Runtime.
    public let method: RuntimeMethod

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        requestID = try container.decode(UUID.self, forKey: .requestID)
        protocolVersion = try container.decode(RuntimeProtocolVersion.self, forKey: .protocolVersion)
        guard protocolVersion == .v2 else {
            throw RuntimeProtocolVersionError.unsupportedVersion(protocolVersion)
        }
        method = try container.decode(RuntimeMethod.self, forKey: .method)
    }

    private enum CodingKeys: String, CodingKey {
        case requestID
        case protocolVersion
        case method
    }
}

public struct RuntimeRequest<Parameters: Codable & Equatable & Sendable>: Codable, Equatable, Sendable {
    /// Correlates the request with its response.
    public let requestID: UUID

    /// Protocol version used by this message.
    public let protocolVersion: RuntimeProtocolVersion

    /// Operation requested from the Runtime.
    public let method: RuntimeMethod

    /// Method-specific parameter object.
    public let parameters: Parameters

    public init(
        requestID: UUID = UUID(),
        method: RuntimeMethod,
        parameters: Parameters
    ) throws {
        try Self.validateMethod(method)
        self.requestID = requestID
        protocolVersion = .v2
        self.method = method
        self.parameters = parameters
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
        parameters = try container.decode(Parameters.self, forKey: .parameters)
    }

    public func encode(to encoder: Encoder) throws {
        try Self.validateMethod(method)
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(requestID, forKey: .requestID)
        try container.encode(protocolVersion, forKey: .protocolVersion)
        try container.encode(method, forKey: .method)
        try container.encode(parameters, forKey: .parameters)
    }

    private static func validateMethod(_ method: RuntimeMethod) throws {
        guard let boundType = Parameters.self as? any RuntimeMethodBound.Type else { return }
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
        case parameters
    }
}

public struct RuntimeApplicationInfoParameters: RuntimeEmptyMethodParameters {
    public static let runtimeMethod = RuntimeMethod.applicationInfo
    public init() {}
}

public struct RuntimeHierarchySnapshotParameters: RuntimeEmptyMethodParameters {
    public static let runtimeMethod = RuntimeMethod.hierarchySnapshot
    public init() {}
}

public struct RuntimePatchableAttributesParameters: RuntimeEmptyMethodParameters {
    public static let runtimeMethod = RuntimeMethod.patchableAttributes
    public init() {}
}

public struct RuntimeListAttributePatchesParameters: RuntimeEmptyMethodParameters {
    public static let runtimeMethod = RuntimeMethod.listAttributePatches
    public init() {}
}

public struct RuntimeClearAttributePatchesParameters: RuntimeEmptyMethodParameters {
    public static let runtimeMethod = RuntimeMethod.clearAttributePatches
    public init() {}
}

public protocol RuntimeEmptyMethodParameters: Codable, Equatable, Sendable, RuntimeMethodBound {
    init()
}

public extension RuntimeEmptyMethodParameters {
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: RuntimeDynamicCodingKey.self)
        guard container.allKeys.isEmpty else {
            throw RuntimeMessageRoutingError.nonEmptyParameters
        }
        self.init()
    }

    func encode(to encoder: Encoder) throws {
        _ = encoder.container(keyedBy: RuntimeDynamicCodingKey.self)
    }

}

public struct RuntimeCancelRequestParameters: Codable, Equatable, Sendable, RuntimeMethodBound {
    /// Request identifier whose in-flight operation should be cancelled.
    public let targetRequestID: UUID

    public init(targetRequestID: UUID) {
        self.targetRequestID = targetRequestID
    }

    public static let runtimeMethod = RuntimeMethod.cancelRequest
}

public struct RuntimeCancelRequestPayload: Codable, Equatable, Sendable, RuntimeMethodBound {
    /// Request identifier supplied by the cancellation request.
    public let targetRequestID: UUID

    /// Whether an active operation accepted cancellation.
    public let cancellationAccepted: Bool

    public init(targetRequestID: UUID, cancellationAccepted: Bool) {
        self.targetRequestID = targetRequestID
        self.cancellationAccepted = cancellationAccepted
    }

    public static let runtimeMethod = RuntimeMethod.cancelRequest
}

public enum RuntimeMessageRoutingError: Error, Equatable, Sendable {
    case methodMismatch(expected: RuntimeMethod, actual: RuntimeMethod)
    case nonEmptyParameters
}

private struct RuntimeDynamicCodingKey: CodingKey {
    let stringValue: String
    let intValue: Int?

    init?(stringValue: String) {
        self.stringValue = stringValue
        intValue = nil
    }

    init?(intValue: Int) {
        stringValue = String(intValue)
        self.intValue = intValue
    }
}
