//
//  RuntimeCapability.kt
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/20.
//

package dev.astrolabe.protocol

import kotlinx.serialization.Serializable

/** Open capability identifier advertised during handshake. */
@JvmInline
@Serializable
public value class RuntimeCapability(
    /** Raw capability value. */
    public val rawValue: String
) {
    init {
        require(rawValue.isNotEmpty() && rawValue.length <= MAXIMUM_CAPABILITY_LENGTH) {
            "Runtime capability must contain between 1 and 128 characters"
        }
    }

    public companion object {
        /** Runtime can describe the inspected application. */
        public val applicationInfo: RuntimeCapability = RuntimeCapability("applicationInfo")
        /** Runtime can capture hierarchy snapshots. */
        public val hierarchySnapshot: RuntimeCapability = RuntimeCapability("hierarchySnapshot")
        /** Runtime can return node details. */
        public val nodeDetail: RuntimeCapability = RuntimeCapability("nodeDetail")
        /** Runtime can describe patchable attributes. */
        public val attributePatchDiscovery: RuntimeCapability = RuntimeCapability("attributePatchDiscovery")
        /** Runtime can apply temporary attribute patches. */
        public val attributePatching: RuntimeCapability = RuntimeCapability("attributePatching")
        /** Runtime supports request cancellation. */
        public val requestCancellation: RuntimeCapability = RuntimeCapability("requestCancellation")
        /** Runtime can report directed cross-tree node relations. */
        public val uiGraphRelations: RuntimeCapability = RuntimeCapability("uiGraphRelations")
    }
}

private const val MAXIMUM_CAPABILITY_LENGTH: Int = 128
