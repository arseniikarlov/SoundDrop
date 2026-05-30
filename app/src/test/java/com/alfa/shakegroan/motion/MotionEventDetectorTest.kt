package com.alfa.shakegroan.motion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MotionEventDetectorTest {

    @Test
    fun `detects shake after three strong peaks`() {
        var now = 0L
        val detector = MotionEventDetector(
            initialConfig = DetectorConfig(
                shakeEnabled = true,
                throwEnabled = false,
                slapEnabled = false,
                shakeDeltaThreshold = 10f,
                cooldownMs = 0
            ),
            clock = { now }
        )

        assertNull(detector.onSample(0f, 0f, 9.81f))
        now += 100
        assertNull(detector.onSample(25f, 0f, 0f))
        now += 100
        assertNull(detector.onSample(0f, 0f, 0f))
        now += 100
        assertEquals(MotionEventType.SHAKE, detector.onSample(25f, 0f, 0f))
    }

    @Test
    fun `detects shake even when slap mode is enabled`() {
        var now = 0L
        val detector = MotionEventDetector(
            initialConfig = DetectorConfig(
                shakeEnabled = true,
                throwEnabled = false,
                slapEnabled = true,
                shakeDeltaThreshold = 10f,
                slapImpactThreshold = 18f,
                cooldownMs = 0
            ),
            clock = { now }
        )

        assertNull(detector.onSample(0f, 0f, 9.81f))
        now += 100
        assertNull(detector.onSample(25f, 0f, 0f))
        now += 100
        assertNull(detector.onSample(0f, 0f, 0f))
        now += 100
        assertEquals(MotionEventType.SHAKE, detector.onSample(25f, 0f, 0f))
    }

    @Test
    fun `detects throw after free fall and impact`() {
        var now = 0L
        val detector = MotionEventDetector(
            initialConfig = DetectorConfig(
                shakeEnabled = false,
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
    fun `detects slap on strong impact without free fall`() {
        var now = 0L
        val detector = MotionEventDetector(
            initialConfig = DetectorConfig(
                shakeEnabled = false,
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
    fun `prioritizes throw over slap after free fall`() {
        var now = 0L
        val detector = MotionEventDetector(
            initialConfig = DetectorConfig(
                shakeEnabled = false,
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
                shakeEnabled = true,
                throwEnabled = false,
                slapEnabled = false,
                shakeDeltaThreshold = 8f,
                cooldownMs = 1_000
            ),
            clock = { now }
        )

        detector.onSample(0f, 0f, 9.81f)
        now += 100
        detector.onSample(25f, 0f, 0f)
        now += 100
        detector.onSample(0f, 0f, 0f)
        now += 100
        assertEquals(MotionEventType.SHAKE, detector.onSample(25f, 0f, 0f))

        now += 100
        detector.onSample(0f, 0f, 0f)
        now += 100
        detector.onSample(25f, 0f, 0f)
        now += 100
        assertNull(detector.onSample(0f, 0f, 0f))
    }
}
