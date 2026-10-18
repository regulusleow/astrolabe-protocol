package dev.astrolabe.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

/** Local completeness independent of the response envelope. */
@Serializable
public enum class RuntimeSemanticAvailability { complete, partial, unavailable }

/** Explicit semantic capture opt-in. */
@Serializable
public data class RuntimeSemanticCaptureOptions(
    /** Explicit native window, omitted only for a unique eligible window. */
    public val windowID: RuntimeOpaqueIdentifier? = null
)

/** Typed semantic inspection fact. */
@Serializable
public data class RuntimeInspectionIssue(
    /** Stable namespaced reason without business values. */
    public val code: RuntimeNamespacedIdentifier,
    /** Optional local field path without business values. */
    public val path: String? = null
) {
    init {
        semanticPath(path)
    }
}

/** Typed semantic inspection fact. */
@Serializable
public data class RuntimeSemanticTextField(
    /** Field source path within its candidate. */
    public val path: String,
    /** Existing identifier and typed attribute value. */
    public val attribute: RuntimeAttribute
) {
    init {
        semanticPath(path)
        semanticAttribute(attribute.value)
    }
}

/** Typed semantic inspection fact. */
@Serializable
public data class RuntimeSemanticTextCandidate(
    /** Namespaced original attachment or combined-cache source. */
    public val source: RuntimeNamespacedIdentifier,
    /** Original candidate path. */
    public val path: String,
    /** Typed fields sharing this candidate scope. */
    public val fields: List<RuntimeSemanticTextField>,
    /** Local reasons for missing or uncertain facts. */
    public val issues: List<RuntimeInspectionIssue>
) {
    init {
        semanticPath(path)
        require(fields.size <= 1500 && issues.size <= 2000) { "Candidate budget exceeded" }
    }
}

/** Typed semantic inspection fact. */
@Serializable
public data class RuntimeSemanticDetailContext(
    /** Runtime capture that owns these frozen values. */
    public val snapshotID: RuntimeOpaqueIdentifier,
    /** Original capture time, never the detail query time. */
    public val capturedAtUnixTime: Double,
    /** Namespaced producer of the frozen observation. */
    public val source: RuntimeNamespacedIdentifier,
    /** Completeness within the requested scope. */
    public val availability: RuntimeSemanticAvailability,
    /** Local reasons for missing facts. */
    public val issues: List<RuntimeInspectionIssue>,
    /** Whether observed content was omitted due to a budget. */
    public val wasTruncated: Boolean,
    /** Original and combined-cache candidates, kept separate. */
    public val textCandidates: List<RuntimeSemanticTextCandidate>
) {
    init {
        require(capturedAtUnixTime.isFinite() && capturedAtUnixTime >= 0) { "Capture time must be finite and non-negative" }
        require(textCandidates.size <= 10000 && issues.size <= 2000 && textCandidates.sumOf { it.fields.size } <= 10000) { "Detail budget exceeded" }
    }
}

/** Typed semantic inspection fact. */
@Serializable
public data class RuntimeSemanticNode(
    /** Opaque identity allocated for this semantic capture. */
    public val nodeID: RuntimeOpaqueIdentifier,
    /** Actual AX container identity; null for a root. */
    public val parentID: RuntimeOpaqueIdentifier?,
    /** Zero-based position among the captured siblings. */
    public val siblingIndex: Int,
    /** Open semantic role; does not prove physical interaction. */
    public val role: String,
    /** Actual runtime object type, not a synthesized UIKit type. */
    public val runtimeType: RuntimeType,
    /** Public accessibility facts when known. */
    public val accessibility: RuntimeAccessibility? = null,
    /** Finite screen/logical accessibility region when known. */
    public val frameInScreen: RuntimeCoordinateRect? = null,
    /** Categories available from the frozen detail capture. */
    public val availableDetailCategories: List<RuntimeNamespacedIdentifier>,
    /** Actual AX containment, not layout or drawing order. */
    public val children: List<RuntimeSemanticNode>
) {
    init {
        require(siblingIndex in 0 until 2000) { "Invalid sibling index" }
        semanticString(role, 128)
        semanticString(runtimeType.name, 512)
        runtimeType.ancestors.forEach { semanticString(it, 512) }
        frameInScreen?.let { semanticRect(it, RuntimeCoordinateSpace.screen) }
        accessibility?.let {
            listOf(it.identifier, it.label, it.value, it.hint).forEach(::semanticText)
            require(it.traits.size <= 2000 && it.traits.toSet().size == it.traits.size) { "Invalid trait budget/identity" }
            it.traits.forEach { trait -> semanticString(trait, 128) }
        }
        require(children.size <= 2000 && availableDetailCategories.size <= 2000 && availableDetailCategories.toSet().size == availableDetailCategories.size) { "Invalid node budget/categories" }
    }
}

