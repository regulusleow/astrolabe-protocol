//
//  RuntimeHierarchy.kt
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/20.
//

package dev.astrolabe.protocol

import kotlinx.serialization.Serializable

/** Platform runtime type and its ancestor chain. */
@Serializable
public data class RuntimeType(
    /** Most-specific platform runtime type name. */
    public val name: String,
    /** Runtime ancestor names ordered from direct parent to root type. */
    public val ancestors: List<String>
) {
    init {
        require(name.isNotEmpty() && name.length <= MAXIMUM_RUNTIME_TYPE_LENGTH) {
            "Runtime type name must contain between 1 and 512 characters"
        }
        require(ancestors.all { it.isNotEmpty() && it.length <= MAXIMUM_RUNTIME_TYPE_LENGTH }) {
            "Runtime type ancestors must contain between 1 and 512 characters"
        }
    }
}

/** Node geometry in local, parent, and screen coordinate spaces. */
@Serializable
public data class RuntimeNodeGeometry(
    /** Node bounds in local logical coordinates. */
    public val bounds: RuntimeCoordinateRect,
    /** Frame in direct-parent logical coordinates, or null for a root. */
    public val frameInParent: RuntimeCoordinateRect?,
    /** Frame in screen logical coordinates. */
    public val frameInScreen: RuntimeCoordinateRect
) {
    init {
        require(
            bounds.coordinateSpace == RuntimeCoordinateSpace.local &&
                bounds.unit == RuntimeMeasurementUnit.logical
        ) { "Node bounds must use local logical coordinates" }
        require(
            frameInParent == null ||
                frameInParent.coordinateSpace == RuntimeCoordinateSpace.parent &&
                frameInParent.unit == RuntimeMeasurementUnit.logical
        ) { "Parent frame must use parent logical coordinates" }
        require(
            frameInScreen.coordinateSpace == RuntimeCoordinateSpace.screen &&
                frameInScreen.unit == RuntimeMeasurementUnit.logical
        ) { "Screen frame must use screen logical coordinates" }
    }
}

/** Explicit visibility causes and the derived final result. */
@Serializable
public data class RuntimeNodeVisibility(
    /** Whether the node explicitly hides itself. */
    public val hidden: Boolean,
    /** Whether an ancestor explicitly hides the node. */
    public val hiddenByAncestor: Boolean,
    /** Node-local opacity in the inclusive range from zero to one. */
    public val opacity: Double,
    /** Opacity after ancestor effects in the inclusive range from zero to one. */
    public val effectiveOpacity: Double,
    /** Whether the screen frame intersects the application viewport. */
    public val intersectsViewport: Boolean,
    /** Whether ancestor clipping fully removes the node. */
    public val fullyClippedByAncestor: Boolean,
    /** Final visibility result derived from all causes. */
    public val onscreen: Boolean
) {
    init {
        require(opacity in 0.0..1.0 && effectiveOpacity in 0.0..1.0) {
            "Visibility opacity must be between zero and one"
        }
        val derivedOnscreen = !hidden &&
            !hiddenByAncestor &&
            effectiveOpacity > VISIBILITY_THRESHOLD &&
            intersectsViewport &&
            !fullyClippedByAncestor
        require(onscreen == derivedOnscreen) { "Onscreen does not match the derived visibility state" }
    }
}

/** Accessibility facts exposed by one node. */
@Serializable
public data class RuntimeAccessibility(
    /** Whether the node is an accessibility element. */
    public val element: Boolean,
    /** Developer-provided accessibility identifier. */
    public val identifier: String?,
    /** Accessibility label. */
    public val label: String?,
    /** Accessibility value. */
    public val value: String?,
    /** Accessibility hint. */
    public val hint: String?,
    /** Open normalized accessibility traits. */
    public val traits: List<String>
)

/** Normalized interaction state for one node. */
@Serializable
public data class RuntimeInteraction(
    /** Whether the node accepts direct interaction. */
    public val interactive: Boolean,
    /** Enabled state when the role exposes one. */
    public val enabled: Boolean?,
    /** Selected state when the role exposes one. */
    public val selected: Boolean?,
    /** Focus state when the platform reports one. */
    public val focused: Boolean?
)

