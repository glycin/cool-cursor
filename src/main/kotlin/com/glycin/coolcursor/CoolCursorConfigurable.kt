package com.glycin.coolcursor

import com.intellij.openapi.options.Configurable
import com.intellij.ui.ColorPanel
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

internal class CoolCursorConfigurable : Configurable {

    private val colorPanel = ColorPanel()
    private var initialRgb: Int = DEFAULT_TRAIL_RGB

    override fun getDisplayName(): String = "Cool Cursor"

    override fun createComponent(): JComponent {
        loadFromSettings()
        return panel {
            row("Dash trail color:") {
                cell(colorPanel)
            }
        }
    }

    override fun isModified(): Boolean {
        val current = colorPanel.selectedColor?.rgb24 ?: return false
        return current != initialRgb
    }

    override fun apply() {
        val picked = colorPanel.selectedColor ?: return
        coolCursorSettings().trailColor = picked
        initialRgb = picked.rgb24
    }

    override fun reset() {
        loadFromSettings()
    }

    private fun loadFromSettings() {
        val color = coolCursorSettings().trailColor
        initialRgb = color.rgb24
        colorPanel.selectedColor = color
    }
}
