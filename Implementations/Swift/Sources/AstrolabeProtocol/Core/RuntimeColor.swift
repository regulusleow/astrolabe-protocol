//
//  RuntimeColor.swift
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/16.
//

public struct RuntimeColor: Codable, Equatable, Sendable {
    /// Color-space identifier used by the components.
    public let colorSpace: String

    /// Red component in the inclusive range from zero to one.
    public let red: Double

    /// Green component in the inclusive range from zero to one.
    public let green: Double

    /// Blue component in the inclusive range from zero to one.
    public let blue: Double

    /// Alpha component in the inclusive range from zero to one.
    public let alpha: Double

    public init(
        colorSpace: String,
        red: Double,
        green: Double,
        blue: Double,
        alpha: Double
    ) {
        self.colorSpace = colorSpace
        self.red = red
        self.green = green
        self.blue = blue
        self.alpha = alpha
    }
}