/** One platform-neutral node captured in a runtime hierarchy. */
@Serializable
public data class RuntimeNode(
    /** Opaque node identifier scoped to the Runtime process. */
    public val nodeID: RuntimeOpaqueIdentifier,
    /** Parent node identifier, or null for a hierarchy root. */
    public val parentID: RuntimeOpaqueIdentifier?,
    /** Position in the parent's ordered child collection. */
    public val siblingIndex: Int,
    /** Open platform-neutral semantic role. */
    public val role: String,
    /** Platform runtime type facts. */
    public val runtimeType: RuntimeType,
    /** Geometry in local, parent, and screen coordinate spaces. */
    public val geometry: RuntimeNodeGeometry,
    /** Explicit visibility causes and final result. */
    public val visibility: RuntimeNodeVisibility,
    /** Whether this node clips descendant content. */
    public val clipsContent: Boolean,
    /** Resolved background color when available. */
    public val backgroundColor: RuntimeColor?,
    /** Short text preview when available. */
    public val text: String?,
    /** Accessibility facts when available. */
    public val accessibility: RuntimeAccessibility?,
    /** Normalized interaction state. */
    public val interaction: RuntimeInteraction,
    /** Namespaced detail categories available for this node. */
    public val availableDetailCategories: List<RuntimeNamespacedIdentifier>,
    /** Namespaced platform-specific node facts. */
    public val extensions: RuntimeExtensionMap,
    /** Ordered child nodes captured in the same snapshot. */
    public val children: List<RuntimeNode>
) {
    init {
        require(siblingIndex >= 0) { "Sibling index cannot be negative" }
        require(role.isNotEmpty() && role.length <= MAXIMUM_NODE_ROLE_LENGTH) {
            "Node role must contain between 1 and 128 characters"
        }
        require(availableDetailCategories.distinct().size == availableDetailCategories.size) {
            "Available detail categories cannot contain duplicates"
        }
    }
}

/** Empty parameters for capturing a hierarchy snapshot. */
@Serializable
public data object RuntimeHierarchySnapshotParameters {
    /** Typed request contract for the hierarchy-snapshot method. */
    public val contract: RuntimeMethodContract<RuntimeHierarchySnapshotParameters> by lazy {
        RuntimeMethodContract(RuntimeMethod.hierarchySnapshot, serializer())
    }
}

/** Successful hierarchy-snapshot response payload. */
@Serializable
public data class RuntimeHierarchySnapshotPayload(
    /** Opaque identifier for this immutable hierarchy capture. */
    public val snapshotID: RuntimeOpaqueIdentifier,
    /** Capture time in seconds since the Unix epoch. */
    public val capturedAtUnixTime: Double,
    /** Process-instance identifier represented by this snapshot. */
    public val targetIdentifier: RuntimeOpaqueIdentifier,
    /** Open interface-orientation identifier. */
    public val orientation: String,
    /** Display facts used by the capture. */
    public val display: RuntimeDisplayInfo,
    /** Application viewport in screen logical coordinates. */
    public val viewport: RuntimeCoordinateRect,
    /** Ordered hierarchy roots. */
    public val roots: List<RuntimeNode>,
    /** Directed cross-tree relations captured with this hierarchy. */
    public val relations: List<RuntimeNodeRelation>? = null,
    /** Optional namespaced snapshot facts. */
    public val extensions: RuntimeExtensionMap? = null
) {
    init {
        require(capturedAtUnixTime >= 0.0) { "Hierarchy capture time cannot be negative" }
        require(orientation.isNotEmpty() && orientation.length <= MAXIMUM_ORIENTATION_LENGTH) {
            "Orientation must contain between 1 and 64 characters"
        }
        require(
            viewport.coordinateSpace == RuntimeCoordinateSpace.screen &&
                viewport.unit == RuntimeMeasurementUnit.logical
        ) { "Hierarchy viewport must use screen logical coordinates" }
    }

    public companion object {
        /** Typed success-payload contract for the hierarchy-snapshot method. */
        public val contract: RuntimeMethodContract<RuntimeHierarchySnapshotPayload> by lazy {
            RuntimeMethodContract(RuntimeMethod.hierarchySnapshot, serializer())
        }
    }
}

private const val VISIBILITY_THRESHOLD: Double = 0.01
private const val MAXIMUM_RUNTIME_TYPE_LENGTH: Int = 512
private const val MAXIMUM_NODE_ROLE_LENGTH: Int = 128
private const val MAXIMUM_ORIENTATION_LENGTH: Int = 64
