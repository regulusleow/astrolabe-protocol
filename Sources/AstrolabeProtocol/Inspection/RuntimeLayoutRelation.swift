//
//  RuntimeLayoutRelation.swift
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/16.
//

public struct RuntimeLayoutAnchor: Codable, Equatable, Sendable {
    /// Node owning this layout anchor.
    public let nodeID: RuntimeOpaqueIdentifier

    /// Open normalized anchor name.
    public let anchor: String

    public init(nodeID: RuntimeOpaqueIdentifier, anchor: String) {
        self.nodeID = nodeID
        self.anchor = anchor
    }
}

public enum RuntimeLayoutRelationKind: String, Codable, Sendable {
    case lessThanOrEqual
    case equal
    case greaterThanOrEqual
}

public struct RuntimeLayoutRelation: Codable, Equatable, Sendable {
    /// Producer-defined relation identifier when available.
    @RuntimeRequiredNullable public private(set) var identifier: String?

    /// Source node anchor constrained by the relation.
    public let source: RuntimeLayoutAnchor

    /// Closed comparison relation.
    public let relation: RuntimeLayoutRelationKind

    /// Target node anchor, or nil for a constant relation.
    @RuntimeRequiredNullable public private(set) var target: RuntimeLayoutAnchor?

    /// Target coefficient applied before offset.
    public let multiplier: Double

    /// Logical-unit constant offset.
    public let offset: RuntimeMeasurement

    /// Normalized relation strength from zero to one when available.
    @RuntimeRequiredNullable public private(set) var strength: Double?

    /// Active state when reported by the platform.
    @RuntimeRequiredNullable public private(set) var active: Bool?

    /// Namespaced platform-specific relation facts.
    public let extensions: RuntimeExtensionMap

    public init(
        identifier: String?,
        source: RuntimeLayoutAnchor,
        relation: RuntimeLayoutRelationKind,
        target: RuntimeLayoutAnchor?,
        multiplier: Double,
        offset: RuntimeMeasurement,
        strength: Double?,
        active: Bool?,
        extensions: RuntimeExtensionMap
    ) {
        self.identifier = identifier
        self.source = source
        self.relation = relation
        self.target = target
        self.multiplier = multiplier
        self.offset = offset
        self.strength = strength
        self.active = active
        self.extensions = extensions
    }
}
