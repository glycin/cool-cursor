package com.glycin.coolcursor

import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.CustomHighlighterRenderer
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.util.registry.Registry
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.LinearGradientPaint
import java.awt.RenderingHints
import java.awt.Stroke
import java.awt.geom.Path2D
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

internal const val HALO_EXTRA = 6f
internal const val SINE_AMPLITUDE_PX = 8f

internal class SmoothDashCaretRenderer(
    private val states: Map<Caret, SmoothDashState>,
) : CustomHighlighterRenderer {

    private val ribbon = Path2D.Double()
    private val caretDurationMs = Registry.intValue("editor.smooth.caret.duration", 120).coerceAtLeast(1)
    private val easeKk = Registry.doubleValue("editor.smooth.caret.curve.parametric.factor", 1.85).coerceIn(1.1, 1.85)
    private val easeA = 1.0 / (easeKk * 1.5 + 0.2)
    private val easeB = easeKk * 1.5 + 0.2

    override fun paint(editor: Editor, highlighter: RangeHighlighter, g: Graphics) {
        if (states.isEmpty()) return

        val now = System.nanoTime()
        val settings = coolCursorSettings()
        val headColor = settings.headColor
        val tailColor = settings.tailColor
        val thickness = settings.trailThickness
        val glow = settings.trailGlow
        val coreStroke: Stroke = BasicStroke(thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        val haloStroke: Stroke? = if (glow) {
            BasicStroke(thickness + HALO_EXTRA, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        } else {
            null
        }
        val haloColor: Color? = if (glow) {
            val glowColor = settings.glowColor
            Color(glowColor.red, glowColor.green, glowColor.blue, HALO_ALPHA)
        } else {
            null
        }
        val colorsMatch = headColor.rgb24 == tailColor.rgb24
        val offsets: DoubleArray = when (settings.lineCount) {
            2 -> AKIRA_OFFSETS
            3 -> TRIPLE_OFFSETS
            else -> SINGLE_OFFSETS
        }
        val offsetScale = editor.lineHeight.toDouble()

        val g2 = g.create() as Graphics2D
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

            for (state in states.values) {
                val elapsedMs = (now - state.startNanos) / 1_000_000.0
                if (elapsedMs >= DASH_DURATION_MS) continue

                val tHead = ((elapsedMs - HEAD_DELAY_MS) / caretDurationMs).coerceIn(0.0, 1.0)
                val tTail = ((elapsedMs - TAIL_DELAY_MS) / caretDurationMs).coerceIn(0.0, 1.0)
                if (tHead - tTail < 1e-4) continue

                val a = parametricEase(tTail)
                val b = parametricEase(tHead)

                val p0 = state.from
                val cp = state.control
                val p1 = state.to

                val dxC = p1.x - p0.x
                val dyC = p1.y - p0.y
                val chordLen = sqrt(dxC * dxC + dyC * dyC)
                val perpX = -dyC / chordLen
                val perpY = dxC / chordLen
                val waveScale = if (state.renderShape == TrailShape.SINE) {
                    chordLen * SINE_CYCLES_PER_PX * 2.0 * Math.PI
                } else {
                    0.0
                }

                for (off in offsets) {
                    val ox = perpX * off * offsetScale
                    val oy = perpY * off * offsetScale
                    val p0x = p0.x + ox; val p0y = p0.y + oy
                    val cpx = cp.x + ox; val cpy = cp.y + oy
                    val p1x = p1.x + ox; val p1y = p1.y + oy

                    val gradTailX: Double; val gradTailY: Double
                    val gradHeadX: Double; val gradHeadY: Double

                    ribbon.reset()
                    when (state.renderShape) {
                        TrailShape.STRAIGHT -> {
                            val tailX = (1 - a) * p0x + a * p1x
                            val tailY = (1 - a) * p0y + a * p1y
                            val headX = (1 - b) * p0x + b * p1x
                            val headY = (1 - b) * p0y + b * p1y
                            ribbon.moveTo(tailX, tailY)
                            ribbon.lineTo(headX, headY)
                            gradTailX = tailX; gradTailY = tailY
                            gradHeadX = headX; gradHeadY = headY
                        }
                        TrailShape.SINE -> {
                            val span = b - a
                            var firstX = 0.0; var firstY = 0.0
                            var lastX = 0.0; var lastY = 0.0
                            for (i in 0..SINE_SAMPLES) {
                                val t = a + (i.toDouble() / SINE_SAMPLES) * span
                                val cx = (1 - t) * p0x + t * p1x
                                val cy = (1 - t) * p0y + t * p1y
                                val wave = SINE_AMPLITUDE_PX * sin(waveScale * t)
                                val x = cx + perpX * wave
                                val y = cy + perpY * wave
                                if (i == 0) {
                                    ribbon.moveTo(x, y); firstX = x; firstY = y
                                } else {
                                    ribbon.lineTo(x, y)
                                }
                                lastX = x; lastY = y
                            }
                            gradTailX = firstX; gradTailY = firstY
                            gradHeadX = lastX; gradHeadY = lastY
                        }
                        else -> {
                            // CURVE (and any unresolved RANDOM, defensively)
                            val tailX = (1 - a) * (1 - a) * p0x + 2 * (1 - a) * a * cpx + a * a * p1x
                            val tailY = (1 - a) * (1 - a) * p0y + 2 * (1 - a) * a * cpy + a * a * p1y
                            val headX = (1 - b) * (1 - b) * p0x + 2 * (1 - b) * b * cpx + b * b * p1x
                            val headY = (1 - b) * (1 - b) * p0y + 2 * (1 - b) * b * cpy + b * b * p1y
                            val subCx = (1 - a) * (1 - b) * p0x + ((1 - a) * b + a * (1 - b)) * cpx + a * b * p1x
                            val subCy = (1 - a) * (1 - b) * p0y + ((1 - a) * b + a * (1 - b)) * cpy + a * b * p1y
                            ribbon.moveTo(tailX, tailY)
                            ribbon.quadTo(subCx, subCy, headX, headY)
                            gradTailX = tailX; gradTailY = tailY
                            gradHeadX = headX; gradHeadY = headY
                        }
                    }

                    if (glow) {
                        g2.paint = haloColor
                        g2.stroke = haloStroke
                        g2.draw(ribbon)
                    }

                    val dxLine = gradHeadX - gradTailX
                    val dyLine = gradHeadY - gradTailY
                    if (!colorsMatch && dxLine * dxLine + dyLine * dyLine >= MIN_GRADIENT_LEN_SQ) {
                        g2.paint = LinearGradientPaint(
                            gradTailX.toFloat(), gradTailY.toFloat(),
                            gradHeadX.toFloat(), gradHeadY.toFloat(),
                            GRADIENT_FRACTIONS,
                            arrayOf(tailColor, headColor),
                        )
                    } else {
                        g2.paint = headColor
                    }
                    g2.stroke = coreStroke
                    g2.draw(ribbon)
                }
            }
        } finally {
            g2.dispose()
        }
    }

    // f(t) = 1 - (1 - t^a)^b — mirrors IntelliJ's editor.smooth.caret.curve.parametric.factor.
    private fun parametricEase(t: Double): Double = 1.0 - (1.0 - t.pow(easeA)).pow(easeB)

    private companion object {
        const val HEAD_DELAY_MS = 20.0
        const val TAIL_DELAY_MS = 150.0
        const val HALO_ALPHA = 90
        const val MIN_GRADIENT_LEN_SQ = 1.0
        const val SINE_CYCLES_PER_PX = 1f / 30f
        const val SINE_SAMPLES = 24
        val GRADIENT_FRACTIONS = floatArrayOf(0f, 1f)
        val SINGLE_OFFSETS = doubleArrayOf(0.0)
        val AKIRA_OFFSETS = doubleArrayOf(-0.5, 0.5)
        val TRIPLE_OFFSETS = doubleArrayOf(-0.5, 0.0, 0.5)
    }
}
