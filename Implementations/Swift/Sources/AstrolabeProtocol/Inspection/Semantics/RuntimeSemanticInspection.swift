import Foundation

/// Local completeness, independent of the success/failure response envelope.
public enum RuntimeSemanticAvailability: String, Codable, Sendable {
    case complete
    case partial
    case unavailable
}

public struct RuntimeSemanticCaptureOptions: Codable, Equatable, Sendable {
    /// Explicit native window; omitted only when a unique eligible window exists.
    public let windowID: RuntimeOpaqueIdentifier?

    public init(windowID: RuntimeOpaqueIdentifier? = nil) { self.windowID = windowID }
}

public struct RuntimeInspectionIssue: Codable, Equatable, Sendable {
    /// Stable namespaced reason without business values.
    public let code: RuntimeNamespacedIdentifier

    /// Optional local field path without business values.
    public let path: String?

    public init(
        code: RuntimeNamespacedIdentifier,
        path: String? = nil
    ) throws {
        try semanticPath(path)
        self.code = code
        self.path = path
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        try self.init(
            code: container.decode(RuntimeNamespacedIdentifier.self, forKey: .code),
            path: container.decodeIfPresent(String.self, forKey: .path)
        )
    }

    private enum CodingKeys: String, CodingKey {
        case code
        case path
    }
}

public struct RuntimeSemanticTextField: Codable, Equatable, Sendable {
    /// Field source path within its candidate.
    public let path: String

    /// Existing identifier and typed attribute value.
    public let attribute: RuntimeAttribute

    public init(
        path: String,
        attribute: RuntimeAttribute
    ) throws {
        try semanticPath(path)
        try semanticAttribute(attribute.value)
        self.path = path
        self.attribute = attribute
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        try self.init(
            path: container.decode(String.self, forKey: .path),
            attribute: container.decode(RuntimeAttribute.self, forKey: .attribute)
        )
    }

    private enum CodingKeys: String, CodingKey {
        case path
        case attribute
    }
}

public struct RuntimeSemanticTextCandidate: Codable, Equatable, Sendable {
    /// Namespaced original attachment or combined-cache source.
    public let source: RuntimeNamespacedIdentifier

    /// Original candidate path.
    public let path: String

    /// Typed fields sharing this candidate scope.
    public let fields: [RuntimeSemanticTextField]

    /// Local reasons for missing or uncertain facts.
    public let issues: [RuntimeInspectionIssue]

    public init(
        source: RuntimeNamespacedIdentifier,
        path: String,
        fields: [RuntimeSemanticTextField],
        issues: [RuntimeInspectionIssue]
    ) throws {
        try semanticPath(path)
        try semanticRequire(fields.count <= 1500 && issues.count <= 2000, "candidate budget")
        self.source = source
        self.path = path
        self.fields = fields
        self.issues = issues
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        try self.init(
            source: container.decode(RuntimeNamespacedIdentifier.self, forKey: .source),
            path: container.decode(String.self, forKey: .path),
            fields: container.decode([RuntimeSemanticTextField].self, forKey: .fields),
            issues: container.decode([RuntimeInspectionIssue].self, forKey: .issues)
        )
    }

    private enum CodingKeys: String, CodingKey {
        case source
        case path
        case fields
        case issues
    }
}

public struct RuntimeSemanticDetailContext: Codable, Equatable, Sendable {
    /// Runtime capture that owns these frozen values.
    public let snapshotID: RuntimeOpaqueIdentifier

    /// Original capture time, never the detail query time.
    public let capturedAtUnixTime: Double

    /// Namespaced producer of the frozen observation.
    public let source: RuntimeNamespacedIdentifier

    /// Completeness within the requested scope.
    public let availability: RuntimeSemanticAvailability

    /// Local reasons for missing facts.
    public let issues: [RuntimeInspectionIssue]

    /// Whether observed content was omitted due to a budget.
    public let wasTruncated: Bool

    /// Original and combined-cache candidates, kept separate.
    public let textCandidates: [RuntimeSemanticTextCandidate]

