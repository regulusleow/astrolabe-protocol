//
//  RuntimeGeometry.kt
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/20.
//

package dev.astrolabe.protocol

import kotlinx.serialization.Serializable

/** Unit used by runtime measurements. */
@Serializable
public enum class RuntimeMeasurementUnit {
    logical,
    scaledLogical,
    pixel
}

/** Coordinate space containing a runtime geometry value. */
@Serializable
public enum class RuntimeCoordinateSpace {
    local,
    parent,
    screen,
    viewport
}

/** Scalar measurement with an explicit unit. */
@Serializable
public data class RuntimeMeasurement(
    /** Numeric magnitude. */
    public val value: Double,
    /** Unit used by the magnitude. */
    public val unit: RuntimeMeasurementUnit
)

/** Point in an explicitly declared coordinate space. */
@Serializable
public data class RuntimeCoordinatePoint(
    /** Horizontal coordinate. */
    public val x: Double,
    /** Vertical coordinate. */
    public val y: Double,
    /** Coordinate space containing the point. */
    public val coordinateSpace: RuntimeCoordinateSpace,
    /** Unit used by both coordinates. */
    public val unit: RuntimeMeasurementUnit
) {
    init {
        require(unit != RuntimeMeasurementUnit.scaledLogical) {
            "Coordinate point unit must be logical or pixel"
        }
    }
}

/** Two-dimensional measured extent. */
@Serializable
public data class RuntimeMeasuredSize(
    /** Non-negative horizontal extent. */
    public val width: Double,
    /** Non-negative vertical extent. */
    public val height: Double,
    /** Unit used by both extents. */
    public val unit: RuntimeMeasurementUnit
) {
    init {
        require(width >= 0.0 && height >= 0.0) { "Measured size dimensions cannot be negative" }
        require(unit != RuntimeMeasurementUnit.scaledLogical) {
            "Measured size unit must be logical or pixel"
        }
    }
}

/** Two-dimensional signed delta. */
@Serializable
public data class RuntimeVector(
    /** Horizontal delta. */
    public val dx: Double,
    /** Vertical delta. */
    public val dy: Double,
    /** Unit used by both deltas. */
    public val unit: RuntimeMeasurementUnit
) {
    init {
        require(unit != RuntimeMeasurementUnit.scaledLogical) {
            "Vector unit must be logical or pixel"
        }
    }
}

/** Insets measured from four edges. */
@Serializable
public data class RuntimeInsets(
    /** Top inset. */
    public val top: Double,
    /** Left inset. */
    public val left: Double,
    /** Bottom inset. */
    public val bottom: Double,
    /** Right inset. */
    public val right: Double,
    /** Unit used by every edge. */
    public val unit: RuntimeMeasurementUnit
) {
    init {
        require(unit != RuntimeMeasurementUnit.scaledLogical) {
            "Insets unit must be logical or pixel"
        }
    }
}

/** Rectangle in an explicitly declared coordinate space. */
@Serializable
public data class RuntimeCoordinateRect(
    /** Horizontal origin. */
    public val x: Double,
    /** Vertical origin. */
    public val y: Double,
    /** Non-negative horizontal extent. */
    public val width: Double,
    /** Non-negative vertical extent. */
    public val height: Double,
    /** Coordinate space containing the rectangle. */
    public val coordinateSpace: RuntimeCoordinateSpace,
    /** Unit used by all rectangle components. */
    public val unit: RuntimeMeasurementUnit
) {
    init {
        require(width >= 0.0 && height >= 0.0) { "Rectangle dimensions cannot be negative" }
        require(unit != RuntimeMeasurementUnit.scaledLogical) {
            "Rectangle unit must be logical or pixel"
        }
    }
}

/** Per-axis logical-to-pixel conversion. */
@Serializable
public data class RuntimeScale(
    /** Horizontal scale. */
    public val x: Double,
    /** Vertical scale. */
    public val y: Double
) {
    init {
        require(x > 0.0 && y > 0.0) { "Scale components must be greater than zero" }
    }
}

/** Display facts required for coordinate conversion. */
@Serializable
public data class RuntimeDisplayInfo(
    /** Display dimensions in logical layout units. */
    public val logicalSize: RuntimeMeasuredSize,
    /** Display dimensions in physical pixels. */
    public val pixelSize: RuntimeMeasuredSize,
    /** Per-axis conversion from logical units to pixels. */
    public val logicalToPixelScale: RuntimeScale,
    /** Maximum refresh rate when reported by the platform. */
    public val maximumRefreshRate: Double?
) {
    init {
        require(logicalSize.unit == RuntimeMeasurementUnit.logical) {
            "Logical display size must use logical units"
        }
        require(pixelSize.unit == RuntimeMeasurementUnit.pixel) {
            "Pixel display size must use pixel units"
        }
        require(maximumRefreshRate == null || maximumRefreshRate > 0.0) {
            "Maximum refresh rate must be greater than zero"
        }
    }
}
