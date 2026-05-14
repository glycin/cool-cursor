package com.glycin.coolcursor

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import java.awt.Color

internal const val DEFAULT_TRAIL_RGB = 0x8B5CF6
internal const val DEFAULT_THICKNESS = 4f
internal const val DEFAULT_GLOW = true
internal const val DEFAULT_LINE_COUNT = 1

internal enum class TrailShape(private val label: String) {
    STRAIGHT("Straight"),
    CURVE("Curve"),
    SINE("Sin wave (long moves only)"),
    RANDOM("Random");

    override fun toString(): String = label
}

internal val Color.rgb24: Int get() = rgb and 0xFFFFFF

internal fun coolCursorSettings(): CoolCursorSettings = service()

@Service(Service.Level.APP)
@State(name = "CoolCursorSettings", storages = [Storage("cool-cursor.xml")])
internal class CoolCursorSettings : SimplePersistentStateComponent<CoolCursorSettings.State>(State()) {

    internal class State : BaseState() {
        var headColorRgb: Int by property(DEFAULT_TRAIL_RGB)
        var tailColorRgb: Int by property(DEFAULT_TRAIL_RGB)
        var glowColorRgb: Int by property(DEFAULT_TRAIL_RGB)
        var trailThickness: Float by property(DEFAULT_THICKNESS)
        var trailGlow: Boolean by property(DEFAULT_GLOW)
        var lineCount: Int by property(DEFAULT_LINE_COUNT)
        var trailShape: TrailShape by enum(TrailShape.CURVE)
    }

    @Volatile private var cachedHead: Color = Color(state.headColorRgb)
    @Volatile private var cachedTail: Color = Color(state.tailColorRgb)
    @Volatile private var cachedGlow: Color = Color(state.glowColorRgb)

    override fun loadState(loaded: State) {
        super.loadState(loaded)
        cachedHead = Color(state.headColorRgb)
        cachedTail = Color(state.tailColorRgb)
        cachedGlow = Color(state.glowColorRgb)
    }

    var headColor: Color
        get() = cachedHead
        set(value) {
            val rgb = value.rgb24
            state.headColorRgb = rgb
            cachedHead = Color(rgb)
        }

    var tailColor: Color
        get() = cachedTail
        set(value) {
            val rgb = value.rgb24
            state.tailColorRgb = rgb
            cachedTail = Color(rgb)
        }

    var glowColor: Color
        get() = cachedGlow
        set(value) {
            val rgb = value.rgb24
            state.glowColorRgb = rgb
            cachedGlow = Color(rgb)
        }

    var trailThickness: Float
        get() = state.trailThickness
        set(value) {
            state.trailThickness = value
        }

    var trailGlow: Boolean
        get() = state.trailGlow
        set(value) {
            state.trailGlow = value
        }

    var lineCount: Int
        get() = state.lineCount.coerceIn(1, 3)
        set(value) {
            state.lineCount = value.coerceIn(1, 3)
        }

    var trailShape: TrailShape
        get() = state.trailShape
        set(value) {
            state.trailShape = value
        }
}
