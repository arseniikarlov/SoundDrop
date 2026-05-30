package com.alfa.shakegroan.media

import org.junit.Assert.assertEquals
import org.junit.Test

class TrimSelectionNormalizerTest {

    @Test
    fun `normalize clamps start and end into duration`() {
        val selection = TrimSelectionNormalizer.normalize(
            durationMs = 5_000L,
            startMs = -400L,
            endMs = 9_500L,
        )

        assertEquals(0L, selection.startMs)
        assertEquals(5_000L, selection.endMs)
    }

    @Test
    fun `normalize enforces minimum clip length`() {
        val selection = TrimSelectionNormalizer.normalize(
            durationMs = 10_000L,
            startMs = 9_900L,
            endMs = 9_950L,
        )

        assertEquals(9_600L, selection.startMs)
        assertEquals(10_000L, selection.endMs)
    }

    @Test
    fun `normalize handles zero duration`() {
        val selection = TrimSelectionNormalizer.normalize(
            durationMs = 0L,
            startMs = 100L,
            endMs = 200L,
        )

        assertEquals(0L, selection.startMs)
        assertEquals(0L, selection.endMs)
    }
}
