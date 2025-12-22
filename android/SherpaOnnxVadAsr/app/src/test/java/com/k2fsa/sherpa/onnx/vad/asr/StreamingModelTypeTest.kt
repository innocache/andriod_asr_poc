package com.k2fsa.sherpa.onnx.vad.asr

import org.junit.Assert.assertEquals
import org.junit.Test

class StreamingModelTypeTest {

    @Test
    fun `StreamingModelType ZIPFORMER_EN_SMALL has correct display name`() {
        assertEquals("English Small", StreamingModelType.ZIPFORMER_EN_SMALL.displayName)
    }

    @Test
    fun `StreamingModelType ZIPFORMER_EN_2023_06_26 has correct display name`() {
        assertEquals("English 2023-06-26", StreamingModelType.ZIPFORMER_EN_2023_06_26.displayName)
    }

    @Test
    fun `StreamingModelType BILINGUAL_ZH_EN has correct display name`() {
        assertEquals("Bilingual ZH-EN", StreamingModelType.BILINGUAL_ZH_EN.displayName)
    }

    @Test
    fun `StreamingModelType PARAFORMER_BILINGUAL has correct display name`() {
        assertEquals("Paraformer Bilingual", StreamingModelType.PARAFORMER_BILINGUAL.displayName)
    }

    @Test
    fun `StreamingModelType has correct model directories`() {
        assertEquals(
            "sherpa-onnx-streaming-zipformer-en-20M-2023-02-17",
            StreamingModelType.ZIPFORMER_EN_SMALL.modelDir
        )
        assertEquals(
            "sherpa-onnx-streaming-zipformer-en-2023-06-26",
            StreamingModelType.ZIPFORMER_EN_2023_06_26.modelDir
        )
        assertEquals(
            "sherpa-onnx-streaming-zipformer-bilingual-zh-en-2023-02-20",
            StreamingModelType.BILINGUAL_ZH_EN.modelDir
        )
        assertEquals(
            "sherpa-onnx-streaming-paraformer-bilingual-zh-en",
            StreamingModelType.PARAFORMER_BILINGUAL.modelDir
        )
    }

    @Test
    fun `StreamingModelType values contains all types`() {
        val values = StreamingModelType.values()
        assertEquals(4, values.size)
    }

    @Test
    fun `StreamingModelType fromOrdinal returns correct type`() {
        assertEquals(StreamingModelType.ZIPFORMER_EN_SMALL, StreamingModelType.fromOrdinal(0))
        assertEquals(StreamingModelType.ZIPFORMER_EN_2023_06_26, StreamingModelType.fromOrdinal(1))
        assertEquals(StreamingModelType.BILINGUAL_ZH_EN, StreamingModelType.fromOrdinal(2))
        assertEquals(StreamingModelType.PARAFORMER_BILINGUAL, StreamingModelType.fromOrdinal(3))
    }

    @Test
    fun `StreamingModelType fromOrdinal returns default for invalid ordinal`() {
        assertEquals(StreamingModelType.ZIPFORMER_EN_SMALL, StreamingModelType.fromOrdinal(-1))
        assertEquals(StreamingModelType.ZIPFORMER_EN_SMALL, StreamingModelType.fromOrdinal(100))
    }
}
