//
//  RuntimeNodeRelation.swift
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/8/4.
//

/// Directed platform-neutral relation between two captured Runtime nodes.
public struct RuntimeNodeRelation: Codable, Equatable, Sendable {
    /// Open namespaced relation type owned by its producer.
    public let type: RuntimeNamespacedIdentifier

    /// Source node identifier for this directed relation.
    public let sourceNodeID: RuntimeOpaqueIdentifier

    /// Target node identifier for this directed relation.
    public let targetNodeID: RuntimeOpaqueIdentifier

    /// Namespaced producer-specific relation facts.
    public let extensions: RuntimeExtensionMap

    public init(
        type: RuntimeNamespacedIdentifier,
        sourceNodeID: RuntimeOpaqueIdentifier,
        targetNodeID: RuntimeOpaqueIdentifier,
        extensions: RuntimeExtensionMap
    ) {
        self.type = type
        self.sourceNodeID = sourceNodeID
        self.targetNodeID = targetNodeID
        self.extensions = extensions
    }
}