    public init(
        snapshotID: RuntimeOpaqueIdentifier,
        capturedAtUnixTime: Double,
        source: RuntimeNamespacedIdentifier,
        availability: RuntimeSemanticAvailability,
        issues: [RuntimeInspectionIssue],
        wasTruncated: Bool,
        textCandidates: [RuntimeSemanticTextCandidate]
    ) throws {
        try semanticRequire(capturedAtUnixTime.isFinite && capturedAtUnixTime >= 0, "capture time")
        try semanticRequire(textCandidates.count <= 10000 && issues.count <= 2000 && textCandidates.reduce(0, { $0 + $1.fields.count }) <= 10000, "detail budget")
        self.snapshotID = snapshotID
        self.capturedAtUnixTime = capturedAtUnixTime
        self.source = source
        self.availability = availability
        self.issues = issues
        self.wasTruncated = wasTruncated
        self.textCandidates = textCandidates
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        try self.init(
            snapshotID: container.decode(RuntimeOpaqueIdentifier.self, forKey: .snapshotID),
            capturedAtUnixTime: container.decode(Double.self, forKey: .capturedAtUnixTime),
            source: container.decode(RuntimeNamespacedIdentifier.self, forKey: .source),
            availability: container.decode(RuntimeSemanticAvailability.self, forKey: .availability),
            issues: container.decode([RuntimeInspectionIssue].self, forKey: .issues),
            wasTruncated: container.decode(Bool.self, forKey: .wasTruncated),
            textCandidates: container.decode([RuntimeSemanticTextCandidate].self, forKey: .textCandidates)
        )
    }

    private enum CodingKeys: String, CodingKey {
        case snapshotID
        case capturedAtUnixTime
        case source
        case availability
        case issues
        case wasTruncated
        case textCandidates
    }
}

public struct RuntimeSemanticNode: Codable, Equatable, Sendable {
    /// Opaque identity allocated for this semantic capture.
    public let nodeID: RuntimeOpaqueIdentifier

    /// Actual AX container identity; null for a root.
    @RuntimeRequiredNullable public private(set) var parentID: RuntimeOpaqueIdentifier?

    /// Zero-based position among the captured siblings.
    public let siblingIndex: Int

    /// Open semantic role; does not prove physical interaction.
    public let role: String

    /// Actual runtime object type, not a synthesized UIKit type.
    public let runtimeType: RuntimeType

    /// Public accessibility facts when known.
    public let accessibility: RuntimeAccessibility?

    /// Finite screen/logical accessibility region when known.
    public let frameInScreen: RuntimeCoordinateRect?

    /// Categories available from the frozen detail capture.
    public let availableDetailCategories: [RuntimeNamespacedIdentifier]

    /// Actual AX containment, not layout or drawing order.
    public let children: [RuntimeSemanticNode]

