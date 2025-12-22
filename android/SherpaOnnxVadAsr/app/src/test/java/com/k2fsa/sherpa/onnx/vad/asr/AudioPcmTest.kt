package com.k2fsa.sherpa.onnx.vad.asr

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioPcmTest {

    @Test
    fun pcm16ToFloat_convertsEdgeValues() {
        val input = shortArrayOf(0, 1, -1, 32767, (-32768).toShort())
        val out = AudioPcm.pcm16ToFloat(input, input.size)

        assertEquals(0.0f, out[0], 0.0f)
        assertEquals(1.0f / 32768.0f, out[1], 1e-8f)
        assertEquals(-1.0f / 32768.0f, out[2], 1e-8f)
        assertEquals(32767.0f / 32768.0f, out[3], 1e-8f)
        assertEquals(-1.0f, out[4], 0.0f)
    }

    @Test
    fun pcm16ToFloat_respectsLength() {
        val input = shortArrayOf(100, 200, 300)
        val out = AudioPcm.pcm16ToFloat(input, 2)

        assertEquals(2, out.size)
        assertEquals(100.0f / 32768.0f, out[0], 1e-8f)
        assertEquals(200.0f / 32768.0f, out[1], 1e-8f)
    }
}
