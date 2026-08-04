//
//  RuntimeHandshake.kt
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/20.
//

package dev.astrolabe.protocol

import kotlinx.serialization.Serializable

/** Host product information sent when opening a protocol session. */
@Serializable
public data class RuntimeClientDescriptor(
    /** Client product name. */
    public val name: String,
    /** Client release version. */
    public val version: String
)

/** Parameters for the handshake request. */
@Serializable
public data class RuntimeHandshakeParameters(
    /** Host initiating the protocol session. */
    public val client: RuntimeClientDescriptor,
    /** Protocol versions the Host can decode. */
    public val supportedProtocolRange: RuntimeProtocolRange
) {
    public companion object {
        /** Typed request contract for the handshake method. */
        public val contract: RuntimeMethodContract<RuntimeHandshakeParameters> by lazy {
            RuntimeMethodContract(RuntimeMethod.handshake, serializer())
        }
    }
}

/** Runtime implementation information returned by handshake. */
@Serializable
public data class RuntimeDescriptor(
    /** Namespaced runtime implementation identifier. */
    public val identifier: RuntimeNamespacedIdentifier,
    /** Runtime SDK release version. */
    public val version: String,
    /** Opaque identifier for this runtime process instance. */
    public val instanceID: RuntimeOpaqueIdentifier
)

/** Successful handshake response payload. */
@Serializable
public data class RuntimeHandshakePayload(
    /** Runtime implementation participating in the session. */
    public val runtime: RuntimeDescriptor,
    /** Platform identifier reported by the runtime. */
    public val platform: String,
    /** Protocol version selected for this session. */
    public val negotiatedProtocolVersion: RuntimeProtocolVersion,
    /** Runtime operations available to the Host. */
    public val capabilities: List<RuntimeCapability>,
    /** Optional namespaced session facts. */
    public val extensions: RuntimeExtensionMap? = null
) {
    init {
        require(negotiatedProtocolVersion == RuntimeProtocolVersion.V2) {
            "Handshake selected an unsupported protocol version"
        }
    }

    public companion object {
        /** Typed success-payload contract for the handshake method. */
        public val contract: RuntimeMethodContract<RuntimeHandshakePayload> by lazy {
            RuntimeMethodContract(RuntimeMethod.handshake, serializer())
        }
    }
}
