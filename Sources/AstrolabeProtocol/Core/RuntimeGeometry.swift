//
//  RuntimeGeometry.swift
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/16.
//

public enum RuntimeMeasurementUnit: String, Codable, Sendable {
    case logical
    case scaledLogical
    case pixel
}

public enum RuntimeCoordinateSpace: String, Codable, Sendable {
    case local
    case parent
    case screen
    case viewport
}

public struct RuntimeMeasurement: Codable, Equatable, Sendable {
    /// Numeric magnitude in the declared unit.
    public let value: Double

    /// Unit used by the magnitude.
    public let unit: RuntimeMeasurementUnit

    public init(value: Double, unit: RuntimeMeasurementUnit) {
        self.value = value
        self.unit = unit
    }
}

public struct RuntimeCoordinatePoint: Codable, Equatable, Sendable {
    /// Horizontal coordinate.
    public let x: Double

    /// Vertical coordinate.
    public let y: Double

    /// Coordinate space containing the point.
    public let coordinateSpace: RuntimeCoordinateSpace

    /// Logical or physical unit used by the coordinates.
    public let unit: RuntimeMeasurementUnit

    public init(
        x: Double,
        y: Double,
        coordinateSpace: RuntimeCoordinateSpace,
        unit: RuntimeMeasurementUnit
    ) {
        self.x = x
        self.y = y
        self.coordinateSpace = coordinateSpace
        self.unit = unit
    }
}

public struct RuntimeMeasuredSize: Codable, Equatable, Sendable {
    /// Non-negative horizontal extent.
    public let width: Double

    /// Non-negative vertical extent.
    public let height: Double

    /// Logical or physical unit used by the extents.
    public let unit: RuntimeMeasurementUnit

    public init(width: Double, height: Double, unit: RuntimeMeasurementUnit) {
        self.width = width
        self.height = height
        self.unit = unit
    }
}

public struct RuntimeVector: Codable, Equatable, Sendable {
    /// Horizontal delta.
    public let dx: Double

    /// Vertical delta.
    public let dy: Double

    /// Logical or physical unit used by the deltas.
    public let unit: RuntimeMeasurementUnit

    public init(dx: Double, dy: Double, unit: RuntimeMeasurementUnit) {
        self.dx = dx
        self.dy = dy
        self.unit = unit
    }
}

public struct RuntimeInsets: Codable, Equatable, Sendable {
    /// Top inset.
    public let top: Double

    /// Left inset.
    public let left: Double

    /// Bottom inset.
    public let bottom: Double

    /// Right inset.
    public let right: Double

    /// Logical or physical unit used by every edge.
    public let unit: RuntimeMeasurementUnit

    public init(
        top: Double,
        left: Double,
        bottom: Double,
        right: Double,
        unit: RuntimeMeasurementUnit
    ) {
        self.top = top
        self.left = left
        self.bottom = bottom
        self.right = right
        self.unit = unit
    }
}

public struct RuntimeCoordinateRect: Codable, Equatable, Sendable {
    /// Horizontal origin.
    public let x: Double

    /// Vertical origin.
    public let y: Double

    /// Non-negative horizontal extent.
    public let width: Double

    /// Non-negative vertical extent.
    public let height: Double

    /// Coordinate space containing the rectangle.
    public let coordinateSpace: RuntimeCoordinateSpace

    /// Logical or physical unit used by the rectangle.
    public let unit: RuntimeMeasurementUnit

    public init(
        x: Double,
        y: Double,
        width: Double,
        height: Double,
        coordinateSpace: RuntimeCoordinateSpace,
        unit: RuntimeMeasurementUnit
    ) {
        self.x = x
        self.y = y
        self.width = width
        self.height = height
        self.coordinateSpace = coordinateSpace
        self.unit = unit
    }
}

public struct RuntimeScale: Codable, Equatable, Sendable {
    /// Horizontal logical-to-pixel scale.
    public let x: Double

    /// Vertical logical-to-pixel scale.
    public let y: Double

    public init(x: Double, y: Double) {
        self.x = x
        self.y = y
    }
}

public struct RuntimeDisplayInfo: Codable, Equatable, Sendable {
    /// Display dimensions in logical layout units.
    public let logicalSize: RuntimeMeasuredSize

    /// Display dimensions in physical pixels.
    public let pixelSize: RuntimeMeasuredSize

    /// Per-axis conversion from logical units to pixels.
    public let logicalToPixelScale: RuntimeScale

    /// Maximum refresh rate when reported by the platform.
    @RuntimeRequiredNullable public private(set) var maximumRefreshRate: Double?

    public init(
        logicalSize: RuntimeMeasuredSize,
        pixelSize: RuntimeMeasuredSize,
        logicalToPixelScale: RuntimeScale,
        maximumRefreshRate: Double?
    ) {
        self.logicalSize = logicalSize
        self.pixelSize = pixelSize
        self.logicalToPixelScale = logicalToPixelScale
        self.maximumRefreshRate = maximumRefreshRate
    }
}
