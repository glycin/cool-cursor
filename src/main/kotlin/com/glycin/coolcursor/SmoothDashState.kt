package com.glycin.coolcursor

import java.awt.geom.Point2D

internal data class SmoothDashState(
    val from: Point2D,
    val to: Point2D,
    val control: Point2D,
    val renderShape: TrailShape,
    val startNanos: Long,
)
