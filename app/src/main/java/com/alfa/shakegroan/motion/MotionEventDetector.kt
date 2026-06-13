package com.alfa.shakegroan.motion

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

data class DetectorConfig(
    val shakeEnabled: Boolean = true,
    val throwEnabled: Boolean = true,
    val slapEnabled: Boolean = true,
    val shakeDeltaThreshold: Float = 13.5f,
    val throwImpactThreshold: Float = 95.0f,
    val slapImpactThreshold: Float = 18.0f,
    val slapConfirmationWindowMs: Long = 90L,
    val freeFallThreshold: Float = 4.0f,
    val gyroShakeThreshold: Float = 3.5f,
    val gyroThrowThreshold: Float = 1.8f,
    val gyroThrowImpactBonus: Float = 3.0f,
    val gyroFreshnessMs: Long = 180L,
    val requiredShakePeaks: Int = 3,
    val shakeWindowMs: Long = 850L,
    val minGapBetweenShakePeaksMs: Long = 70L,
    val throwWindowMs: Long = 1200L,
    val cooldownMs: Int = 1000,
)

enum class MotionEventType {
    SHAKE,
    THROW,
    SLAP,
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
    private var pendingSlapAt = Long.MIN_VALUE
    private var lastGyroMagnitude = 0f
    private var lastGyroAt = Long.MIN_VALUE

    fun updateConfig(newConfig: DetectorConfig) {
        config = newConfig
    }

    fun onGyroscopeSample(x: Float, y: Float, z: Float) {
        lastGyroMagnitude = sqrt(x * x + y * y + z * z)
        lastGyroAt = clock()
    }

    fun onSample(x: Float, y: Float, z: Float): MotionEventType? {
        val now = clock()
        val magnitude = sqrt(x * x + y * y + z * z)
        val delta = abs(magnitude - lastMagnitude)
        val gyroMagnitude = recentGyroMagnitude(now)
        lastMagnitude = magnitude

        val throwEvent = if (config.throwEnabled) {
            detectThrow(now, magnitude, gyroMagnitude)
        } else {
            null
        }
        val slapEvent = if (throwEvent == null && config.slapEnabled) {
            detectSlap(now, magnitude, delta)
        } else {
            null
        }
        val shakeEvent = if (throwEvent == null && slapEvent == null && config.shakeEnabled) {
            detectShake(now, delta, gyroMagnitude)
        } else {
            null
        }
        if (throwEvent != null || shakeEvent != null) {
            clearPendingSlap()
        }
        val detectedEvent = throwEvent ?: slapEvent ?: shakeEvent

        if (detectedEvent == null) {
            return null
        }

        if (lastTriggeredAt != Long.MIN_VALUE && now - lastTriggeredAt < config.cooldownMs) {
            return null
        }

        lastTriggeredAt = now
        return detectedEvent
    }

    private fun detectShake(
        now: Long,
        delta: Float,
        gyroMagnitude: Float,
    ): MotionEventType? {
        val strongAccelPeak = delta >= config.shakeDeltaThreshold
        val gyroAssistedPeak = delta >= config.shakeDeltaThreshold * 0.65f &&
            gyroMagnitude >= config.gyroShakeThreshold
        if (!strongAccelPeak && !gyroAssistedPeak) {
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

    private fun detectSlap(
        now: Long,
        magnitude: Float,
        delta: Float,
    ): MotionEventType? {
        if (lastFreeFallAt != Long.MIN_VALUE && now - lastFreeFallAt <= config.throwWindowMs) {
            clearPendingSlap()
            return null
        }

        val impactDeltaThreshold = max(6f, config.shakeDeltaThreshold * 0.6f)
        val strongImpact = magnitude >= config.slapImpactThreshold && delta >= impactDeltaThreshold

        if (strongImpact && pendingSlapAt == Long.MIN_VALUE) {
            pendingSlapAt = now
            return null
        }

        if (strongImpact && pendingSlapAt != Long.MIN_VALUE) {
            clearPendingSlap()
            return null
        }

        if (pendingSlapAt == Long.MIN_VALUE) {
            return null
        }

        val confirmationPassed = now - pendingSlapAt >= config.slapConfirmationWindowMs
        if (!confirmationPassed) {
            return null
        }

        val settledDown = delta < config.shakeDeltaThreshold
        if (!settledDown) {
            return null
        }

        clearPendingSlap()
        return MotionEventType.SLAP
    }

    private fun detectThrow(
        now: Long,
        magnitude: Float,
        gyroMagnitude: Float,
    ): MotionEventType? {
        if (magnitude < config.freeFallThreshold) {
            lastFreeFallAt = now
            return null
        }

        if (lastFreeFallAt != Long.MIN_VALUE) {
            if (now - lastFreeFallAt > config.throwWindowMs) {
                lastFreeFallAt = Long.MIN_VALUE
                return null
            }

            val rotatingInFlight = gyroMagnitude >= config.gyroThrowThreshold
            val impactThreshold = if (rotatingInFlight) {
                (config.throwImpactThreshold - config.gyroThrowImpactBonus).coerceAtLeast(config.freeFallThreshold + 8f)
            } else {
                config.throwImpactThreshold
            }

            if (magnitude >= impactThreshold) {
                lastFreeFallAt = Long.MIN_VALUE
                return MotionEventType.THROW
            }
        }

        return null
    }

    private fun clearPendingSlap() {
        pendingSlapAt = Long.MIN_VALUE
    }

    private fun recentGyroMagnitude(now: Long): Float {
        if (lastGyroAt == Long.MIN_VALUE) {
            return 0f
        }
        return if (now - lastGyroAt <= config.gyroFreshnessMs) {
            lastGyroMagnitude
        } else {
            0f
        }
    }
}
