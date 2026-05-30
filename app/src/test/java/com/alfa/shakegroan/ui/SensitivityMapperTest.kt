package com.alfa.shakegroan.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SensitivityMapperTest {

    @Test
    fun `shake slider increases sensitivity to the right`() {
        val low = SensitivityMapper.shakeThreshold(0f)
        val high = SensitivityMapper.shakeThreshold(1f)

        assertTrue(high < low)
        assertEquals(22f, low, 0.001f)
        assertEquals(8f, high, 0.001f)
    }

    @Test
    fun `throw slider increases sensitivity to the right`() {
        val low = SensitivityMapper.throwThreshold(0f)
        val high = SensitivityMapper.throwThreshold(1f)

        assertTrue(high < low)
        assertEquals(30f, low, 0.001f)
        assertEquals(14f, high, 0.001f)
    }

    @Test
    fun `slap slider increases sensitivity to the right`() {
        val low = SensitivityMapper.slapThreshold(0f)
        val high = SensitivityMapper.slapThreshold(1f)

        assertTrue(high < low)
        assertEquals(26f, low, 0.001f)
        assertEquals(12f, high, 0.001f)
    }

    @Test
    fun `progress round trips thresholds`() {
        val shake = SensitivityMapper.shakeProgress(13.5f)
        val throwValue = SensitivityMapper.throwProgress(22f)
        val slap = SensitivityMapper.slapProgress(18f)

        assertEquals(13.5f, SensitivityMapper.shakeThreshold(shake), 0.001f)
        assertEquals(22f, SensitivityMapper.throwThreshold(throwValue), 0.001f)
        assertEquals(18f, SensitivityMapper.slapThreshold(slap), 0.001f)
    }
}
