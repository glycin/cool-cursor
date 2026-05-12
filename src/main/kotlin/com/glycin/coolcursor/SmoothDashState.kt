package com.glycin.coolcursor

import java.awt.geom.Point2D

internal const val DASH_DURATION_MS = 350

internal data class SmoothDashState(
    val from: Point2D,
    val to: Point2D,
    val startNanos: Long,
    val tipLengthFactor: Double,
    val tipPerpFactor: Double,
)
