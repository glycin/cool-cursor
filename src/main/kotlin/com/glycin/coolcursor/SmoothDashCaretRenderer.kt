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
import kotlin.math.sqrt

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

        val snapshot = coolCursorSettings().snapshot()
        val now = System.nanoTime()
        val coreStroke: Stroke = BasicStroke(snapshot.trailThickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        val haloStroke: Stroke? = if (snapshot.trailGlow) {
            BasicStroke(snapshot.trailThickness + TrailTuning.HALO_EXTRA, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        } else {
            null
        }
        val haloColor: Color? = if (snapshot.trailGlow) {
            Color(snapshot.glowColor.red, snapshot.glowColor.green, snapshot.glowColor.blue, TrailTuning.HALO_ALPHA)
        } else {
            null
        }
        val colorsMatch = snapshot.headColor.rgb24 == snapshot.tailColor.rgb24
        val offsets: DoubleArray = when (snapshot.lineCount) {
            2 -> TrailTuning.AKIRA_OFFSETS
            3 -> TrailTuning.TRIPLE_OFFSETS
            else -> TrailTuning.SINGLE_OFFSETS
        }
        val offsetScale = editor.lineHeight.toDouble()

        val g2 = g.create() as Graphics2D
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

            for (state in states.values) {
                val elapsedMs = (now - state.startNanos) / 1_000_000.0
                if (elapsedMs >= TrailTuning.DASH_DURATION_MS) continue

                val tHead = ((elapsedMs - TrailTuning.HEAD_DELAY_MS) / caretDurationMs).coerceIn(0.0, 1.0)
                val tTail = ((elapsedMs - TrailTuning.TAIL_DELAY_MS) / caretDurationMs).coerceIn(0.0, 1.0)
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
                    chordLen * TrailTuning.SINE_CYCLES_PER_PX * 2.0 * Math.PI
                } else {
                    0.0
                }

                for (off in offsets) {
                    val ox = perpX * off * offsetScale
                    val oy = perpY * off * offsetScale
                    val endpoints = TrailGeometry.build(
                        shape = state.renderShape,
                        ribbon = ribbon,
                        a = a, b = b,
                        p0x = p0.x + ox, p0y = p0.y + oy,
                        cpx = cp.x + ox, cpy = cp.y + oy,
                        p1x = p1.x + ox, p1y = p1.y + oy,
                        perpX = perpX, perpY = perpY,
                        waveScale = waveScale,
                    )

                    if (snapshot.trailGlow) {
                        g2.paint = haloColor
                        g2.stroke = haloStroke
                        g2.draw(ribbon)
                    }

                    val dxLine = endpoints.headX - endpoints.tailX
                    val dyLine = endpoints.headY - endpoints.tailY
                    if (!colorsMatch && dxLine * dxLine + dyLine * dyLine >= TrailTuning.MIN_GRADIENT_LEN_SQ) {
                        g2.paint = LinearGradientPaint(
                            endpoints.tailX.toFloat(), endpoints.tailY.toFloat(),
                            endpoints.headX.toFloat(), endpoints.headY.toFloat(),
                            TrailTuning.GRADIENT_FRACTIONS,
                            arrayOf(snapshot.tailColor, snapshot.headColor),
                        )
                    } else {
                        g2.paint = snapshot.headColor
                    }
                    g2.stroke = coreStroke
                    g2.draw(ribbon)
                }
            }
        } finally {
            g2.dispose()
        }
    }

    private fun parametricEase(t: Double): Double = 1.0 - (1.0 - t.pow(easeA)).pow(easeB)
}
