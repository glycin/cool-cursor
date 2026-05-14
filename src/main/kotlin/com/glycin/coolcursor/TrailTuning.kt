package com.glycin.coolcursor

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal object TrailTuning {
    const val DASH_DURATION_MS = 240
    val FRAME_INTERVAL: Duration = 16.milliseconds

    const val HEAD_DELAY_MS = 15.0
    const val TAIL_DELAY_MS = 90.0

    const val BOW = 0.18

    const val SINE_THRESHOLD_PX = 100.0
    const val SINE_AMPLITUDE_PX = 5f
    const val SINE_CYCLES_PER_PX = 1f / 60f
    const val SINE_SAMPLES = 24

    const val HALO_EXTRA = 6f
    const val HALO_ALPHA = 90

    const val MIN_GRADIENT_LEN_SQ = 1.0
    val GRADIENT_FRACTIONS = floatArrayOf(0f, 1f)

    val SINGLE_OFFSETS = doubleArrayOf(0.0)
    val AKIRA_OFFSETS = doubleArrayOf(-0.5, 0.5)
    val TRIPLE_OFFSETS = doubleArrayOf(-0.5, 0.0, 0.5)

    val RANDOMIZABLE_SHAPES = arrayOf(TrailShape.STRAIGHT, TrailShape.CURVE, TrailShape.SINE)
}
