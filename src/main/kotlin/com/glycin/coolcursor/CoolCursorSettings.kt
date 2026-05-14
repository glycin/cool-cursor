package com.glycin.coolcursor

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import java.awt.Color

internal const val DEFAULT_TRAIL_RGB = 0x8B5CF6

internal enum class TrailStyle(private val label: String) {
    SOLID("Solid Trail"),
    LINE("Line Trail");

    override fun toString(): String = label
}

internal val Color.rgb24: Int get() = rgb and 0xFFFFFF

internal fun coolCursorSettings(): CoolCursorSettings = service()

@Service(Service.Level.APP)
@State(name = "CoolCursorSettings", storages = [Storage("cool-cursor.xml")])
internal class CoolCursorSettings : SimplePersistentStateComponent<CoolCursorSettings.State>(State()) {

    internal class State : BaseState() {
        var trailColorRgb: Int by property(DEFAULT_TRAIL_RGB)
        var trailStyle: TrailStyle by enum(TrailStyle.SOLID)
    }

    @Volatile
    private var cachedColor: Color = Color(state.trailColorRgb)

    override fun loadState(loaded: State) {
        super.loadState(loaded)
        cachedColor = Color(state.trailColorRgb)
    }

    var trailColor: Color
        get() = cachedColor
        set(value) {
            val rgb = value.rgb24
            state.trailColorRgb = rgb
            cachedColor = Color(rgb)
        }

    var trailStyle: TrailStyle
        get() = state.trailStyle
        set(value) {
            state.trailStyle = value
        }
}
