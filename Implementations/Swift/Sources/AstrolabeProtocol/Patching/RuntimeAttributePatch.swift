//
//  RuntimeAttributePatch.swift
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/16.
//

public struct RuntimeApplyAttributePatchParameters: Codable, Equatable, Sendable, RuntimeMethodBound {
    /// Node receiving the temporary patch.
    public let nodeID: RuntimeOpaqueIdentifier

    /// Namespaced attribute to patch.
    public let attributeIdentifier: RuntimeAttributeIdentifier

    /// Requested typed value.
    public let value: RuntimeAttributeValue

    public init(
        nodeID: RuntimeOpaqueIdentifier,
        attributeIdentifier: RuntimeAttributeIdentifier,
        value: RuntimeAttributeValue
    ) {
        self.nodeID = nodeID
        self.attributeIdentifier = attributeIdentifier
        self.value = value
    }

    public static let runtimeMethod = RuntimeMethod.applyAttributePatch
}

public struct RuntimeAttributePatch: Codable, Equatable, Sendable, RuntimeMethodBound {
    /// Opaque identifier for this active patch.
    public let patchID: RuntimeOpaqueIdentifier

    /// Patched node identifier.
    public let nodeID: RuntimeOpaqueIdentifier

    /// Patched namespaced attribute.
    public let attributeIdentifier: RuntimeAttributeIdentifier

    /// Value captured before the first patch when available.
    @RuntimeRequiredNullable public private(set) var originalValue: RuntimeAttributeValue?

    /// Value requested by the Host.
    public let requestedValue: RuntimeAttributeValue

    /// Value observed after applying the patch when available.
    @RuntimeRequiredNullable public private(set) var actualValue: RuntimeAttributeValue?

    /// Apply time in seconds since the Unix epoch.
    public let appliedAtUnixTime: Double

    public init(
        patchID: RuntimeOpaqueIdentifier,
        nodeID: RuntimeOpaqueIdentifier,
        attributeIdentifier: RuntimeAttributeIdentifier,
        originalValue: RuntimeAttributeValue?,
        requestedValue: RuntimeAttributeValue,
        actualValue: RuntimeAttributeValue?,
        appliedAtUnixTime: Double
    ) {
        self.patchID = patchID
        self.nodeID = nodeID
        self.attributeIdentifier = attributeIdentifier
        self.originalValue = originalValue
        self.requestedValue = requestedValue
        self.actualValue = actualValue
        self.appliedAtUnixTime = appliedAtUnixTime
    }

    public static let runtimeMethod = RuntimeMethod.applyAttributePatch
}

public struct RuntimeAttributePatchListPayload: Codable, Equatable, Sendable, RuntimeMethodBound {
    /// Active temporary patches in Runtime-defined order.
    public let patches: [RuntimeAttributePatch]

    public init(patches: [RuntimeAttributePatch]) {
        self.patches = patches
    }

    public static let runtimeMethod = RuntimeMethod.listAttributePatches
}

public struct RuntimeRevertAttributePatchParameters: Codable, Equatable, Sendable, RuntimeMethodBound {
    /// Active patch to revert.
    public let patchID: RuntimeOpaqueIdentifier

    public init(patchID: RuntimeOpaqueIdentifier) {
        self.patchID = patchID
    }

    public static let runtimeMethod = RuntimeMethod.revertAttributePatch
}

public struct RuntimeRevertAttributePatchPayload: Codable, Equatable, Sendable, RuntimeMethodBound {
    /// Identifier of the reverted patch.
    public let revertedPatchID: RuntimeOpaqueIdentifier

    /// Value restored by the Runtime when available.
    @RuntimeRequiredNullable public private(set) var restoredValue: RuntimeAttributeValue?

    /// Number of patches remaining after the operation.
    public let remainingPatchCount: Int

    public init(
        revertedPatchID: RuntimeOpaqueIdentifier,
        restoredValue: RuntimeAttributeValue?,
        remainingPatchCount: Int
    ) {
        self.revertedPatchID = revertedPatchID
        self.restoredValue = restoredValue
        self.remainingPatchCount = remainingPatchCount
    }

    public static let runtimeMethod = RuntimeMethod.revertAttributePatch
}

public struct RuntimeClearAttributePatchesPayload: Codable, Equatable, Sendable, RuntimeMethodBound {
    /// Patch identifiers successfully reverted by the clear operation.
    public let revertedPatchIDs: [RuntimeOpaqueIdentifier]

    /// Number of patches remaining after all revert attempts.
    public let remainingPatchCount: Int

    public init(revertedPatchIDs: [RuntimeOpaqueIdentifier], remainingPatchCount: Int) {
        self.revertedPatchIDs = revertedPatchIDs
        self.remainingPatchCount = remainingPatchCount
    }

    public static let runtimeMethod = RuntimeMethod.clearAttributePatches
}
