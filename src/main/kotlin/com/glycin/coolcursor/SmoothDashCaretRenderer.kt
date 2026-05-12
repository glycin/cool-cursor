package com.glycin.coolcursor

import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.CustomHighlighterRenderer
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.util.registry.Registry
import java.awt.Color
import java.awt.GradientPaint
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Path2D
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sqrt

internal class SmoothDashCaretRenderer(
    private val states: Map<Caret, SmoothDashState>,
) : CustomHighlighterRenderer {

    private val triangle = Path2D.Double()

    override fun paint(editor: Editor, highlighter: RangeHighlighter, g: Graphics) {
        if (states.isEmpty()) return

        val lineHeight = editor.lineHeight
        val caretDurationMs = Registry.intValue("editor.smooth.caret.duration", 120).coerceAtLeast(1)
        val curveK = Registry.doubleValue("editor.smooth.caret.curve.parametric.factor", 1.85)
        val now = System.nanoTime()

        val g2 = g.create() as Graphics2D
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

            for (state in states.values) {
                val elapsedMs = (now - state.startNanos) / 1_000_000.0
                val tFade = (elapsedMs / DASH_DURATION_MS).coerceIn(0.0, 1.0)
                if (tFade >= 1.0) continue

                val dx = state.to.x - state.from.x
                val dy = state.to.y - state.from.y
                val len = sqrt(dx * dx + dy * dy)
                if (len < 1.0) continue

                // Slide the base along the same curve the platform uses for its smooth caret,
                // so the triangle stays glued to the visible (animating) caret rather than racing ahead to the logical destination.
                val tCaret = (elapsedMs / caretDurationMs).coerceIn(0.0, 1.0)
                val easedCaret = parametricEase(tCaret, curveK)
                val baseX = state.from.x + dx * easedCaret
                val baseY = state.from.y + dy * easedCaret

                val unitBackX = -dx / len
                val unitBackY = -dy / len
                val perpX = -unitBackY
                val perpY = unitBackX

                val baseMidX = baseX
                val baseMidY = baseY + lineHeight / 2.0

                val tipDist = len * state.tipLengthFactor
                val tipPerpDist = len * state.tipPerpFactor + lineHeight * 0.5 * state.tipPerpFactor.sign
                val tipX = baseMidX + unitBackX * tipDist + perpX * tipPerpDist
                val tipY = baseMidY + unitBackY * tipDist + perpY * tipPerpDist

                val alpha = (1.0 - tFade).toFloat()
                val baseColor = Color(BASE.red, BASE.green, BASE.blue, (alpha * 220).toInt().coerceIn(0, 255))
                val tipColor = Color(BASE.red, BASE.green, BASE.blue, 0)
                g2.paint = GradientPaint(
                    baseMidX.toFloat(), baseMidY.toFloat(), baseColor,
                    tipX.toFloat(), tipY.toFloat(), tipColor,
                )

                triangle.reset()
                triangle.moveTo(baseX, baseY)
                triangle.lineTo(baseX, baseY + lineHeight)
                triangle.lineTo(tipX, tipY)
                triangle.closePath()
                g2.fill(triangle)
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
        val BASE = Color(0x8B, 0x5C, 0xF6)
    }
}