/** Typed semantic inspection fact. */
@Serializable
public data class RuntimeSemanticCapture(
    /** Native window registry identity for the selected window. */
    public val windowID: RuntimeOpaqueIdentifier,
    /** Namespaced source of this semantic observation. */
    public val source: RuntimeNamespacedIdentifier,
    /** Completeness within the requested scope. */
    public val availability: RuntimeSemanticAvailability,
    /** Typed reasons for omitted or unavailable facts. */
    public val issues: List<RuntimeInspectionIssue>,
    /** Number of AX objects visited, including containers. */
    public val visitedNodeCount: Int,
    /** True only when content remained unvisited or omitted. */
    public val wasTruncated: Boolean,
    /** Ordered semantic roots with snapshot-local identities. */
    public val roots: List<RuntimeSemanticNode>
) {
    init {
        require(visitedNodeCount in 0..2000 && issues.size <= 2000) { "Capture budget exceeded" }
        val seen = mutableSetOf<RuntimeOpaqueIdentifier>()
        fun visit(nodes: List<RuntimeSemanticNode>, parent: RuntimeOpaqueIdentifier?, depth: Int) {
            require(nodes.isEmpty() || depth < 20) { "Semantic depth exceeded" }
            nodes.forEachIndexed { index, node ->
                require(node.parentID == parent && node.siblingIndex == index) { "Invalid semantic tree edge" }
                require(seen.add(node.nodeID) && seen.size <= visitedNodeCount) { "Invalid semantic node identity/count" }
                visit(node.children, node.nodeID, depth + 1)
            }
        }
        visit(roots, null, 0)
    }
}

/** Typed semantic inspection fact. */
@Serializable
public data class RuntimeInspectionWindow(
    /** Native window registry identity scoped to this Runtime. */
    public val windowID: RuntimeOpaqueIdentifier,
    /** Scene identity when exposed by the platform. */
    public val sceneID: RuntimeOpaqueIdentifier? = null,
    /** Actual native window type. */
    public val runtimeType: RuntimeType,
    /** Finite local/logical bounds when known. */
    public val bounds: RuntimeCoordinateRect? = null,
    /** Finite native window level when known. */
    public val level: Double? = null,
    /** Namespaced reason the window cannot be selected. */
    public val exclusionReason: RuntimeNamespacedIdentifier? = null
) {
    init {
        semanticString(runtimeType.name, 512)
        runtimeType.ancestors.forEach { semanticString(it, 512) }
        bounds?.let { semanticRect(it, RuntimeCoordinateSpace.local) }
        require(level == null || level.isFinite()) { "Window level must be finite" }
    }
}

private fun semanticString(value: String, maximum: Int) {
    require(value.isNotEmpty() && value.codePointCount(0, value.length) <= maximum) { "Invalid semantic string length" }
}

private fun semanticPath(value: String?) {
    if (value == null) return
    require(value.isNotEmpty() && value.toByteArray(Charsets.UTF_8).size <= 1024 && value.none { it.code < 32 || it.code == 127 }) { "Invalid semantic field path" }
}

private fun semanticText(value: String?) {
    require(value == null || value.toByteArray(Charsets.UTF_8).size <= 16384) { "Semantic text byte budget exceeded" }
}

private fun semanticRect(value: RuntimeCoordinateRect, space: RuntimeCoordinateSpace) {
    require(listOf(value.x, value.y, value.width, value.height).all { it.isFinite() } && value.width >= 0 && value.height >= 0 && value.coordinateSpace == space && value.unit == RuntimeMeasurementUnit.logical) { "Invalid semantic coordinates" }
}

