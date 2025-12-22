package com.k2fsa.sherpa.onnx.vad.asr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineTypeTest {

    @Test
    fun `EngineType MOONSHINE has correct display name`() {
        assertEquals("Moonshine", EngineType.MOONSHINE.displayName)
    }

    @Test
    fun `EngineType VOSK has correct display name`() {
        assertEquals("Vosk", EngineType.VOSK.displayName)
    }

    @Test
    fun `EngineType STREAMING_EN has correct display name`() {
        assertEquals("Streaming EN", EngineType.STREAMING_EN.displayName)
    }

    @Test
    fun `EngineType STREAMING_ZH_EN has correct display name`() {
        assertEquals("Streaming ZH-EN", EngineType.STREAMING_ZH_EN.displayName)
    }

    @Test
    fun `EngineType values contains all engines`() {
        val values = EngineType.values()
        assertEquals(4, values.size)
        assertTrue(values.contains(EngineType.MOONSHINE))
        assertTrue(values.contains(EngineType.VOSK))
        assertTrue(values.contains(EngineType.STREAMING_EN))
        assertTrue(values.contains(EngineType.STREAMING_ZH_EN))
    }

    @Test
    fun `EngineType isStreaming is correct for offline engines`() {
        assertFalse(EngineType.MOONSHINE.isStreaming)
        assertFalse(EngineType.VOSK.isStreaming)
    }

    @Test
    fun `EngineType isStreaming is correct for streaming engines`() {
        assertTrue(EngineType.STREAMING_EN.isStreaming)
        assertTrue(EngineType.STREAMING_ZH_EN.isStreaming)
    }

    @Test
    fun `EngineType offlineEngines returns only non-streaming engines`() {
        val offline = EngineType.offlineEngines
        assertEquals(2, offline.size)
        assertTrue(offline.contains(EngineType.MOONSHINE))
        assertTrue(offline.contains(EngineType.VOSK))
        assertFalse(offline.contains(EngineType.STREAMING_EN))
    }

    @Test
    fun `EngineType streamingEngines returns only streaming engines`() {
        val streaming = EngineType.streamingEngines
        assertEquals(2, streaming.size)
        assertTrue(streaming.contains(EngineType.STREAMING_EN))
        assertTrue(streaming.contains(EngineType.STREAMING_ZH_EN))
        assertFalse(streaming.contains(EngineType.MOONSHINE))
    }

    @Test
    fun `EngineType valueOf works for valid names`() {
        assertEquals(EngineType.MOONSHINE, EngineType.valueOf("MOONSHINE"))
        assertEquals(EngineType.VOSK, EngineType.valueOf("VOSK"))
        assertEquals(EngineType.STREAMING_EN, EngineType.valueOf("STREAMING_EN"))
        assertEquals(EngineType.STREAMING_ZH_EN, EngineType.valueOf("STREAMING_ZH_EN"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `EngineType valueOf throws for invalid name`() {
        EngineType.valueOf("INVALID")
    }
}
