//
//  RuntimePatchableAttribute.swift
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/16.
//

import Foundation

public struct RuntimePatchValueType: Codable, Equatable, Hashable, Sendable {
    /// Common or namespaced attribute-value type identifier.
    public let rawValue: String

    public init(rawValue: String) throws {
        guard Self.commonTypes.contains(rawValue) || RuntimeNamespacedIdentifier.isValid(rawValue) else {
            throw RuntimeContractValueError.invalidOpenIdentifier(rawValue)
        }
        self.rawValue = rawValue
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        try self.init(rawValue: container.decode(String.self))
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        try container.encode(rawValue)
    }

    private static let commonTypes: Set<String> = [
        "null", "boolean", "integer", "number", "string", "stringList",
        "measurement", "point", "size", "vector", "rect", "insets", "color",
        "textRuns", "layoutRelations", "array", "object"
    ]
}

public struct RuntimePatchValueConstraints: Codable, Equatable, Sendable {
    /// Inclusive or exclusive numeric lower bound when present.
    @RuntimeRequiredNullable public private(set) var minimum: Double?

    /// Inclusive or exclusive numeric upper bound when present.
    @RuntimeRequiredNullable public private(set) var maximum: Double?

    /// Whether the minimum bound is exclusive.
    public let minimumExclusive: Bool

    /// Whether the maximum bound is exclusive.
    public let maximumExclusive: Bool

    /// Accepted producer-defined string formats.
    public let acceptedFormats: [String]

    /// Explicit values accepted by the Runtime.
    public let allowedValues: [RuntimeAttributeValue]

    public init(
        minimum: Double?,
        maximum: Double?,
        minimumExclusive: Bool,
        maximumExclusive: Bool,
        acceptedFormats: [String],
        allowedValues: [RuntimeAttributeValue]
    ) {
        self.minimum = minimum
        self.maximum = maximum
        self.minimumExclusive = minimumExclusive
        self.maximumExclusive = maximumExclusive
        self.acceptedFormats = acceptedFormats
        self.allowedValues = allowedValues
    }
}

public struct RuntimePatchableAttribute: Codable, Equatable, Sendable {
    /// Namespaced attribute path or placeholder pattern.
    public let attributePattern: String

    /// Value type accepted by the attribute.
    public let valueType: RuntimePatchValueType

    /// Semantic node roles eligible for the patch.
    public let targetRoles: [String]

    /// Optional value-level constraints.
    @RuntimeRequiredNullable public private(set) var valueConstraints: RuntimePatchValueConstraints?

    /// Namespaced platform-specific applicability facts.
    public let extensions: RuntimeExtensionMap

    public init(
        attributePattern: String,
        valueType: RuntimePatchValueType,
        targetRoles: [String],
        valueConstraints: RuntimePatchValueConstraints?,
        extensions: RuntimeExtensionMap
    ) throws {
        guard Self.isValidPattern(attributePattern) else {
            throw RuntimePatchValidationError.invalidAttributePattern(attributePattern)
        }
        self.attributePattern = attributePattern
        self.valueType = valueType
        self.targetRoles = targetRoles
        self.valueConstraints = valueConstraints
        self.extensions = extensions
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        try self.init(
            attributePattern: container.decode(String.self, forKey: .attributePattern),
            valueType: container.decode(RuntimePatchValueType.self, forKey: .valueType),
            targetRoles: container.decode([String].self, forKey: .targetRoles),
            valueConstraints: container.decodeIfPresent(
                RuntimePatchValueConstraints.self,
                forKey: .valueConstraints
            ),
            extensions: container.decode(RuntimeExtensionMap.self, forKey: .extensions)
        )
    }

    private static func isValidPattern(_ value: String) -> Bool {
        guard value.count <= 256 else { return false }
        return value.range(
            of: #"^[a-z][a-z0-9-]*(\.(?:[A-Za-z][A-Za-z0-9_-]*|<[A-Za-z][A-Za-z0-9_-]*>))+$"#,
            options: .regularExpression
        ) != nil
    }

    private enum CodingKeys: String, CodingKey {
        case attributePattern
        case valueType
        case targetRoles
        case valueConstraints
        case extensions
    }
}

public struct RuntimePatchableAttributesPayload: Codable, Equatable, Sendable, RuntimeMethodBound {
    /// Runtime-owned catalog of patchable attributes.
    public let attributes: [RuntimePatchableAttribute]

    public init(attributes: [RuntimePatchableAttribute]) {
        self.attributes = attributes
    }

    public static let runtimeMethod = RuntimeMethod.patchableAttributes
}

public enum RuntimePatchValidationError: Error, Equatable, Sendable {
    case invalidAttributePattern(String)
}
