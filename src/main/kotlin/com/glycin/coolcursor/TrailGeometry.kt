package com.glycin.coolcursor

import java.awt.geom.Path2D
import kotlin.math.sin

internal data class RibbonEndpoints(
    val tailX: Double,
    val tailY: Double,
    val headX: Double,
    val headY: Double,
)

internal object TrailGeometry {

    fun build(
        shape: TrailShape,
        ribbon: Path2D,
        a: Double,
        b: Double,
        p0x: Double, p0y: Double,
        cpx: Double, cpy: Double,
        p1x: Double, p1y: Double,
        perpX: Double, perpY: Double,
        waveScale: Double,
    ): RibbonEndpoints {
        ribbon.reset()
        return when (shape) {
            TrailShape.STRAIGHT -> straight(ribbon, a, b, p0x, p0y, p1x, p1y)
            TrailShape.SINE -> sine(ribbon, a, b, p0x, p0y, p1x, p1y, perpX, perpY, waveScale)
            else -> curve(ribbon, a, b, p0x, p0y, cpx, cpy, p1x, p1y)
        }
    }

    private fun straight(
        ribbon: Path2D,
        a: Double, b: Double,
        p0x: Double, p0y: Double,
        p1x: Double, p1y: Double,
    ): RibbonEndpoints {
        val tailX = (1 - a) * p0x + a * p1x
        val tailY = (1 - a) * p0y + a * p1y
        val headX = (1 - b) * p0x + b * p1x
        val headY = (1 - b) * p0y + b * p1y
        ribbon.moveTo(tailX, tailY)
        ribbon.lineTo(headX, headY)
        return RibbonEndpoints(tailX, tailY, headX, headY)
    }

    private fun curve(
        ribbon: Path2D,
        a: Double, b: Double,
        p0x: Double, p0y: Double,
        cpx: Double, cpy: Double,
        p1x: Double, p1y: Double,
    ): RibbonEndpoints {
        val tailX = (1 - a) * (1 - a) * p0x + 2 * (1 - a) * a * cpx + a * a * p1x
        val tailY = (1 - a) * (1 - a) * p0y + 2 * (1 - a) * a * cpy + a * a * p1y
        val headX = (1 - b) * (1 - b) * p0x + 2 * (1 - b) * b * cpx + b * b * p1x
        val headY = (1 - b) * (1 - b) * p0y + 2 * (1 - b) * b * cpy + b * b * p1y
        val subCx = (1 - a) * (1 - b) * p0x + ((1 - a) * b + a * (1 - b)) * cpx + a * b * p1x
        val subCy = (1 - a) * (1 - b) * p0y + ((1 - a) * b + a * (1 - b)) * cpy + a * b * p1y
        ribbon.moveTo(tailX, tailY)
        ribbon.quadTo(subCx, subCy, headX, headY)
        return RibbonEndpoints(tailX, tailY, headX, headY)
    }

    private fun sine(
        ribbon: Path2D,
        a: Double, b: Double,
        p0x: Double, p0y: Double,
        p1x: Double, p1y: Double,
        perpX: Double, perpY: Double,
        waveScale: Double,
    ): RibbonEndpoints {
        val span = b - a
        var firstX = 0.0; var firstY = 0.0
        var lastX = 0.0; var lastY = 0.0
        for (i in 0..TrailTuning.SINE_SAMPLES) {
            val t = a + (i.toDouble() / TrailTuning.SINE_SAMPLES) * span
            val cx = (1 - t) * p0x + t * p1x
            val cy = (1 - t) * p0y + t * p1y
            val wave = TrailTuning.SINE_AMPLITUDE_PX * sin(waveScale * t)
            val x = cx + perpX * wave
            val y = cy + perpY * wave
            if (i == 0) {
                ribbon.moveTo(x, y); firstX = x; firstY = y
            } else {
                ribbon.lineTo(x, y)
            }
            lastX = x; lastY = y
        }
        return RibbonEndpoints(firstX, firstY, lastX, lastY)
    }
}
