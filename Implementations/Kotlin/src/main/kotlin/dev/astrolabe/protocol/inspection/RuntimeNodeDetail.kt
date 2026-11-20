//
//  RuntimeNodeDetail.kt
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/20.
//

package dev.astrolabe.protocol

import kotlinx.serialization.Serializable

/** Namespaced semantic or platform attribute identifier. */
@JvmInline
@Serializable
public value class RuntimeAttributeIdentifier(
    /** Raw namespaced attribute identifier. */
    public val rawValue: String
) {
    init {
        require(RuntimeNamespacedIdentifier.isValid(rawValue)) { "Attribute identifier must be namespaced" }
    }
}

/** Namespaced category grouping related detail attributes. */
@JvmInline
@Serializable
public value class RuntimeAttributeCategory(
    /** Raw namespaced category identifier. */
    public val rawValue: String
) {
    init {
        require(RuntimeNamespacedIdentifier.isValid(rawValue)) { "Attribute category must be namespaced" }
    }
}

/** Parameters for reading one node's detail sections. */
@Serializable
public data class RuntimeNodeDetailParameters(
    /** Node whose detail sections are requested. */
    public val nodeID: RuntimeOpaqueIdentifier,
    /** Runtime snapshot owning frozen semantic detail; omitted for live render detail. */
    public val snapshotID: RuntimeOpaqueIdentifier? = null
) {
    // Keep the constructor and copy descriptors used by already compiled Protocol 2.1 clients.
    @Deprecated("Binary compatibility with Protocol 2.1", level = DeprecationLevel.HIDDEN)
    public constructor(
        nodeID: RuntimeOpaqueIdentifier
    ) : this(nodeID, null)

    @Deprecated("Binary compatibility with Protocol 2.1", level = DeprecationLevel.HIDDEN)
    public fun copy(
        nodeID: RuntimeOpaqueIdentifier = this.nodeID
    ): RuntimeNodeDetailParameters = RuntimeNodeDetailParameters(nodeID, this.snapshotID)

    public companion object {
        /** Typed request contract for the node-detail method. */
        public val contract: RuntimeMethodContract<RuntimeNodeDetailParameters> by lazy {
            RuntimeMethodContract(RuntimeMethod.nodeDetail, serializer())
        }
    }
}

/** One typed runtime attribute. */
@Serializable
public data class RuntimeAttribute(
    /** Namespaced semantic or platform attribute identifier. */
    public val identifier: RuntimeAttributeIdentifier,
    /** Typed attribute value. */
    public val value: RuntimeAttributeValue
)

/** Ordered attributes belonging to one detail category. */
@Serializable
public data class RuntimeAttributeSection(
    /** Namespaced category grouping related attributes. */
    public val category: RuntimeAttributeCategory,
    /** Attributes collected for this category. */
    public val attributes: List<RuntimeAttribute>
)

/** Successful node-detail response payload. */
@Serializable
public data class RuntimeNodeDetailPayload(
    /** Node represented by these detail sections. */
    public val nodeID: RuntimeOpaqueIdentifier,
    /** Ordered semantic and platform detail sections. */
    public val sections: List<RuntimeAttributeSection>,
    /** Optional namespaced detail metadata. */
    public val extensions: RuntimeExtensionMap? = null,
    /** Frozen semantic facts and original Runtime capture identity. */
    public val semanticContext: RuntimeSemanticDetailContext? = null
) {
    // Keep the constructor and copy descriptors used by already compiled Protocol 2.1 clients.
    @Deprecated("Binary compatibility with Protocol 2.1", level = DeprecationLevel.HIDDEN)
    public constructor(
        nodeID: RuntimeOpaqueIdentifier,
        sections: List<RuntimeAttributeSection>,
        extensions: RuntimeExtensionMap? = null
    ) : this(nodeID, sections, extensions, null)

    @Deprecated("Binary compatibility with Protocol 2.1", level = DeprecationLevel.HIDDEN)
    public fun copy(
        nodeID: RuntimeOpaqueIdentifier = this.nodeID,
        sections: List<RuntimeAttributeSection> = this.sections,
        extensions: RuntimeExtensionMap? = this.extensions
    ): RuntimeNodeDetailPayload = RuntimeNodeDetailPayload(nodeID, sections, extensions, this.semanticContext)

    public companion object {
        /** Typed success-payload contract for the node-detail method. */
        public val contract: RuntimeMethodContract<RuntimeNodeDetailPayload> by lazy {
            RuntimeMethodContract(RuntimeMethod.nodeDetail, serializer())
        }
    }
}