    public init(
        nodeID: RuntimeOpaqueIdentifier,
        parentID: RuntimeOpaqueIdentifier?,
        siblingIndex: Int,
        role: String,
        runtimeType: RuntimeType,
        accessibility: RuntimeAccessibility? = nil,
        frameInScreen: RuntimeCoordinateRect? = nil,
        availableDetailCategories: [RuntimeNamespacedIdentifier],
        children: [RuntimeSemanticNode]
    ) throws {
        try semanticRequire((0..<2000).contains(siblingIndex), "sibling index")
        try semanticString(role, maximum: 128)
        try semanticString(runtimeType.name, maximum: 512)
        for ancestor in runtimeType.ancestors { try semanticString(ancestor, maximum: 512) }
        if let frameInScreen { try semanticRect(frameInScreen, space: .screen) }
        if let accessibility {
            for value in [accessibility.identifier, accessibility.label, accessibility.value, accessibility.hint] { try semanticText(value) }
            try semanticRequire(accessibility.traits.count <= 2000 && Set(accessibility.traits).count == accessibility.traits.count, "trait budget/identity")
            for trait in accessibility.traits { try semanticString(trait, maximum: 128) }
        }
        try semanticRequire(children.count <= 2000 && availableDetailCategories.count <= 2000 && Set(availableDetailCategories).count == availableDetailCategories.count, "node budget/categories")
        self.nodeID = nodeID
        self.parentID = parentID
        self.siblingIndex = siblingIndex
        self.role = role
        self.runtimeType = runtimeType
        self.accessibility = accessibility
        self.frameInScreen = frameInScreen
        self.availableDetailCategories = availableDetailCategories
        self.children = children
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        try self.init(
            nodeID: container.decode(RuntimeOpaqueIdentifier.self, forKey: .nodeID),
            parentID: container.decode(RuntimeOpaqueIdentifier?.self, forKey: .parentID),
            siblingIndex: container.decode(Int.self, forKey: .siblingIndex),
            role: container.decode(String.self, forKey: .role),
            runtimeType: container.decode(RuntimeType.self, forKey: .runtimeType),
            accessibility: container.decodeIfPresent(RuntimeAccessibility.self, forKey: .accessibility),
            frameInScreen: container.decodeIfPresent(RuntimeCoordinateRect.self, forKey: .frameInScreen),
            availableDetailCategories: container.decode([RuntimeNamespacedIdentifier].self, forKey: .availableDetailCategories),
            children: container.decode([RuntimeSemanticNode].self, forKey: .children)
        )
    }

    private enum CodingKeys: String, CodingKey {
        case nodeID
        case parentID
        case siblingIndex
        case role
        case runtimeType
        case accessibility
        case frameInScreen
        case availableDetailCategories
        case children
    }
}

public struct RuntimeSemanticCapture: Codable, Equatable, Sendable {
    /// Native window registry identity for the selected window.
    public let windowID: RuntimeOpaqueIdentifier

    /// Namespaced source of this semantic observation.
    public let source: RuntimeNamespacedIdentifier

    /// Completeness within the requested scope.
    public let availability: RuntimeSemanticAvailability

    /// Typed reasons for omitted or unavailable facts.
    public let issues: [RuntimeInspectionIssue]

    /// Number of AX objects visited, including containers.
    public let visitedNodeCount: Int

    /// True only when content remained unvisited or omitted.
    public let wasTruncated: Bool

    /// Ordered semantic roots with snapshot-local identities.
    public let roots: [RuntimeSemanticNode]

    public init(
        windowID: RuntimeOpaqueIdentifier,
        source: RuntimeNamespacedIdentifier,
        availability: RuntimeSemanticAvailability,
        issues: [RuntimeInspectionIssue],
        visitedNodeCount: Int,
        wasTruncated: Bool,
        roots: [RuntimeSemanticNode]
    ) throws {
        try semanticRequire((0...2000).contains(visitedNodeCount) && issues.count <= 2000, "capture budget")
        var seen = Set<RuntimeOpaqueIdentifier>()
        func visit(_ nodes: [RuntimeSemanticNode], parent: RuntimeOpaqueIdentifier?, depth: Int) throws {
            try semanticRequire(nodes.isEmpty || depth < 20, "semantic depth")
            for (index, node) in nodes.enumerated() {
                try semanticRequire(node.parentID == parent && node.siblingIndex == index, "semantic tree edge")
                try semanticRequire(seen.insert(node.nodeID).inserted && seen.count <= visitedNodeCount, "semantic node identity/count")
                try visit(node.children, parent: node.nodeID, depth: depth + 1)
            }
        }
        try visit(roots, parent: nil, depth: 0)
        self.windowID = windowID
        self.source = source
        self.availability = availability
        self.issues = issues
        self.visitedNodeCount = visitedNodeCount
        self.wasTruncated = wasTruncated
        self.roots = roots
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        try self.init(
            windowID: container.decode(RuntimeOpaqueIdentifier.self, forKey: .windowID),
            source: container.decode(RuntimeNamespacedIdentifier.self, forKey: .source),
            availability: container.decode(RuntimeSemanticAvailability.self, forKey: .availability),
            issues: container.decode([RuntimeInspectionIssue].self, forKey: .issues),
            visitedNodeCount: container.decode(Int.self, forKey: .visitedNodeCount),
            wasTruncated: container.decode(Bool.self, forKey: .wasTruncated),
            roots: container.decode([RuntimeSemanticNode].self, forKey: .roots)
        )
    }

