package com.k2fsa.sherpa.onnx.asr

interface AsrEngine : AutoCloseable {
    val name: String

    /**
     * Decode a single, already-finalized speech segment.
     */
    fun decode(samples: FloatArray, sampleRate: Int): String

    override fun close()
}
