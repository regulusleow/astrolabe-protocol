//
//  RuntimeHierarchy.swift
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/16.
//

public struct RuntimeType: Codable, Equatable, Sendable {
    /// Most-specific platform runtime type name.
    public let name: String

    /// Runtime ancestor names ordered from direct parent to root type.
    public let ancestors: [String]

    public init(name: String, ancestors: [String]) {
        self.name = name
        self.ancestors = ancestors
    }
}

public struct RuntimeNodeGeometry: Codable, Equatable, Sendable {
    /// Node bounds in local logical coordinates.
    public let bounds: RuntimeCoordinateRect

    /// Frame in direct-parent logical coordinates, or nil for a root.
    @RuntimeRequiredNullable public private(set) var frameInParent: RuntimeCoordinateRect?

    /// Frame in screen logical coordinates.
    public let frameInScreen: RuntimeCoordinateRect

    public init(
        bounds: RuntimeCoordinateRect,
        frameInParent: RuntimeCoordinateRect?,
        frameInScreen: RuntimeCoordinateRect
    ) {
        self.bounds = bounds
        self.frameInParent = frameInParent
        self.frameInScreen = frameInScreen
    }
}

public struct RuntimeNodeVisibility: Codable, Equatable, Sendable {
    /// Whether the node explicitly hides itself.
    public let hidden: Bool

    /// Whether an ancestor explicitly hides the node.
    public let hiddenByAncestor: Bool

    /// Node-local opacity in the inclusive range from zero to one.
    public let opacity: Double

    /// Opacity after ancestor effects in the inclusive range from zero to one.
    public let effectiveOpacity: Double

    /// Whether the screen frame intersects the App viewport.
    public let intersectsViewport: Bool

    /// Whether ancestor clipping fully removes the node.
    public let fullyClippedByAncestor: Bool

    /// Final visibility result derived from all causes.
    public let onscreen: Bool

    public init(
        hidden: Bool,
        hiddenByAncestor: Bool,
        opacity: Double,
        effectiveOpacity: Double,
        intersectsViewport: Bool,
        fullyClippedByAncestor: Bool,
        onscreen: Bool
    ) throws {
        let derivedOnscreen = !hidden && !hiddenByAncestor && effectiveOpacity > 0.01 &&
            intersectsViewport && !fullyClippedByAncestor
        guard onscreen == derivedOnscreen else {
            throw RuntimeHierarchyValidationError.inconsistentVisibility
        }
        self.hidden = hidden
        self.hiddenByAncestor = hiddenByAncestor
        self.opacity = opacity
        self.effectiveOpacity = effectiveOpacity
        self.intersectsViewport = intersectsViewport
        self.fullyClippedByAncestor = fullyClippedByAncestor
        self.onscreen = onscreen
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        try self.init(
            hidden: container.decode(Bool.self, forKey: .hidden),
            hiddenByAncestor: container.decode(Bool.self, forKey: .hiddenByAncestor),
            opacity: container.decode(Double.self, forKey: .opacity),
            effectiveOpacity: container.decode(Double.self, forKey: .effectiveOpacity),
            intersectsViewport: container.decode(Bool.self, forKey: .intersectsViewport),
            fullyClippedByAncestor: container.decode(Bool.self, forKey: .fullyClippedByAncestor),
            onscreen: container.decode(Bool.self, forKey: .onscreen)
        )
    }

    private enum CodingKeys: String, CodingKey {
        case hidden
        case hiddenByAncestor
        case opacity
        case effectiveOpacity
        case intersectsViewport
        case fullyClippedByAncestor
        case onscreen
    }
}

public struct RuntimeAccessibility: Codable, Equatable, Sendable {
    /// Whether the node is an accessibility element.
    public let element: Bool

    /// Developer-provided accessibility identifier.
    @RuntimeRequiredNullable public private(set) var identifier: String?

    /// Accessibility label.
    @RuntimeRequiredNullable public private(set) var label: String?

    /// Accessibility value.
    @RuntimeRequiredNullable public private(set) var value: String?

    /// Accessibility hint.
    @RuntimeRequiredNullable public private(set) var hint: String?

    /// Open normalized accessibility traits.
    public let traits: [String]

    public init(
        element: Bool,
        identifier: String?,
        label: String?,
        value: String?,
        hint: String?,
        traits: [String]
    ) {
        self.element = element
        self.identifier = identifier
        self.label = label
        self.value = value
        self.hint = hint
        self.traits = traits
    }
}

public struct RuntimeInteraction: Codable, Equatable, Sendable {
    /// Whether the node accepts direct interaction.
    public let interactive: Bool

    /// Enabled state when the role exposes one.
    @RuntimeRequiredNullable public private(set) var enabled: Bool?

    /// Selected state when the role exposes one.
    @RuntimeRequiredNullable public private(set) var selected: Bool?

