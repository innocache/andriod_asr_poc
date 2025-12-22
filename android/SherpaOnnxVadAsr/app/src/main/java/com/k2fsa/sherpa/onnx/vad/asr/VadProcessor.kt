package com.k2fsa.sherpa.onnx.vad.asr

import android.content.res.AssetManager
import android.util.Log
import com.k2fsa.sherpa.onnx.SpeechSegment
import com.k2fsa.sherpa.onnx.Vad
import com.k2fsa.sherpa.onnx.getVadModelConfig

private const val TAG = "VadProcessor"

/**
 * Wraps the VAD lifecycle and provides a clean interface for processing audio.
 */
class VadProcessor(
    assetManager: AssetManager,
    vadModelType: Int = 0, // 0 = Silero, 1 = Ten
) : AutoCloseable {

    private val vad: Vad

    init {
        val config = getVadModelConfig(vadModelType)
            ?: throw IllegalArgumentException("Unknown VAD model type: $vadModelType")

        Log.i(TAG, "Initializing VAD with type=$vadModelType")
        vad = Vad(assetManager = assetManager, config = config)
        Log.i(TAG, "VAD initialized")
    }

    /**
     * Feed audio samples to the VAD.
     */
    fun acceptWaveform(samples: FloatArray) {
        vad.acceptWaveform(samples)
    }

    /**
     * Check if there are speech segments ready.
     */
    fun hasSegments(): Boolean = !vad.empty()

    /**
     * Pop the next speech segment.
     */
    fun popSegment(): SpeechSegment {
        val segment = vad.front()
        vad.pop()
        return segment
    }

    /**
     * Pop all available speech segments.
     */
    fun popAllSegments(): List<SpeechSegment> {
        val segments = mutableListOf<SpeechSegment>()
        while (!vad.empty()) {
            segments.add(vad.front())
            vad.pop()
        }
        return segments
    }

    /**
     * Reset VAD state (call between recordings).
     */
    fun reset() {
        vad.reset()
    }

    /**
     * Check if speech is currently detected.
     */
    fun isSpeechDetected(): Boolean = vad.isSpeechDetected()

    override fun close() {
        try {
            vad.release()
            Log.i(TAG, "VAD released")
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing VAD", e)
        }
    }
}
