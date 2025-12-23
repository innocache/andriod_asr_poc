package com.k2fsa.sherpa.onnx.vad.asr

object AudioPcm {
    fun pcm16ToFloat(samples: ShortArray, length: Int): FloatArray {
        require(length >= 0) { "length must be >= 0" }
        require(length <= samples.size) { "length must be <= samples.size" }

        val out = FloatArray(length)
        for (i in 0 until length) {
            out[i] = samples[i] / 32768.0f
        }
        return out
    }
}
