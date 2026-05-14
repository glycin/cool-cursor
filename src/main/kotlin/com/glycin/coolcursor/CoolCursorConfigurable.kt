package com.glycin.coolcursor

import com.intellij.openapi.options.Configurable
import com.intellij.ui.ColorPanel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.selected
import java.awt.Color
import javax.swing.JComponent
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel

internal class CoolCursorConfigurable : Configurable {

    private val headColorPanel = ColorPanel()
    private val tailColorPanel = ColorPanel()
    private val glowColorPanel = ColorPanel()
    private val thicknessSpinner = JSpinner(SpinnerNumberModel(DEFAULT_THICKNESS.toDouble(), 1.0, 50.0, 0.5))
    private val glowCheckbox = JBCheckBox("Glow")

    override fun getDisplayName(): String = "Cool Cursor"

    override fun createComponent(): JComponent {
        loadFromSettings()
        return panel {
            row("Trail thickness:") {
                cell(thicknessSpinner)
            }
            row("Head color:") {
                cell(headColorPanel)
            }
            row("Tail color:") {
                cell(tailColorPanel)
            }
            row {
                cell(glowCheckbox)
            }
            row("Glow color:") {
                cell(glowColorPanel)
            }.enabledIf(glowCheckbox.selected)
        }
    }

    override fun isModified(): Boolean {
        val s = coolCursorSettings()
        return headColorPanel.differsFrom(s.headColor)
            || tailColorPanel.differsFrom(s.tailColor)
            || glowColorPanel.differsFrom(s.glowColor)
            || spinnerThickness() != s.trailThickness
            || glowCheckbox.isSelected != s.trailGlow
    }

    override fun apply() {
        val settings = coolCursorSettings()
        headColorPanel.selectedColor?.let { settings.headColor = it }
        tailColorPanel.selectedColor?.let { settings.tailColor = it }
        glowColorPanel.selectedColor?.let { settings.glowColor = it }
        settings.trailThickness = spinnerThickness()
        settings.trailGlow = glowCheckbox.isSelected
    }

    override fun reset() {
        loadFromSettings()
    }

    private fun loadFromSettings() {
        val s = coolCursorSettings()
        headColorPanel.selectedColor = s.headColor
        tailColorPanel.selectedColor = s.tailColor
        glowColorPanel.selectedColor = s.glowColor
        thicknessSpinner.value = s.trailThickness.toDouble()
        glowCheckbox.isSelected = s.trailGlow
    }

    private fun spinnerThickness(): Float = (thicknessSpinner.value as Number).toFloat()

    private fun ColorPanel.differsFrom(other: Color): Boolean {
        val rgb = selectedColor?.rgb24 ?: return false
        return rgb != other.rgb24
    }
}
