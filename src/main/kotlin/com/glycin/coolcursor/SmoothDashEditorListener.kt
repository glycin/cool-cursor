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
import java.util.IdentityHashMap

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

    private companion object {
        val ATTACHMENT_KEY = Key.create<Disposable>("cool-cursor.smoothDashAttachment")
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
                val caret = event.caret ?: return
                val from = editor.visualPositionToPoint2D(editor.logicalToVisualPosition(event.oldPosition))
                val to = editor.visualPositionToPoint2D(caret.visualPosition)
                states[caret] = SmoothDashState(
                    from = from,
                    to = to,
                    startNanos = System.nanoTime(),
                )
                editor.contentComponent.repaint()
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
                delay(FRAME_INTERVAL_MS)
                editor.contentComponent.repaint()
            }
        }
    }

    private fun pruneFinished(): Boolean {
        val durationNanos = DASH_DURATION_MS.toLong() * 1_000_000L
        val now = System.nanoTime()
        states.entries.removeIf { now - it.value.startNanos > durationNanos }
        return states.isEmpty()
    }

    private companion object {
        const val FRAME_INTERVAL_MS = 16L
    }
}
