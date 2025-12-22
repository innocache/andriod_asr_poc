package com.k2fsa.sherpa.onnx.vad.asr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamingResultTest {

    @Test
    fun `StreamingResult stores text correctly`() {
        val result = StreamingResult("hello world", isFinal = false)
        assertEquals("hello world", result.text)
    }

    @Test
    fun `StreamingResult isFinal property works`() {
        val partial = StreamingResult("hello", isFinal = false)
        val final = StreamingResult("hello world", isFinal = true)

        assertFalse(partial.isFinal)
        assertTrue(final.isFinal)
    }

    @Test
    fun `StreamingResult default timestamps is empty`() {
        val result = StreamingResult("test", isFinal = true)
        assertTrue(result.timestamps.isEmpty())
    }

    @Test
    fun `StreamingResult stores timestamps correctly`() {
        val timestamps = listOf(0.1f, 0.5f, 1.0f)
        val result = StreamingResult("hello world", isFinal = true, timestamps = timestamps)

        assertEquals(3, result.timestamps.size)
        assertEquals(0.1f, result.timestamps[0], 0.001f)
        assertEquals(0.5f, result.timestamps[1], 0.001f)
        assertEquals(1.0f, result.timestamps[2], 0.001f)
    }

    @Test
    fun `StreamingResult equals works correctly`() {
        val result1 = StreamingResult("hello", isFinal = true)
        val result2 = StreamingResult("hello", isFinal = true)
        val result3 = StreamingResult("hello", isFinal = false)
        val result4 = StreamingResult("world", isFinal = true)

        assertEquals(result1, result2)
        assertFalse(result1 == result3)
        assertFalse(result1 == result4)
    }
}
