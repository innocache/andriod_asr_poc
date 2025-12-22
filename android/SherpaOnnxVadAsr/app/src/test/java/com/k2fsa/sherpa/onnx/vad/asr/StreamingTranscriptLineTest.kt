package com.k2fsa.sherpa.onnx.vad.asr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamingTranscriptLineTest {

    @Test
    fun `StreamingTranscriptLine stores text correctly`() {
        val line = StreamingTranscriptLine("hello world", isFinal = true)
        assertEquals("hello world", line.text)
    }

    @Test
    fun `StreamingTranscriptLine isFinal works correctly`() {
        val partial = StreamingTranscriptLine("hello", isFinal = false)
        val final = StreamingTranscriptLine("hello world", isFinal = true)

        assertFalse(partial.isFinal)
        assertTrue(final.isFinal)
    }

    @Test
    fun `StreamingTranscriptLine has timestamp`() {
        val before = System.currentTimeMillis()
        val line = StreamingTranscriptLine("test", isFinal = true)
        val after = System.currentTimeMillis()

        assertTrue(line.timestampMs >= before)
        assertTrue(line.timestampMs <= after)
    }

    @Test
    fun `StreamingTranscriptLine equals works correctly`() {
        val timestamp = System.currentTimeMillis()
        val line1 = StreamingTranscriptLine("hello", isFinal = true, timestampMs = timestamp)
        val line2 = StreamingTranscriptLine("hello", isFinal = true, timestampMs = timestamp)
        val line3 = StreamingTranscriptLine("hello", isFinal = false, timestampMs = timestamp)

        assertEquals(line1, line2)
        assertFalse(line1 == line3)
    }
}
