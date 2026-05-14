package com.glycin.coolcursor

import java.awt.geom.Point2D

internal const val DASH_DURATION_MS = 240

internal data class SmoothDashState(
    val from: Point2D,
    val to: Point2D,
    val control: Point2D,
    val renderShape: TrailShape,
    val startNanos: Long,
)
