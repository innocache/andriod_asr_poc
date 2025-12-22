package com.k2fsa.sherpa.onnx.vad.asr

/**
 * Represents the state of the ASR recording/transcription pipeline.
 */
sealed class RecordingState {
    /** Initial state - ready to start recording */
    object Idle : RecordingState()

    /** Initializing models (VAD + ASR) */
    object Initializing : RecordingState()

    /** Currently recording and processing audio */
    object Recording : RecordingState()

    /** An error occurred */
    data class Error(val message: String) : RecordingState()
}

/**
 * UI state for the main screen.
 */
data class MainUiState(
    val recordingState: RecordingState = RecordingState.Initializing,
    val transcriptLines: List<TranscriptLine> = emptyList(),
    val selectedEngine: EngineType = EngineType.MOONSHINE,
    val availableEngines: List<EngineType> = EngineType.values().toList(),
    val audioLevel: Float = 0f, // 0.0 to 1.0 normalized audio level
    val sessionStats: SessionStats = SessionStats(),
)

/**
 * Session statistics for the current recording session.
 */
data class SessionStats(
    val totalSegments: Int = 0,
    val totalAudioDurationMs: Double = 0.0,
    val totalDecodeTimeMs: Double = 0.0,
    val averageRtf: Double = 0.0,
) {
    fun formatted(): String {
        if (totalSegments == 0) return "No segments yet"
        val avgRtfStr = "%.2f".format(averageRtf)
        val totalAudioSec = "%.1f".format(totalAudioDurationMs / 1000.0)
        return "$totalSegments segments | ${totalAudioSec}s audio | avg RTF: $avgRtfStr"
    }
}

/**
 * A single transcript line with metadata.
 */
data class TranscriptLine(
    val index: Int,
    val text: String,
    val engineName: String,
    val segmentDurationMs: Double,
    val decodeTimeMs: Double,
    val rtf: Double,
) {
    fun formatted(): String {
        val metrics = "seg=${"%.0f".format(segmentDurationMs)}ms decode=${"%.0f".format(decodeTimeMs)}ms rtf=${"%.2f".format(rtf)}"
        return "$index: [$engineName] $text | $metrics"
    }
}

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

/**
 * Streaming-specific transcript line with partial result support.
 */
data class StreamingTranscriptLine(
    val text: String,
    val isFinal: Boolean,
    val timestampMs: Long = System.currentTimeMillis(),
)
