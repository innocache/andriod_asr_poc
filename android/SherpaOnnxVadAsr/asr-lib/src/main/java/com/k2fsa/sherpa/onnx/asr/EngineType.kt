package com.k2fsa.sherpa.onnx.asr

/**
 * ASR engine type.
 */
enum class EngineType(val displayName: String, val isStreaming: Boolean = false) {
    /** Offline VAD + Moonshine ASR (batch processing) */
    MOONSHINE("Moonshine", isStreaming = false),

    /** Offline VAD + Vosk ASR (batch processing) */
    VOSK("Vosk", isStreaming = false),

    /** Streaming ASR with built-in endpoint detection (English small model) */
    STREAMING_EN("Streaming EN", isStreaming = true),

    /** Streaming ASR with bilingual Chinese + English */
    STREAMING_ZH_EN("Streaming ZH-EN", isStreaming = true);

    companion object {
        val offlineEngines: List<EngineType> = values().filter { !it.isStreaming }
        val streamingEngines: List<EngineType> = values().filter { it.isStreaming }
    }
}
