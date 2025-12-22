package com.k2fsa.sherpa.onnx.vad.asr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AsrEngineFactoryTest {

    @Test
    fun `getErrorMessage for MOONSHINE returns generic message`() {
        val message = AsrEngineFactory.getErrorMessage(EngineType.MOONSHINE, emptyList())
        assertTrue(message.contains("Moonshine"))
    }

    @Test
    fun `getErrorMessage for VOSK includes checked paths`() {
        val candidates = listOf(
            File("/path/to/model1"),
            File("/path/to/model2")
        )
        val message = AsrEngineFactory.getErrorMessage(EngineType.VOSK, candidates)

        assertTrue(message.contains("Vosk"))
        assertTrue(message.contains("/path/to/model1"))
        assertTrue(message.contains("/path/to/model2"))
        assertTrue(message.contains("vosk_model_push_device.sh"))
    }

    @Test
    fun `getErrorMessage for VOSK with empty candidates`() {
        val message = AsrEngineFactory.getErrorMessage(EngineType.VOSK, emptyList())
        assertTrue(message.contains("Vosk"))
    }
}