private fun semanticAttribute(value: RuntimeAttributeValue) {
    when (value) {
        is RuntimeAttributeValue.StringValue -> semanticText(value.value)
        is RuntimeAttributeValue.StringList -> {
            require(value.value.size <= 1500) { "Text list budget exceeded" }
            value.value.forEach(::semanticText)
        }
        is RuntimeAttributeValue.Measurement -> require(value.value.value.isFinite()) { "Non-finite measurement" }
        is RuntimeAttributeValue.Point -> semanticCoordinateValues(listOf(value.value.x, value.value.y), value.value.unit)
        is RuntimeAttributeValue.Size -> semanticCoordinateValues(listOf(value.value.width, value.value.height), value.value.unit)
        is RuntimeAttributeValue.Vector -> semanticCoordinateValues(listOf(value.value.dx, value.value.dy), value.value.unit)
        is RuntimeAttributeValue.Rect -> semanticCoordinateValues(listOf(value.value.x, value.value.y, value.value.width, value.value.height), value.value.unit)
        is RuntimeAttributeValue.Insets -> semanticCoordinateValues(listOf(value.value.top, value.value.left, value.value.bottom, value.value.right), value.value.unit)
        is RuntimeAttributeValue.Color -> {
            semanticString(value.value.colorSpace, 64)
            require(listOf(value.value.red, value.value.green, value.value.blue, value.value.alpha).all { it.isFinite() && it in 0.0..1.0 }) { "Invalid color" }
        }
        is RuntimeAttributeValue.TextRuns -> value.value.forEach {
            semanticText(it.text)
            semanticText(it.fontName)
            semanticText(it.fontFamilyName)
            it.fontSize?.let { size -> semanticAttribute(RuntimeAttributeValue.Measurement(size)) }
            it.color?.let { color -> semanticAttribute(RuntimeAttributeValue.Color(color)) }
        }
        is RuntimeAttributeValue.ArrayValue -> semanticJSONStrings(value.value)
        is RuntimeAttributeValue.ObjectValue -> semanticJSONStrings(value.value)
        is RuntimeAttributeValue.Extension -> semanticJSONStrings(value.value)
        else -> Unit
    }
}

private fun semanticCoordinateValues(values: List<Double>, unit: RuntimeMeasurementUnit) {
    require(values.all { it.isFinite() } && unit != RuntimeMeasurementUnit.scaledLogical) { "Invalid coordinate values/unit" }
}

private fun semanticJSONStrings(value: kotlinx.serialization.json.JsonElement) {
    when (value) {
        is kotlinx.serialization.json.JsonPrimitive -> if (value.isString) semanticText(value.content)
        is kotlinx.serialization.json.JsonArray -> value.forEach(::semanticJSONStrings)
        is kotlinx.serialization.json.JsonObject -> value.values.forEach(::semanticJSONStrings)
    }
}

private val semanticJson: Json = Json { ignoreUnknownKeys = true; encodeDefaults = false; explicitNulls = true }

/** Decodes typed window diagnostics, preserving absent versus empty. */
public fun RuntimeExtensionMap.inspectionTargets(): List<RuntimeInspectionWindow>? {
    val value = values["common.inspectionTargets"] ?: return null
    return semanticJson.decodeFromJsonElement(ListSerializer(RuntimeInspectionWindow.serializer()), value).also(::validateInspectionTargets)
}

/** Decodes target-selection diagnostics from this error. */
public fun RuntimeError.inspectionTargets(): List<RuntimeInspectionWindow>? = extensions?.inspectionTargets()

/** Builds the defined extension while preserving other namespaced extensions. */
public fun runtimeInspectionTargetsExtension(
    inspectionTargets: List<RuntimeInspectionWindow>,
    values: Map<String, kotlinx.serialization.json.JsonElement> = emptyMap()
): RuntimeExtensionMap {
    validateInspectionTargets(inspectionTargets)
    return RuntimeExtensionMap(values + ("common.inspectionTargets" to semanticJson.encodeToJsonElement(ListSerializer(RuntimeInspectionWindow.serializer()), inspectionTargets)))
}

private fun validateInspectionTargets(windows: List<RuntimeInspectionWindow>) {
    require(windows.size <= 64 && windows.map { it.windowID }.toSet().size == windows.size) { "Invalid inspection targets" }
}

internal fun validateInspectionTargetsValue(value: kotlinx.serialization.json.JsonElement?) {
    if (value != null) validateInspectionTargets(semanticJson.decodeFromJsonElement(ListSerializer(RuntimeInspectionWindow.serializer()), value))
}
