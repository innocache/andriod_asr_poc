package com.k2fsa.sherpa.onnx.asr

import kotlinx.coroutines.flow.Flow

/**
 * Interface for streaming ASR engines that provide real-time transcription.
 * Unlike [AsrEngine] which processes complete segments, streaming engines
 * process audio continuously and emit partial results as they become available.
 */
interface StreamingAsrEngine : AutoCloseable {
    val name: String

    /**
     * Feed audio samples to the recognizer.
     * @param samples Audio samples (16kHz, mono, normalized to [-1, 1])
     * @param sampleRate Sample rate (should be 16000)
     */
    fun acceptWaveform(samples: FloatArray, sampleRate: Int)

    /**
     * Signal that no more audio will be provided.
     */
    fun inputFinished()

    /**
     * Check if there is enough audio data to perform decoding.
     */
    fun isReady(): Boolean

    /**
     * Decode the buffered audio and update internal state.
     */
    fun decode()

    /**
     * Check if an endpoint (end of utterance) has been detected.
     */
    fun isEndpoint(): Boolean

    /**
     * Reset the recognizer to start a new utterance after endpoint.
     */
    fun reset()

    /**
     * Get the current transcription result.
     * @return Current transcription (may be partial or final)
     */
    fun getResult(): StreamingResult

    override fun close()
}

/**
 * Represents a streaming transcription result.
 */
data class StreamingResult(
    /** The transcribed text */
    val text: String,
    /** Whether this is a final result (after endpoint detection) */
    val isFinal: Boolean,
    /** Timestamp tokens if available */
    val timestamps: List<Float> = emptyList()
)
