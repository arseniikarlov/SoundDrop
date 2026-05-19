package com.alfa.shakegroan.motion

import kotlin.math.abs
import kotlin.math.sqrt

data class DetectorConfig(
    val shakeEnabled: Boolean = true,
    val throwEnabled: Boolean = true,
    val shakeDeltaThreshold: Float = 13.5f,
    val throwImpactThreshold: Float = 22.0f,
    val freeFallThreshold: Float = 3.0f,
    val requiredShakePeaks: Int = 3,
    val shakeWindowMs: Long = 850L,
    val minGapBetweenShakePeaksMs: Long = 70L,
    val throwWindowMs: Long = 900L,
    val cooldownMs: Int = 1400,
)

enum class MotionEventType {
    SHAKE,
    THROW,
}

class MotionEventDetector(
    initialConfig: DetectorConfig,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {

    private var config = initialConfig
    private var lastMagnitude = 9.81f
    private var shakeWindowStart = 0L
    private var shakePeakCount = 0
    private var lastShakePeakAt = Long.MIN_VALUE
    private var lastFreeFallAt = Long.MIN_VALUE
    private var lastTriggeredAt = Long.MIN_VALUE

    fun updateConfig(newConfig: DetectorConfig) {
        config = newConfig
    }

    fun onSample(x: Float, y: Float, z: Float): MotionEventType? {
        val now = clock()
        val magnitude = sqrt(x * x + y * y + z * z)

        val throwEvent = if (config.throwEnabled) detectThrow(now, magnitude) else null
        val shakeEvent = if (throwEvent == null && config.shakeEnabled) detectShake(now, magnitude) else null
        val detectedEvent = throwEvent ?: shakeEvent

        if (detectedEvent == null) {
            return null
        }

        if (lastTriggeredAt != Long.MIN_VALUE && now - lastTriggeredAt < config.cooldownMs) {
            return null
        }

        lastTriggeredAt = now
        return detectedEvent
    }

    private fun detectShake(now: Long, magnitude: Float): MotionEventType? {
        val delta = abs(magnitude - lastMagnitude)
        lastMagnitude = magnitude

        if (delta < config.shakeDeltaThreshold) {
            return null
        }

        if (lastShakePeakAt != Long.MIN_VALUE && now - lastShakePeakAt < config.minGapBetweenShakePeaksMs) {
            return null
        }

        if (shakeWindowStart == 0L || now - shakeWindowStart > config.shakeWindowMs) {
            shakeWindowStart = now
            shakePeakCount = 1
        } else {
            shakePeakCount += 1
        }
        lastShakePeakAt = now

        if (shakePeakCount >= config.requiredShakePeaks) {
            shakePeakCount = 0
            shakeWindowStart = 0L
            return MotionEventType.SHAKE
        }

        return null
    }

    private fun detectThrow(now: Long, magnitude: Float): MotionEventType? {
        if (magnitude < config.freeFallThreshold) {
            lastFreeFallAt = now
            return null
        }

        if (lastFreeFallAt != Long.MIN_VALUE) {
            if (now - lastFreeFallAt > config.throwWindowMs) {
                lastFreeFallAt = Long.MIN_VALUE
                return null
            }

            if (magnitude >= config.throwImpactThreshold) {
                lastFreeFallAt = Long.MIN_VALUE
                return MotionEventType.THROW
            }
        }

        return null
    }
}

