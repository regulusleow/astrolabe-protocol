//
//  RuntimeLayoutRelation.kt
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/20.
//

package dev.astrolabe.protocol

import kotlinx.serialization.Serializable

/** Node-owned normalized layout anchor. */
@Serializable
public data class RuntimeLayoutAnchor(
    /** Node owning this layout anchor. */
    public val nodeID: RuntimeOpaqueIdentifier,
    /** Open normalized anchor name. */
    public val anchor: String
) {
    init {
        require(anchor.isNotEmpty() && anchor.length <= MAXIMUM_LAYOUT_ANCHOR_LENGTH) {
            "Layout anchor must contain between 1 and 128 characters"
        }
    }
}

/** Closed comparison relation used by a layout constraint. */
@Serializable
public enum class RuntimeLayoutRelationKind {
    lessThanOrEqual,
    equal,
    greaterThanOrEqual
}

/** Platform-neutral layout relation. */
@Serializable
public data class RuntimeLayoutRelation(
    /** Producer-defined relation identifier when available. */
    public val identifier: String?,
    /** Source node anchor constrained by the relation. */
    public val source: RuntimeLayoutAnchor,
    /** Closed comparison relation. */
    public val relation: RuntimeLayoutRelationKind,
    /** Target node anchor, or null for a constant relation. */
    public val target: RuntimeLayoutAnchor?,
    /** Target coefficient applied before offset. */
    public val multiplier: Double,
    /** Logical-unit constant offset. */
    public val offset: RuntimeMeasurement,
    /** Normalized relation strength when available. */
    public val strength: Double?,
    /** Active state when reported by the platform. */
    public val active: Boolean?,
    /** Namespaced platform-specific relation facts. */
    public val extensions: RuntimeExtensionMap
) {
    init {
        require(strength == null || strength in 0.0..1.0) {
            "Layout relation strength must be between zero and one"
        }
        require(offset.unit == RuntimeMeasurementUnit.logical) {
            "Layout relation offset must use logical units"
        }
    }
}

private const val MAXIMUM_LAYOUT_ANCHOR_LENGTH: Int = 128
