package dev.astrolabe.protocol

import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class RuntimeNodeRelationTest {
    private val codec = RuntimeMessageCodec()

    @Test
    fun `namespaced node relation round trips`() {
        val relation = RuntimeNodeRelation(
            type = RuntimeNamespacedIdentifier("vendor.graph.related"),
            sourceNodeID = RuntimeOpaqueIdentifier("node:view:1"),
            targetNodeID = RuntimeOpaqueIdentifier("node:layer:1"),
            extensions = RuntimeExtensionMap(
                mapOf("vendor.graph.confidence" to JsonPrimitive(1))
            )
        )

        val encoded = codec.encodeValue(relation, RuntimeNodeRelation.serializer())

        assertEquals(
            relation,
            codec.decodeValue(encoded, RuntimeNodeRelation.serializer())
        )
        assertEquals("uiGraphRelations", RuntimeCapability.uiGraphRelations.rawValue)
    }
}
