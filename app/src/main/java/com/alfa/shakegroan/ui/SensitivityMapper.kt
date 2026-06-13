package com.alfa.shakegroan.ui

internal object SensitivityMapper {
    fun shakeProgress(threshold: Float): Float = descendingProgress(
        threshold = threshold,
        lowSensitivityThreshold = 22f,
        highSensitivityThreshold = 8f,
    )

    fun shakeThreshold(progress: Float): Float = descendingThreshold(
        progress = progress,
        lowSensitivityThreshold = 22f,
        highSensitivityThreshold = 8f,
    )

    fun throwProgress(threshold: Float): Float = descendingProgress(
        threshold = threshold,
        lowSensitivityThreshold = 150f,
        highSensitivityThreshold = 70f,
    )

    fun throwThreshold(progress: Float): Float = descendingThreshold(
        progress = progress,
        lowSensitivityThreshold = 150f,
        highSensitivityThreshold = 70f,
    )

    fun slapProgress(threshold: Float): Float = descendingProgress(
        threshold = threshold,
        lowSensitivityThreshold = 26f,
        highSensitivityThreshold = 12f,
    )

    fun slapThreshold(progress: Float): Float = descendingThreshold(
        progress = progress,
        lowSensitivityThreshold = 26f,
        highSensitivityThreshold = 12f,
    )

    private fun descendingProgress(
        threshold: Float,
        lowSensitivityThreshold: Float,
        highSensitivityThreshold: Float,
    ): Float {
        return (
            (lowSensitivityThreshold - threshold) /
                (lowSensitivityThreshold - highSensitivityThreshold)
            ).coerceIn(0f, 1f)
    }

    private fun descendingThreshold(
        progress: Float,
        lowSensitivityThreshold: Float,
        highSensitivityThreshold: Float,
    ): Float {
        val clampedProgress = progress.coerceIn(0f, 1f)
        return lowSensitivityThreshold +
            (highSensitivityThreshold - lowSensitivityThreshold) * clampedProgress
    }
}
