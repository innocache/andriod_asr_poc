package com.k2fsa.sherpa.onnx.vad.asr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionStatsTest {

    @Test
    fun `default SessionStats has zero values`() {
        val stats = SessionStats()
        assertEquals(0, stats.totalSegments)
        assertEquals(0.0, stats.totalAudioDurationMs, 0.001)
        assertEquals(0.0, stats.totalDecodeTimeMs, 0.001)
        assertEquals(0.0, stats.averageRtf, 0.001)
    }

    @Test
    fun `formatted returns no segments message for empty stats`() {
        val stats = SessionStats()
        assertEquals("No segments yet", stats.formatted())
    }

    @Test
    fun `formatted includes all stats when segments exist`() {
        val stats = SessionStats(
            totalSegments = 5,
            totalAudioDurationMs = 10000.0, // 10 seconds
            totalDecodeTimeMs = 2000.0,      // 2 seconds
            averageRtf = 0.2
        )

        val formatted = stats.formatted()

        assertTrue(formatted.contains("5 segments"))
        assertTrue(formatted.contains("10.0s audio"))
        assertTrue(formatted.contains("avg RTF: 0.20"))
    }

    @Test
    fun `SessionStats copy works correctly`() {
        val original = SessionStats(1, 1000.0, 500.0, 0.5)
        val updated = original.copy(totalSegments = 2)

        assertEquals(2, updated.totalSegments)
        assertEquals(1000.0, updated.totalAudioDurationMs, 0.001)
        assertEquals(1, original.totalSegments)
    }

    @Test
    fun `SessionStats equality based on all fields`() {
        val stats1 = SessionStats(1, 1000.0, 500.0, 0.5)
        val stats2 = SessionStats(1, 1000.0, 500.0, 0.5)
        val stats3 = SessionStats(2, 1000.0, 500.0, 0.5)

        assertEquals(stats1, stats2)
        assertTrue(stats1 != stats3)
    }
}
