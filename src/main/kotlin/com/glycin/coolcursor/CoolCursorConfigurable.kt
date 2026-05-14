package com.glycin.coolcursor

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.ColorPanel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.selected
import com.intellij.ui.layout.selected
import java.awt.Color
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel

private enum class LineCountOption(val count: Int, private val label: String) {
    SINGLE(1, "Single"),
    AKIRA(2, "Top + bottom"),
    TRIPLE(3, "Top + middle + bottom");

    override fun toString(): String = label

    companion object {
        fun forCount(count: Int): LineCountOption = entries.firstOrNull { it.count == count } ?: SINGLE
    }
}

internal class CoolCursorConfigurable : Configurable {

    private val headColorPanel = ColorPanel()
    private val tailColorPanel = ColorPanel()
    private val glowColorPanel = ColorPanel()
    private val thicknessSpinner = JSpinner(SpinnerNumberModel(DEFAULT_THICKNESS.toDouble(), 1.0, 50.0, 0.5))
    private val lineCountCombo = ComboBox(DefaultComboBoxModel(LineCountOption.entries.toTypedArray()))
    private val trailShapeCombo = ComboBox(DefaultComboBoxModel(TrailShape.entries.toTypedArray()))
    private val glowCheckbox = JBCheckBox("Glow")

    override fun getDisplayName(): String = "Smooth Caret Trail"

    override fun createComponent(): JComponent {
        loadFromSettings()
        return panel {
            row("Trail thickness:") {
                cell(thicknessSpinner)
            }
            row("Trail shape:") {
                cell(trailShapeCombo)
            }
            row("Number of lines:") {
                cell(lineCountCombo)
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
            || comboLineCount() != s.lineCount
            || comboTrailShape() != s.trailShape
            || glowCheckbox.isSelected != s.trailGlow
    }

    override fun apply() {
        val settings = coolCursorSettings()
        headColorPanel.selectedColor?.let { settings.headColor = it }
        tailColorPanel.selectedColor?.let { settings.tailColor = it }
        glowColorPanel.selectedColor?.let { settings.glowColor = it }
        settings.trailThickness = spinnerThickness()
        settings.lineCount = comboLineCount()
        settings.trailShape = comboTrailShape()
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
        lineCountCombo.selectedItem = LineCountOption.forCount(s.lineCount)
        trailShapeCombo.selectedItem = s.trailShape
        glowCheckbox.isSelected = s.trailGlow
    }

    private fun spinnerThickness(): Float = (thicknessSpinner.value as Number).toFloat()

    private fun comboLineCount(): Int = (lineCountCombo.selectedItem as? LineCountOption)?.count ?: DEFAULT_LINE_COUNT

    private fun comboTrailShape(): TrailShape = (trailShapeCombo.selectedItem as? TrailShape) ?: TrailShape.CURVE

    private fun ColorPanel.differsFrom(other: Color): Boolean {
        val rgb = selectedColor?.rgb24 ?: return false
        return rgb != other.rgb24
    }
}
