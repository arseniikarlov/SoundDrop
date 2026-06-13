package com.alfa.shakegroan.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SensitivityMapperTest {

    @Test
    fun `throw slider increases sensitivity to the right`() {
        val low = SensitivityMapper.throwThreshold(0f)
        val high = SensitivityMapper.throwThreshold(1f)

        assertTrue(high < low)
        assertEquals(150f, low, 0.001f)
        assertEquals(70f, high, 0.001f)
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
        val throwValue = SensitivityMapper.throwProgress(95f)
        val slap = SensitivityMapper.slapProgress(18f)

        assertEquals(95f, SensitivityMapper.throwThreshold(throwValue), 0.001f)
        assertEquals(18f, SensitivityMapper.slapThreshold(slap), 0.001f)
    }
}
