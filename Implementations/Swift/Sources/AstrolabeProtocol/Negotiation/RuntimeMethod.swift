//
//  RuntimeMethod.swift
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/16.
//

public struct RuntimeMethod: Codable, Equatable, Hashable, Sendable {
    /// Open wire method identifier.
    public let rawValue: String

    public init(rawValue: String) throws {
        guard !rawValue.isEmpty, rawValue.count <= 256 else {
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

    public static let handshake = RuntimeMethod(uncheckedRawValue: "handshake")
    public static let applicationInfo = RuntimeMethod(uncheckedRawValue: "applicationInfo")
    public static let hierarchySnapshot = RuntimeMethod(uncheckedRawValue: "hierarchySnapshot")
    public static let nodeDetail = RuntimeMethod(uncheckedRawValue: "nodeDetail")
    public static let patchableAttributes = RuntimeMethod(uncheckedRawValue: "patchableAttributes")
    public static let applyAttributePatch = RuntimeMethod(uncheckedRawValue: "applyAttributePatch")
    public static let listAttributePatches = RuntimeMethod(uncheckedRawValue: "listAttributePatches")
    public static let revertAttributePatch = RuntimeMethod(uncheckedRawValue: "revertAttributePatch")
    public static let clearAttributePatches = RuntimeMethod(uncheckedRawValue: "clearAttributePatches")
    public static let cancelRequest = RuntimeMethod(uncheckedRawValue: "cancelRequest")

    private init(uncheckedRawValue: String) {
        rawValue = uncheckedRawValue
    }
}

public protocol RuntimeMethodBound {
    /// Wire method represented by the conforming parameters or payload.
    static var runtimeMethod: RuntimeMethod { get }
}
