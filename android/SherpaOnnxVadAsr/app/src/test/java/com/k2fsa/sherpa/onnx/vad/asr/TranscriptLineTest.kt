package com.k2fsa.sherpa.onnx.vad.asr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TranscriptLineTest {

    @Test
    fun `TranscriptLine formatted includes all metrics`() {
        val line = TranscriptLine(
            index = 5,
            text = "hello world",
            engineName = "TestEngine",
            segmentDurationMs = 1500.0,
            decodeTimeMs = 300.0,
            rtf = 0.2
        )

        val formatted = line.formatted()

        assert(formatted.contains("5:"))
        assert(formatted.contains("[TestEngine]"))
        assert(formatted.contains("hello world"))
        assert(formatted.contains("seg=1500ms"))
        assert(formatted.contains("decode=300ms"))
        assert(formatted.contains("rtf=0.20"))
    }

    @Test
    fun `TranscriptLine formatted handles zero values`() {
        val line = TranscriptLine(
            index = 0,
            text = "",
            engineName = "Engine",
            segmentDurationMs = 0.0,
            decodeTimeMs = 0.0,
            rtf = 0.0
        )

        val formatted = line.formatted()
        assertNotNull(formatted)
        assert(formatted.contains("seg=0ms"))
        assert(formatted.contains("rtf=0.00"))
    }

    @Test
    fun `TranscriptLine equality based on all fields`() {
        val line1 = TranscriptLine(1, "text", "engine", 100.0, 50.0, 0.5)
        val line2 = TranscriptLine(1, "text", "engine", 100.0, 50.0, 0.5)
        val line3 = TranscriptLine(2, "text", "engine", 100.0, 50.0, 0.5)

        assertEquals(line1, line2)
        assert(line1 != line3)
    }
}
