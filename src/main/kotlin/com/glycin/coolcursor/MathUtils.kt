package com.glycin.coolcursor

import kotlin.math.sqrt

internal object MathUtils {
    fun chordLength(dx: Double, dy: Double): Double = sqrt(dx * dx + dy * dy)

    fun lerp(a: Double, b: Double, t: Double): Double = (1 - t) * a + t * b

    fun quadBezier(p0: Double, cp: Double, p1: Double, t: Double): Double {
        val omt = 1 - t
        return omt * omt * p0 + 2 * omt * t * cp + t * t * p1
    }
}
