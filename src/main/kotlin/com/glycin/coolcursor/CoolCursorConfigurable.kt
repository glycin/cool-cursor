package com.glycin.coolcursor

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.ColorPanel
import com.intellij.ui.dsl.builder.panel
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent

internal class CoolCursorConfigurable : Configurable {

    private val colorPanel = ColorPanel()
    private val styleCombo = ComboBox(DefaultComboBoxModel(TrailStyle.values()))

    private var initialRgb: Int = DEFAULT_TRAIL_RGB
    private var initialStyle: TrailStyle = TrailStyle.SOLID

    override fun getDisplayName(): String = "Cool Cursor"

    override fun createComponent(): JComponent {
        loadFromSettings()
        return panel {
            row("Trail style:") {
                cell(styleCombo)
            }
            row("Trail color:") {
                cell(colorPanel)
            }
        }
    }

    override fun isModified(): Boolean {
        val rgb = colorPanel.selectedColor?.rgb24
        if (rgb != null && rgb != initialRgb) return true
        return styleCombo.selectedItem != initialStyle
    }

    override fun apply() {
        val settings = coolCursorSettings()
        colorPanel.selectedColor?.let {
            settings.trailColor = it
            initialRgb = it.rgb24
        }
        (styleCombo.selectedItem as? TrailStyle)?.let {
            settings.trailStyle = it
            initialStyle = it
        }
    }

    override fun reset() {
        loadFromSettings()
    }

    private fun loadFromSettings() {
        val settings = coolCursorSettings()
        val color = settings.trailColor
        initialRgb = color.rgb24
        colorPanel.selectedColor = color
        initialStyle = settings.trailStyle
        styleCombo.selectedItem = initialStyle
    }
}
