package dev.astrolabe.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class RuntimeSemanticInspectionTest {
    private val codec = RuntimeMessageCodec()

    @Test
    fun `semantic requests preserve explicit window and snapshot identity`() {
        val request = """{"requestID":"11111111-1111-4111-8111-111111111111","protocolVersion":{"major":2,"minor":0},"method":"hierarchySnapshot","parameters":{"semanticCapture":{"windowID":"window:1"}}}""".toByteArray()
        val envelope = codec.decodeRequest(request)
        val parameters = codec.decodeRequestParameters(envelope, RuntimeHierarchySnapshotParameters.contract)
        val encoded = codec.encodeRequest(envelope.requestID, RuntimeHierarchySnapshotParameters.contract, parameters)
        assertEquals(codec.decodeDocument(request), codec.decodeDocument(encoded))
        val detail = """{"nodeID":"semantic:1","snapshotID":"snapshot:1"}""".toByteArray()
        val value = codec.decodeValue(detail, RuntimeNodeDetailParameters.serializer())
        assertEquals(codec.decodeDocument(detail), codec.decodeDocument(codec.encodeValue(value, RuntimeNodeDetailParameters.serializer())))
    }

    @Test
    fun `container tree accepts exact object budget without applying semantic element budget`() {
        val nodes = (0 until 2000).map { index ->
            RuntimeSemanticNode(RuntimeOpaqueIdentifier("container:$index"), null, index, "container",
                RuntimeType("AXContainer", emptyList()), availableDetailCategories = emptyList(), children = emptyList())
        }
        val capture = RuntimeSemanticCapture(RuntimeOpaqueIdentifier("window:1"),
            RuntimeNamespacedIdentifier("ios.accessibility"), RuntimeSemanticAvailability.complete, emptyList(), 2000, false, nodes)
        assertEquals(2000, codec.decodeValue(codec.encodeValue(capture, RuntimeSemanticCapture.serializer()), RuntimeSemanticCapture.serializer()).roots.size)
        assertFailsWith<IllegalArgumentException> {
            capture.copy(visitedNodeCount = 1999)
        }
        assertFailsWith<IllegalArgumentException> {
            capture.copy(roots = listOf(nodes[0], nodes[0]))
        }
    }

    @Test
    fun `typed diagnostics preserve absence and reject malformed known extension`() {
        val windows = listOf(RuntimeInspectionWindow(RuntimeOpaqueIdentifier("window:1"), runtimeType = RuntimeType("UIWindow", emptyList())))
        val extension = runtimeInspectionTargetsExtension(windows)
        assertEquals(windows, extension.inspectionTargets())
        assertEquals(null, RuntimeExtensionMap(emptyMap()).inspectionTargets())
        assertFailsWith<RuntimeMessageException> {
            codec.decodeValue("""{"common.inspectionTargets":"opaque"}""".toByteArray(), RuntimeExtensionMap.serializer())
        }
        assertFailsWith<IllegalArgumentException> { runtimeInspectionTargetsExtension(windows + windows) }
    }

    @Test
    fun `UTF-8 path and text budgets and color ranges are enforced`() {
        assertFailsWith<IllegalArgumentException> {
            RuntimeInspectionIssue(RuntimeNamespacedIdentifier("inspection.limit"), "界".repeat(342))
        }
        val identifier = RuntimeAttributeIdentifier("common.text.content")
        assertFailsWith<IllegalArgumentException> {
            RuntimeSemanticTextField("text", RuntimeAttribute(identifier, RuntimeAttributeValue.StringValue("界".repeat(5462))))
        }
        assertFailsWith<IllegalArgumentException> {
            RuntimeSemanticTextField("color", RuntimeAttribute(identifier, RuntimeAttributeValue.Color(RuntimeColor("sRGB", 1.01, 0.0, 0.0, 1.0))))
        }
        assertFailsWith<IllegalArgumentException> {
            RuntimeSemanticTextField("extra", RuntimeAttribute(identifier, RuntimeAttributeValue.ObjectValue(JsonObject(mapOf("text" to JsonPrimitive("x".repeat(16385)))))))
        }
    }
}