    /// Focus state when the platform reports one.
    @RuntimeRequiredNullable public private(set) var focused: Bool?

    public init(interactive: Bool, enabled: Bool?, selected: Bool?, focused: Bool?) {
        self.interactive = interactive
        self.enabled = enabled
        self.selected = selected
        self.focused = focused
    }
}

public struct RuntimeNode: Codable, Equatable, Sendable {
    /// Opaque node identifier scoped to the Runtime process.
    public let nodeID: RuntimeOpaqueIdentifier

    /// Parent node identifier, or nil for a hierarchy root.
    @RuntimeRequiredNullable public private(set) var parentID: RuntimeOpaqueIdentifier?

    /// Position in the parent's ordered child collection.
    public let siblingIndex: Int

    /// Open platform-neutral semantic role.
    public let role: String

    /// Platform runtime type facts.
    public let runtimeType: RuntimeType

    /// Geometry in local, parent, and screen coordinate spaces.
    public let geometry: RuntimeNodeGeometry

    /// Explicit visibility causes and final result.
    public let visibility: RuntimeNodeVisibility

    /// Whether this node clips descendant content.
    public let clipsContent: Bool

    /// Resolved background color when available.
    @RuntimeRequiredNullable public private(set) var backgroundColor: RuntimeColor?

    /// Short text preview when available.
    @RuntimeRequiredNullable public private(set) var text: String?

    /// Accessibility facts when available.
    @RuntimeRequiredNullable public private(set) var accessibility: RuntimeAccessibility?

    /// Normalized interaction state.
    public let interaction: RuntimeInteraction

    /// Namespaced detail categories available for this node.
    public let availableDetailCategories: [RuntimeNamespacedIdentifier]

    /// Namespaced platform-specific node facts.
    public let extensions: RuntimeExtensionMap

    /// Ordered child nodes captured in the same snapshot.
    public let children: [RuntimeNode]

    public init(
        nodeID: RuntimeOpaqueIdentifier,
        parentID: RuntimeOpaqueIdentifier?,
        siblingIndex: Int,
        role: String,
        runtimeType: RuntimeType,
        geometry: RuntimeNodeGeometry,
        visibility: RuntimeNodeVisibility,
        clipsContent: Bool,
        backgroundColor: RuntimeColor?,
        text: String?,
        accessibility: RuntimeAccessibility?,
        interaction: RuntimeInteraction,
        availableDetailCategories: [RuntimeNamespacedIdentifier],
        extensions: RuntimeExtensionMap,
        children: [RuntimeNode]
    ) {
        self.nodeID = nodeID
        self.parentID = parentID
        self.siblingIndex = siblingIndex
        self.role = role
        self.runtimeType = runtimeType
        self.geometry = geometry
        self.visibility = visibility
        self.clipsContent = clipsContent
        self.backgroundColor = backgroundColor
        self.text = text
        self.accessibility = accessibility
        self.interaction = interaction
        self.availableDetailCategories = availableDetailCategories
        self.extensions = extensions
        self.children = children
    }
}

public struct RuntimeHierarchySnapshotPayload: Codable, Equatable, Sendable, RuntimeMethodBound {
    /// Opaque identifier for this immutable hierarchy capture.
    public let snapshotID: RuntimeOpaqueIdentifier

    /// Capture time in seconds since the Unix epoch.
    public let capturedAtUnixTime: Double

    /// Process-instance identifier represented by this snapshot.
    public let targetIdentifier: RuntimeOpaqueIdentifier

    /// Open interface-orientation identifier.
    public let orientation: String

    /// Display facts used by the capture.
    public let display: RuntimeDisplayInfo

    /// App viewport in screen logical coordinates.
    public let viewport: RuntimeCoordinateRect

    /// Ordered hierarchy roots.
    public let roots: [RuntimeNode]

    /// Optional namespaced snapshot facts.
    public let extensions: RuntimeExtensionMap?

    public init(
        snapshotID: RuntimeOpaqueIdentifier,
        capturedAtUnixTime: Double,
        targetIdentifier: RuntimeOpaqueIdentifier,
        orientation: String,
        display: RuntimeDisplayInfo,
        viewport: RuntimeCoordinateRect,
        roots: [RuntimeNode],
        extensions: RuntimeExtensionMap? = nil
    ) {
        self.snapshotID = snapshotID
        self.capturedAtUnixTime = capturedAtUnixTime
        self.targetIdentifier = targetIdentifier
        self.orientation = orientation
        self.display = display
        self.viewport = viewport
        self.roots = roots
        self.extensions = extensions
    }

    public static let runtimeMethod = RuntimeMethod.hierarchySnapshot
}

public enum RuntimeHierarchyValidationError: Error, Equatable, Sendable {
    case inconsistentVisibility
}
