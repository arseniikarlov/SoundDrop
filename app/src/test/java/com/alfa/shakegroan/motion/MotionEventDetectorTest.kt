package com.alfa.shakegroan.motion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MotionEventDetectorTest {

    @Test
    fun `detects throw after free fall and impact`() {
        var now = 0L
        val detector = MotionEventDetector(
            initialConfig = DetectorConfig(
                throwEnabled = true,
                slapEnabled = false,
                throwImpactThreshold = 20f,
                freeFallThreshold = 3f,
                cooldownMs = 0
            ),
            clock = { now }
        )

        assertNull(detector.onSample(0.3f, 0.2f, 0.4f))
        now += 200
        assertEquals(MotionEventType.THROW, detector.onSample(0f, 0f, 22f))
    }

    @Test
    fun `gyro lowers throw impact threshold when phone rotates in flight`() {
        var now = 0L
        val detector = MotionEventDetector(
            initialConfig = DetectorConfig(
                throwEnabled = true,
                slapEnabled = true,
                throwImpactThreshold = 20f,
                gyroThrowThreshold = 1.5f,
                gyroThrowImpactBonus = 3f,
                cooldownMs = 0
            ),
            clock = { now }
        )

        assertNull(detector.onSample(0.3f, 0.2f, 0.4f))
        now += 100
        detector.onGyroscopeSample(0f, 2f, 0f)
        assertEquals(MotionEventType.THROW, detector.onSample(0f, 0f, 18f))
    }

    @Test
    fun `stale gyro sample does not lower throw threshold`() {
        var now = 0L
        val detector = MotionEventDetector(
            initialConfig = DetectorConfig(
                throwEnabled = true,
                slapEnabled = false,
                throwImpactThreshold = 20f,
                gyroThrowThreshold = 1.5f,
                gyroThrowImpactBonus = 3f,
                gyroFreshnessMs = 180L,
                cooldownMs = 0
            ),
            clock = { now }
        )

        detector.onGyroscopeSample(0f, 2f, 0f)
        assertNull(detector.onSample(0.3f, 0.2f, 0.4f))
        now += 300
        assertNull(detector.onSample(0f, 0f, 18f))
    }

    @Test
    fun `detects slap on strong impact without free fall`() {
        var now = 0L
        val detector = MotionEventDetector(
            initialConfig = DetectorConfig(
                throwEnabled = false,
                slapEnabled = true,
                slapImpactThreshold = 18f,
                slapConfirmationWindowMs = 150L,
                cooldownMs = 0
            ),
            clock = { now }
        )

        assertNull(detector.onSample(0f, 0f, 9.81f))
        now += 100
        assertNull(detector.onSample(0f, 0f, 20f))
        now += 170
        assertEquals(MotionEventType.SLAP, detector.onSample(0f, 0f, 9.81f))
    }

    @Test
    fun `detects slap quickly after isolated impact`() {
        var now = 0L
        val detector = MotionEventDetector(
            initialConfig = DetectorConfig(
                throwEnabled = false,
                slapEnabled = true,
                cooldownMs = 0
            ),
            clock = { now }
        )

        assertNull(detector.onSample(0f, 0f, 9.81f))
        now += 50
        assertNull(detector.onSample(0f, 0f, 20f))
        now += 90
        assertEquals(MotionEventType.SLAP, detector.onSample(0f, 0f, 9.81f))
    }

    @Test
    fun `prioritizes throw over slap after free fall`() {
        var now = 0L
        val detector = MotionEventDetector(
            initialConfig = DetectorConfig(
                throwEnabled = true,
                slapEnabled = true,
                throwImpactThreshold = 20f,
                slapImpactThreshold = 18f,
                freeFallThreshold = 3f,
                cooldownMs = 0
            ),
            clock = { now }
        )

        assertNull(detector.onSample(0.3f, 0.2f, 0.4f))
        now += 120
        assertEquals(MotionEventType.THROW, detector.onSample(0f, 0f, 22f))
    }

    @Test
    fun `respects cooldown between events`() {
        var now = 0L
        val detector = MotionEventDetector(
            initialConfig = DetectorConfig(
                throwEnabled = false,
                slapEnabled = true,
                cooldownMs = 1_000
            ),
            clock = { now }
        )

        detector.onSample(0f, 0f, 9.81f)
        now += 100
        detector.onSample(0f, 0f, 20f)
        now += 90
        assertEquals(MotionEventType.SLAP, detector.onSample(0f, 0f, 9.81f))

        now += 100
        detector.onSample(0f, 0f, 20f)
        now += 90
        assertNull(detector.onSample(0f, 0f, 9.81f))
    }
}
