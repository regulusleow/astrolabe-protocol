//
//  RuntimeNodeDetail.swift
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/16.
//

public struct RuntimeAttributeIdentifier: Codable, Equatable, Hashable, Sendable {
    /// Namespaced attribute identifier.
    public let rawValue: String

    public init(rawValue: String) throws {
        self.rawValue = try RuntimeNamespacedIdentifier(rawValue: rawValue).rawValue
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        try self.init(rawValue: container.decode(String.self))
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        try container.encode(rawValue)
    }
}

public struct RuntimeAttributeCategory: Codable, Equatable, Hashable, Sendable {
    /// Namespaced detail-section category.
    public let rawValue: String

    public init(rawValue: String) throws {
        self.rawValue = try RuntimeNamespacedIdentifier(rawValue: rawValue).rawValue
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        try self.init(rawValue: container.decode(String.self))
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        try container.encode(rawValue)
    }
}

public struct RuntimeNodeDetailParameters: Codable, Equatable, Sendable, RuntimeMethodBound {
    /// Node whose detail sections are requested.
    public let nodeID: RuntimeOpaqueIdentifier

    public init(nodeID: RuntimeOpaqueIdentifier) {
        self.nodeID = nodeID
    }

    public static let runtimeMethod = RuntimeMethod.nodeDetail
}

public struct RuntimeAttribute: Codable, Equatable, Sendable {
    /// Namespaced semantic or platform attribute identifier.
    public let identifier: RuntimeAttributeIdentifier

    /// Typed attribute value.
    public let value: RuntimeAttributeValue

    public init(identifier: RuntimeAttributeIdentifier, value: RuntimeAttributeValue) {
        self.identifier = identifier
        self.value = value
    }
}

public struct RuntimeAttributeSection: Codable, Equatable, Sendable {
    /// Namespaced category grouping related attributes.
    public let category: RuntimeAttributeCategory

    /// Attributes collected for this category.
    public let attributes: [RuntimeAttribute]

    public init(category: RuntimeAttributeCategory, attributes: [RuntimeAttribute]) {
        self.category = category
        self.attributes = attributes
    }
}

public struct RuntimeNodeDetailPayload: Codable, Equatable, Sendable, RuntimeMethodBound {
    /// Node represented by these detail sections.
    public let nodeID: RuntimeOpaqueIdentifier

    /// Ordered semantic and platform detail sections.
    public let sections: [RuntimeAttributeSection]

    /// Optional namespaced detail metadata.
    public let extensions: RuntimeExtensionMap?

    public init(
        nodeID: RuntimeOpaqueIdentifier,
        sections: [RuntimeAttributeSection],
        extensions: RuntimeExtensionMap? = nil
    ) {
        self.nodeID = nodeID
        self.sections = sections
        self.extensions = extensions
    }

    public static let runtimeMethod = RuntimeMethod.nodeDetail
}
