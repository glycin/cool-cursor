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
import java.awt.RenderingHints
import java.awt.Stroke
import java.awt.geom.Path2D
import kotlin.math.abs
import kotlin.math.pow

internal class SmoothDashCaretRenderer(
    private val states: Map<Caret, SmoothDashState>,
) : CustomHighlighterRenderer {

    private val ribbon = Path2D.Double()
    private val caretDurationMs = Registry.intValue("editor.smooth.caret.duration", 120).coerceAtLeast(1)
    private val curveK = Registry.doubleValue("editor.smooth.caret.curve.parametric.factor", 1.85)

    override fun paint(editor: Editor, highlighter: RangeHighlighter, g: Graphics) {
        if (states.isEmpty()) return

        val lineHeight = editor.lineHeight.toDouble()
        val now = System.nanoTime()
        val settings = coolCursorSettings()
        val trailColor = settings.trailColor
        val style = settings.trailStyle

        val g2 = g.create() as Graphics2D
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

            for (state in states.values) {
                val elapsedMs = (now - state.startNanos) / 1_000_000.0
                if (elapsedMs >= DASH_DURATION_MS) continue
                if (abs(state.to.y - state.from.y) > 0.5) continue

                val tHead = (elapsedMs / caretDurationMs).coerceIn(0.0, 1.0)
                val tTail = ((elapsedMs - TAIL_DELAY_MS) / caretDurationMs).coerceIn(0.0, 1.0)
                if (tHead - tTail < 1e-4) continue

                val headFrac = parametricEase(tHead, curveK)
                val tailFrac = parametricEase(tTail, curveK)

                val dx = state.to.x - state.from.x
                val headX = state.from.x + dx * headFrac
                val tailX = state.from.x + dx * tailFrac
                val y = state.from.y

                ribbon.reset()
                when (style) {
                    TrailStyle.SOLID -> {
                        ribbon.moveTo(tailX, y)
                        ribbon.lineTo(tailX, y + lineHeight)
                        ribbon.lineTo(headX, y + lineHeight)
                        ribbon.lineTo(headX, y)
                        ribbon.closePath()
                        g2.color = trailColor
                        g2.fill(ribbon)
                    }
                    TrailStyle.LINE -> {
                        ribbon.moveTo(tailX, y)
                        ribbon.lineTo(headX, y)
                        ribbon.moveTo(tailX, y + lineHeight)
                        ribbon.lineTo(headX, y + lineHeight)
                        g2.color = Color(trailColor.red, trailColor.green, trailColor.blue, HALO_ALPHA)
                        g2.stroke = HALO_STROKE
                        g2.draw(ribbon)
                        g2.color = trailColor
                        g2.stroke = CORE_STROKE
                        g2.draw(ribbon)
                    }
                }
            }
        } finally {
            g2.dispose()
        }
    }

    // f(t) = 1 - (1 - t^a)^b — mirrors IntelliJ's editor.smooth.caret.curve.parametric.factor.
    private fun parametricEase(t: Double, k: Double): Double {
        val kk = k.coerceIn(1.1, 1.85)
        val a = 1.0 / (kk * 1.5 + 0.2)
        val b = kk * 1.5 + 0.2
        return 1.0 - (1.0 - t.pow(a)).pow(b)
    }

    private companion object {
        const val TAIL_DELAY_MS = 90.0
        const val HALO_ALPHA = 90
        val CORE_STROKE: Stroke = BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        val HALO_STROKE: Stroke = BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
    }
}
