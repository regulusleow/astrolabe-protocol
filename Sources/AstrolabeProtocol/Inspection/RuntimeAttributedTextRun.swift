//
//  RuntimeAttributedTextRun.swift
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/16.
//

public struct RuntimeTextRange: Codable, Equatable, Sendable {
    /// Zero-based text location.
    public let location: Int64

    /// Number of text units covered by the run.
    public let length: Int64

    public init(location: Int64, length: Int64) {
        self.location = location
        self.length = length
    }
}

public struct RuntimeTextRun: Codable, Equatable, Sendable {
    /// Range covered by this run.
    public let range: RuntimeTextRange

    /// Text covered by this run.
    public let text: String

    /// Platform font name when available.
    @RuntimeRequiredNullable public private(set) var fontName: String?

    /// Platform font-family name when available.
    @RuntimeRequiredNullable public private(set) var fontFamilyName: String?

    /// Font size and scaling unit when available.
    @RuntimeRequiredNullable public private(set) var fontSize: RuntimeMeasurement?

    /// Foreground color when available.
    @RuntimeRequiredNullable public private(set) var color: RuntimeColor?

    /// Namespaced platform-specific text-run facts.
    public let extensions: RuntimeExtensionMap

    public init(
        range: RuntimeTextRange,
        text: String,
        fontName: String?,
        fontFamilyName: String?,
        fontSize: RuntimeMeasurement?,
        color: RuntimeColor?,
        extensions: RuntimeExtensionMap
    ) {
        self.range = range
        self.text = text
        self.fontName = fontName
        self.fontFamilyName = fontFamilyName
        self.fontSize = fontSize
        self.color = color
        self.extensions = extensions
    }
}
