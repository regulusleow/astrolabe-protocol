//
//  RuntimeNodeRelation.kt
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/8/4.
//

package dev.astrolabe.protocol

import kotlinx.serialization.Serializable

/** Directed platform-neutral relation between two captured Runtime nodes. */
@Serializable
public data class RuntimeNodeRelation(
    /** Open namespaced relation type owned by its producer. */
    public val type: RuntimeNamespacedIdentifier,
    /** Source node identifier for this directed relation. */
    public val sourceNodeID: RuntimeOpaqueIdentifier,
    /** Target node identifier for this directed relation. */
    public val targetNodeID: RuntimeOpaqueIdentifier,
    /** Namespaced producer-specific relation facts. */
    public val extensions: RuntimeExtensionMap
)
