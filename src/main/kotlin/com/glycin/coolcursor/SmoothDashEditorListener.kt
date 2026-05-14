package com.glycin.coolcursor

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.awt.geom.Point2D
import java.util.IdentityHashMap
import kotlin.math.abs
import kotlin.math.sqrt

private val ATTACHMENT_KEY = Key.create<Disposable>("cool-cursor.smoothDashAttachment")

internal class SmoothDashEditorListener : EditorFactoryListener {

    override fun editorCreated(event: EditorFactoryEvent) {
        val editor = event.editor
        val parent = Disposer.newDisposable("SmoothDashCaret")
        editor.putUserData(ATTACHMENT_KEY, parent)
        Attachment(editor, parent)
    }

    override fun editorReleased(event: EditorFactoryEvent) {
        val parent = event.editor.getUserData(ATTACHMENT_KEY) ?: return
        event.editor.putUserData(ATTACHMENT_KEY, null)
        Disposer.dispose(parent)
    }
}

private class Attachment(private val editor: Editor, parent: Disposable) {

    private val states: MutableMap<Caret, SmoothDashState> = IdentityHashMap()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.EDT)
    private var renderJob: Job? = null
    private val highlighter: RangeHighlighter = installHighlighter()

    init {
        editor.caretModel.addCaretListener(object : CaretListener {
            override fun caretPositionChanged(event: CaretEvent) {
                val caret = event.caret
                val rawFrom = editor.visualPositionToPoint2D(editor.logicalToVisualPosition(event.oldPosition))
                val rawTo = editor.visualPositionToPoint2D(caret.visualPosition)
                val half = editor.lineHeight / 2.0
                val from = Point2D.Double(rawFrom.x, rawFrom.y + half)
                val to = Point2D.Double(rawTo.x, rawTo.y + half)
                if (abs(to.x - from.x) < 0.5 && abs(to.y - from.y) < 0.5) return

                val dx = to.x - from.x
                val dy = to.y - from.y
                val midX = (from.x + to.x) / 2.0
                val midY = (from.y + to.y) / 2.0
                val control = Point2D.Double(midX - dy * TrailTuning.BOW, midY + dx * TrailTuning.BOW)

                val userShape = coolCursorSettings().trailShape
                val chordLen = sqrt(dx * dx + dy * dy)
                val resolved = when (userShape) {
                    TrailShape.RANDOM -> {
                        val pick = TrailTuning.RANDOMIZABLE_SHAPES.random()
                        if (pick == TrailShape.SINE && chordLen < TrailTuning.SINE_THRESHOLD_PX) TrailShape.CURVE else pick
                    }
                    TrailShape.SINE -> if (chordLen < TrailTuning.SINE_THRESHOLD_PX) TrailShape.CURVE else TrailShape.SINE
                    else -> userShape
                }

                val state = SmoothDashState(from, to, control, resolved, System.nanoTime())
                states[caret] = state
                repaintState(state, coolCursorSettings().snapshot())
                ensureRenderLoop()
            }
        }, parent)

        Disposer.register(parent) {
            scope.cancel()
            if (highlighter.isValid) highlighter.dispose()
            states.clear()
        }
    }

    private fun installHighlighter(): RangeHighlighter =
        editor.markupModel.addRangeHighlighter(
            0,
            editor.document.textLength.coerceAtLeast(0),
            HighlighterLayer.LAST + 100,
            null,
            HighlighterTargetArea.LINES_IN_RANGE,
        ).also {
            it.isGreedyToRight = true
            it.customRenderer = SmoothDashCaretRenderer(states)
        }

    private fun ensureRenderLoop() {
        if (renderJob?.isActive == true) return
        renderJob = scope.launch {
            while (!pruneFinished()) {
                delay(TrailTuning.FRAME_INTERVAL)
                val snapshot = coolCursorSettings().snapshot()
                for (state in states.values) repaintState(state, snapshot)
            }
        }
    }

    private fun pruneFinished(): Boolean {
        val durationNanos = TrailTuning.DASH_DURATION_MS.toLong() * 1_000_000L
        val now = System.nanoTime()
        states.entries.removeIf { now - it.value.startNanos > durationNanos }
        return states.isEmpty()
    }

    private fun repaintState(state: SmoothDashState, snapshot: RenderSnapshot) {
        val haloHalf = if (snapshot.trailGlow) TrailTuning.HALO_EXTRA / 2f else 0f
        val laneHalf = if (snapshot.lineCount > 1) editor.lineHeight / 2f else 0f
        val waveAmp = if (state.renderShape == TrailShape.SINE) TrailTuning.SINE_AMPLITUDE_PX else 0f
        val pad = (snapshot.trailThickness / 2f + haloHalf + laneHalf + waveAmp + 2f).toInt()
        val minX = minOf(state.from.x, state.to.x, state.control.x).toInt() - pad
        val maxX = maxOf(state.from.x, state.to.x, state.control.x).toInt() + pad
        val minY = minOf(state.from.y, state.to.y, state.control.y).toInt() - pad
        val maxY = maxOf(state.from.y, state.to.y, state.control.y).toInt() + pad
        editor.contentComponent.repaint(minX, minY, maxX - minX, maxY - minY)
    }
}
