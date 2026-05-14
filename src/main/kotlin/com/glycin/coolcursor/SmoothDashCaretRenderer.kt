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

internal const val HALO_EXTRA = 6f

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

        val g2 = g.create() as Graphics2D
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

            for (state in states.values) {
                val elapsedMs = (now - state.startNanos) / 1_000_000.0
                if (elapsedMs >= DASH_DURATION_MS) continue

                val tHead = (elapsedMs / caretDurationMs).coerceIn(0.0, 1.0)
                val tTail = ((elapsedMs - TAIL_DELAY_MS) / caretDurationMs).coerceIn(0.0, 1.0)
                if (tHead - tTail < 1e-4) continue

                val a = parametricEase(tTail)
                val b = parametricEase(tHead)

                val p0 = state.from
                val cp = state.control
                val p1 = state.to

                val tailX = (1 - a) * (1 - a) * p0.x + 2 * (1 - a) * a * cp.x + a * a * p1.x
                val tailY = (1 - a) * (1 - a) * p0.y + 2 * (1 - a) * a * cp.y + a * a * p1.y
                val headX = (1 - b) * (1 - b) * p0.x + 2 * (1 - b) * b * cp.x + b * b * p1.x
                val headY = (1 - b) * (1 - b) * p0.y + 2 * (1 - b) * b * cp.y + b * b * p1.y
                val subCx = (1 - a) * (1 - b) * p0.x + ((1 - a) * b + a * (1 - b)) * cp.x + a * b * p1.x
                val subCy = (1 - a) * (1 - b) * p0.y + ((1 - a) * b + a * (1 - b)) * cp.y + a * b * p1.y

                ribbon.reset()
                ribbon.moveTo(tailX, tailY)
                ribbon.quadTo(subCx, subCy, headX, headY)

                if (glow) {
                    g2.paint = haloColor
                    g2.stroke = haloStroke
                    g2.draw(ribbon)
                }

                val dxLine = headX - tailX
                val dyLine = headY - tailY
                if (!colorsMatch && dxLine * dxLine + dyLine * dyLine >= MIN_GRADIENT_LEN_SQ) {
                    g2.paint = LinearGradientPaint(
                        tailX.toFloat(), tailY.toFloat(),
                        headX.toFloat(), headY.toFloat(),
                        GRADIENT_FRACTIONS,
                        arrayOf(tailColor, headColor),
                    )
                } else {
                    g2.paint = headColor
                }
                g2.stroke = coreStroke
                g2.draw(ribbon)
            }
        } finally {
            g2.dispose()
        }
    }

    // f(t) = 1 - (1 - t^a)^b — mirrors IntelliJ's editor.smooth.caret.curve.parametric.factor.
    private fun parametricEase(t: Double): Double = 1.0 - (1.0 - t.pow(easeA)).pow(easeB)

    private companion object {
        const val TAIL_DELAY_MS = 90.0
        const val HALO_ALPHA = 90
        const val MIN_GRADIENT_LEN_SQ = 1.0
        val GRADIENT_FRACTIONS = floatArrayOf(0f, 1f)
    }
}