    private enum CodingKeys: String, CodingKey {
        case windowID
        case source
        case availability
        case issues
        case visitedNodeCount
        case wasTruncated
        case roots
    }
}

public struct RuntimeInspectionWindow: Codable, Equatable, Sendable {
    /// Native window registry identity scoped to this Runtime.
    public let windowID: RuntimeOpaqueIdentifier

    /// Scene identity when exposed by the platform.
    public let sceneID: RuntimeOpaqueIdentifier?

    /// Actual native window type.
    public let runtimeType: RuntimeType

    /// Finite local/logical bounds when known.
    public let bounds: RuntimeCoordinateRect?

    /// Finite native window level when known.
    public let level: Double?

    /// Namespaced reason the window cannot be selected.
    public let exclusionReason: RuntimeNamespacedIdentifier?

    public init(
        windowID: RuntimeOpaqueIdentifier,
        sceneID: RuntimeOpaqueIdentifier? = nil,
        runtimeType: RuntimeType,
        bounds: RuntimeCoordinateRect? = nil,
        level: Double? = nil,
        exclusionReason: RuntimeNamespacedIdentifier? = nil
    ) throws {
        try semanticString(runtimeType.name, maximum: 512)
        for ancestor in runtimeType.ancestors { try semanticString(ancestor, maximum: 512) }
        if let bounds { try semanticRect(bounds, space: .local) }
        if let level { try semanticRequire(level.isFinite, "window level") }
        self.windowID = windowID
        self.sceneID = sceneID
        self.runtimeType = runtimeType
        self.bounds = bounds
        self.level = level
        self.exclusionReason = exclusionReason
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        try self.init(
            windowID: container.decode(RuntimeOpaqueIdentifier.self, forKey: .windowID),
            sceneID: container.decodeIfPresent(RuntimeOpaqueIdentifier.self, forKey: .sceneID),
            runtimeType: container.decode(RuntimeType.self, forKey: .runtimeType),
            bounds: container.decodeIfPresent(RuntimeCoordinateRect.self, forKey: .bounds),
            level: container.decodeIfPresent(Double.self, forKey: .level),
            exclusionReason: container.decodeIfPresent(RuntimeNamespacedIdentifier.self, forKey: .exclusionReason)
        )
    }

    private enum CodingKeys: String, CodingKey {
        case windowID
        case sceneID
        case runtimeType
        case bounds
        case level
        case exclusionReason
    }
}

public enum RuntimeSemanticValidationError: Error, Equatable, Sendable {
    case invalidValue(String)
}

private func semanticRequire(_ condition: Bool, _ field: String) throws {
    guard condition else { throw RuntimeSemanticValidationError.invalidValue(field) }
}

private func semanticString(_ value: String, maximum: Int) throws {
    try semanticRequire(!value.isEmpty && value.unicodeScalars.count <= maximum, "string length")
}

private func semanticPath(_ value: String?) throws {
    guard let value else { return }
    try semanticRequire(!value.isEmpty && value.utf8.count <= 1024 && !value.unicodeScalars.contains(where: { $0.value < 32 || $0.value == 127 }), "field path")
}

private func semanticText(_ value: String?) throws {
    if let value { try semanticRequire(value.utf8.count <= 16384, "text byte budget") }
}

private func semanticRect(_ value: RuntimeCoordinateRect, space: RuntimeCoordinateSpace) throws {
    try semanticRequire([value.x, value.y, value.width, value.height].allSatisfy(\.isFinite) && value.width >= 0 && value.height >= 0 && value.coordinateSpace == space && value.unit == .logical, "semantic coordinates")
}

