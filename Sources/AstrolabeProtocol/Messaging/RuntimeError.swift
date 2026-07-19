//
//  RuntimeError.swift
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/16.
//

public struct RuntimeErrorCode: Codable, Equatable, Hashable, Sendable {
    /// Stable common or namespaced error identifier.
    public let rawValue: String

    public init(rawValue: String) throws {
        guard Self.commonCodes.contains(rawValue) || RuntimeNamespacedIdentifier.isValid(rawValue) else {
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

    public static let malformedFrame = common("malformedFrame")
    public static let frameTooLarge = common("frameTooLarge")
    public static let malformedMessage = common("malformedMessage")
    public static let unsupportedProtocolVersion = common("unsupportedProtocolVersion")
    public static let unsupportedMethod = common("unsupportedMethod")
    public static let handshakeRequired = common("handshakeRequired")
    public static let capabilityUnavailable = common("capabilityUnavailable")
    public static let invalidParameters = common("invalidParameters")
    public static let nodeNotFound = common("nodeNotFound")
    public static let unsupportedAttribute = common("unsupportedAttribute")
    public static let invalidAttributeValue = common("invalidAttributeValue")
    public static let patchNotFound = common("patchNotFound")
    public static let patchConflict = common("patchConflict")
    public static let patchRestorationFailed = common("patchRestorationFailed")
    public static let tooManyRequests = common("tooManyRequests")
    public static let requestCancelled = common("requestCancelled")
    public static let requestTimedOut = common("requestTimedOut")
    public static let internalFailure = common("internalFailure")

    private static let commonCodes: Set<String> = [
        "malformedFrame", "frameTooLarge", "malformedMessage",
        "unsupportedProtocolVersion", "unsupportedMethod", "handshakeRequired",
        "capabilityUnavailable", "invalidParameters", "nodeNotFound",
        "unsupportedAttribute", "invalidAttributeValue", "patchNotFound",
        "patchConflict", "patchRestorationFailed", "tooManyRequests",
        "requestCancelled", "requestTimedOut", "internalFailure"
    ]

    private static func common(_ rawValue: String) -> Self {
        do {
            return try Self(rawValue: rawValue)
        } catch {
            preconditionFailure("Invalid built-in error code: \(rawValue)")
        }
    }
}

public struct RuntimeError: Codable, Equatable, Error, Sendable {
    /// Stable machine-readable failure code.
    public let code: RuntimeErrorCode

    /// Human-readable diagnostic message.
    public let message: String

    /// Optional recovery instruction for the caller.
    @RuntimeRequiredNullable public private(set) var recoverySuggestion: String?

    /// Optional structured failure context.
    public let details: RuntimeJSONObject?

    /// Optional namespaced platform diagnostics.
    public let extensions: RuntimeExtensionMap?

    public init(
        code: RuntimeErrorCode,
        message: String,
        recoverySuggestion: String?,
        details: RuntimeJSONObject? = nil,
        extensions: RuntimeExtensionMap? = nil
    ) {
        self.code = code
        self.message = message
        self.recoverySuggestion = recoverySuggestion
        self.details = details
        self.extensions = extensions
    }
}
