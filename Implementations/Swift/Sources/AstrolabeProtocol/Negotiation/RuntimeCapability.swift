//
//  RuntimeCapability.swift
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/16.
//

public struct RuntimeCapability: Codable, Equatable, Hashable, Sendable {
    /// Open runtime capability identifier.
    public let rawValue: String

    public init(rawValue: String) throws {
        guard !rawValue.isEmpty, rawValue.count <= 128 else {
            throw RuntimeContractValueError.invalidOpenIdentifier(rawValue)
        }
        self.rawValue = rawValue
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        try self.init(rawValue: container.decode(String.self))
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        try container.encode(rawValue)
    }

    public static let applicationInfo = RuntimeCapability(uncheckedRawValue: "applicationInfo")
    public static let hierarchySnapshot = RuntimeCapability(uncheckedRawValue: "hierarchySnapshot")
    public static let nodeDetail = RuntimeCapability(uncheckedRawValue: "nodeDetail")
    public static let attributePatchDiscovery = RuntimeCapability(uncheckedRawValue: "attributePatchDiscovery")
    public static let attributePatching = RuntimeCapability(uncheckedRawValue: "attributePatching")
    public static let requestCancellation = RuntimeCapability(uncheckedRawValue: "requestCancellation")

    private init(uncheckedRawValue: String) {
        rawValue = uncheckedRawValue
    }
}
