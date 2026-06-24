package com.alfa.shakegroan.motion

import kotlin.math.abs
import kotlin.math.sqrt

data class DetectorConfig(
    val throwEnabled: Boolean = true,
    val slapEnabled: Boolean = true,
    val throwImpactThreshold: Float = 67.0f,
    val slapImpactThreshold: Float = 13.0f,
    val slapImpactDeltaThreshold: Float = 8.0f,
    val slapSettleDeltaThreshold: Float = 12.0f,
    val slapConfirmationWindowMs: Long = 90L,
    val freeFallThreshold: Float = 4.0f,
    val gyroThrowThreshold: Float = 1.8f,
    val gyroThrowImpactBonus: Float = 3.0f,
    val gyroFreshnessMs: Long = 180L,
    val throwWindowMs: Long = 1200L,
    val cooldownMs: Int = 1000,
)

enum class MotionEventType {
    THROW,
    SLAP,
}

class MotionEventDetector(
    initialConfig: DetectorConfig,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {

    private var config = initialConfig
    private var lastMagnitude = 9.81f
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
        if (throwEvent != null) {
            clearPendingSlap()
        }
        val detectedEvent = throwEvent ?: slapEvent

        if (detectedEvent == null) {
            return null
        }

        if (lastTriggeredAt != Long.MIN_VALUE && now - lastTriggeredAt < config.cooldownMs) {
            return null
        }

        lastTriggeredAt = now
        return detectedEvent
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

        val strongImpact = magnitude >= config.slapImpactThreshold && delta >= config.slapImpactDeltaThreshold

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

        val settledDown = delta < config.slapSettleDeltaThreshold
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
