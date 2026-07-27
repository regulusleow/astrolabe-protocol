//
//  RuntimeHierarchyModelTest.kt
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/20.
//

package dev.astrolabe.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RuntimeHierarchyModelTest {
    private val codec = RuntimeMessageCodec()

    @Test
    fun `hierarchy fixtures decode through typed contracts`() {
        val request = codec.decodeRequest(fixture("v2/valid/hierarchy-snapshot-request.json"))
        val response = codec.decodeResponse(fixture("v2/valid/hierarchy-snapshot-response.json"))

        codec.decodeRequestParameters(request, RuntimeHierarchySnapshotParameters.contract)
        val payload = codec.decodeSuccessPayload(response, RuntimeHierarchySnapshotPayload.contract)

        assertEquals("node:window:0", payload.roots.single().nodeID.rawValue)
        assertEquals("node:label:1", payload.roots.single().children.single().nodeID.rawValue)
    }

    @Test
    fun `hierarchy values reject missing units and inconsistent visibility`() {
        assertFailsWith<RuntimeMessageException.InvalidDocument> {
            codec.decodeValue(
                fixture("v2/invalid/coordinate-rect-missing-unit.json"),
                RuntimeCoordinateRect.serializer()
            )
        }
        assertFailsWith<RuntimeMessageException.InvalidDocument> {
            codec.decodeValue(
                fixture("v2/invalid/inconsistent-visibility.json"),
                RuntimeNodeVisibility.serializer()
            )
        }
    }

    private fun fixture(path: String): ByteArray = checkNotNull(
        javaClass.classLoader.getResource(path)
    ).readBytes()
}