private func semanticAttribute(_ value: RuntimeAttributeValue) throws {
    // Scan the complete existing typed value, including nested extension maps.
    try semanticJSONStrings(JSONDecoder().decode(RuntimeJSONValue.self, from: JSONEncoder().encode(value)))
    switch value {
    case let .string(text): try semanticText(text)
    case let .stringList(texts):
        try semanticRequire(texts.count <= 1500, "text list budget")
        for text in texts { try semanticText(text) }
    case let .measurement(measurement): try semanticRequire(measurement.value.isFinite, "measurement")
    case let .point(point):
        try semanticCoordinateValues([point.x, point.y], unit: point.unit)
    case let .size(size):
        try semanticCoordinateValues([size.width, size.height], unit: size.unit)
        try semanticRequire(size.width >= 0 && size.height >= 0, "size")
    case let .vector(vector):
        try semanticCoordinateValues([vector.dx, vector.dy], unit: vector.unit)
    case let .rect(rect):
        try semanticCoordinateValues([rect.x, rect.y, rect.width, rect.height], unit: rect.unit)
        try semanticRequire(rect.width >= 0 && rect.height >= 0, "rect")
    case let .insets(insets):
        try semanticCoordinateValues([insets.top, insets.left, insets.bottom, insets.right], unit: insets.unit)
    case let .color(color):
        try semanticString(color.colorSpace, maximum: 64)
        try semanticRequire([color.red, color.green, color.blue, color.alpha].allSatisfy { $0.isFinite && (0...1).contains($0) }, "color")
    case let .textRuns(runs):
        for run in runs {
            try semanticText(run.text)
            try semanticText(run.fontName)
            try semanticText(run.fontFamilyName)
            if let size = run.fontSize { try semanticAttribute(.measurement(size)) }
            if let color = run.color { try semanticAttribute(.color(color)) }
        }
    case let .array(values): try semanticJSONStrings(.array(values))
    case let .object(values): try semanticJSONStrings(.object(values))
    case let .extensionValue(_, value): try semanticJSONStrings(value)
    default: break
    }
}

private func semanticCoordinateValues(_ values: [Double], unit: RuntimeMeasurementUnit) throws {
    try semanticRequire(values.allSatisfy(\.isFinite) && unit != .scaledLogical, "coordinate values/unit")
}

private func semanticJSONStrings(_ value: RuntimeJSONValue) throws {
    switch value {
    case let .string(text): try semanticText(text)
    case let .array(values): for value in values { try semanticJSONStrings(value) }
    case let .object(values): for value in values.values { try semanticJSONStrings(value) }
    default: break
    }
}

public extension RuntimeExtensionMap {
    /// Typed diagnostic windows; absence remains distinct from an empty list.
    func inspectionTargets() throws -> [RuntimeInspectionWindow]? {
        guard let value = values["common.inspectionTargets"] else { return nil }
        let windows = try JSONDecoder().decode([RuntimeInspectionWindow].self, from: JSONEncoder().encode(value))
        try validateInspectionTargets(windows)
        return windows
    }

    /// Constructs the defined diagnostic extension while preserving other extensions.
    init(inspectionTargets: [RuntimeInspectionWindow], values: [String: RuntimeJSONValue] = [:]) throws {
        try validateInspectionTargets(inspectionTargets)
        var values = values
        values["common.inspectionTargets"] = try JSONDecoder().decode(RuntimeJSONValue.self, from: JSONEncoder().encode(inspectionTargets))
        try self.init(values: values)
    }
}

public extension RuntimeError {
    /// Typed target-selection diagnostics from the namespaced error extension.
    func inspectionTargets() throws -> [RuntimeInspectionWindow]? { try extensions?.inspectionTargets() }
}

private func validateInspectionTargets(_ windows: [RuntimeInspectionWindow]) throws {
    try semanticRequire(windows.count <= 64 && Set(windows.map(\.windowID)).count == windows.count, "inspection targets")
}

func validateInspectionTargetsValue(_ value: RuntimeJSONValue?) throws {
    guard let value else { return }
    let windows = try JSONDecoder().decode([RuntimeInspectionWindow].self, from: JSONEncoder().encode(value))
    try validateInspectionTargets(windows)
}
