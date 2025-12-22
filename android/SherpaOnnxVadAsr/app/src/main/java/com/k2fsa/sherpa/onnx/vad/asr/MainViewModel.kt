package com.k2fsa.sherpa.onnx.vad.asr

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.sqrt

private const val TAG = "MainViewModel"
private const val PREFS_NAME = "settings"
private const val PREF_ENGINE = "asr_engine"

/**
 * ViewModel for the main ASR screen.
 * Manages VAD, ASR engine lifecycle, and audio processing.
 * Supports both offline (VAD + batch ASR) and streaming modes.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val sampleRateHz = 16000

    // Offline mode components
    private var vadProcessor: VadProcessor? = null
    private var asrEngine: AsrEngine? = null

    // Streaming mode components
    private var streamingEngine: StreamingAsrEngine? = null
    private var currentStreamingText = StringBuilder()
    private var streamingSegmentCount = 0

    private var recordingJob: Job? = null

    private val audioRecorder = AudioRecorder(sampleRateHz)
    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        // Load saved engine preference
        val savedEngine = prefs.getString(PREF_ENGINE, EngineType.MOONSHINE.name)
        val engineType = runCatching { EngineType.valueOf(savedEngine ?: "") }
            .getOrDefault(EngineType.MOONSHINE)

        _uiState.update { it.copy(selectedEngine = engineType) }
    }

    /**
     * Initialize the VAD and ASR models.
     * Should be called after permissions are granted.
     */
    fun initialize() {
        if (_uiState.value.recordingState != RecordingState.Initializing) {
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val selectedEngine = _uiState.value.selectedEngine

                if (selectedEngine.isStreaming) {
                    Log.i(TAG, "Initializing streaming engine: ${selectedEngine.displayName}")
                    initStreamingEngine(selectedEngine)
                } else {
                    Log.i(TAG, "Initializing VAD...")
                    vadProcessor = VadProcessor(getApplication<Application>().assets)

                    Log.i(TAG, "Initializing ASR engine: ${selectedEngine.displayName}")
                    initAsrEngine(selectedEngine)
                }

                _uiState.update { it.copy(recordingState = RecordingState.Idle) }
                Log.i(TAG, "Initialization complete")
            } catch (e: Exception) {
                Log.e(TAG, "Initialization failed", e)
                _uiState.update {
                    it.copy(recordingState = RecordingState.Error("Initialization failed: ${e.message}"))
                }
            }
        }
    }

    /**
     * Select a different ASR engine.
     */
    fun selectEngine(engineType: EngineType) {
        if (_uiState.value.recordingState == RecordingState.Recording) {
            Log.w(TAG, "Cannot change engine while recording")
            return
        }

        if (engineType == _uiState.value.selectedEngine) {
            return
        }

        _uiState.update { it.copy(selectedEngine = engineType) }
        prefs.edit().putString(PREF_ENGINE, engineType.name).apply()

        // Re-initialize engine
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (engineType.isStreaming) {
                    // Close offline components
                    vadProcessor?.close()
                    vadProcessor = null
                    asrEngine?.close()
                    asrEngine = null

                    initStreamingEngine(engineType)
                } else {
                    // Close streaming engine
                    streamingEngine?.close()
                    streamingEngine = null

                    // Initialize offline components if needed
                    if (vadProcessor == null) {
                        vadProcessor = VadProcessor(getApplication<Application>().assets)
                    }
                    initAsrEngine(engineType)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error switching engine", e)
                _uiState.update {
                    it.copy(recordingState = RecordingState.Error("Failed to switch engine: ${e.message}"))
                }
            }
        }
    }

    /**
     * Force a specific engine (for testing).
     */
    fun forceEngine(engineType: EngineType) {
        _uiState.update { it.copy(selectedEngine = engineType) }
        Log.i(TAG, "Forced ASR engine: ${engineType.displayName}")
    }

    /**
     * Toggle recording on/off.
     */
    @RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    fun toggleRecording() {
        val currentState = _uiState.value.recordingState

        when (currentState) {
            is RecordingState.Idle, is RecordingState.Error -> startRecording()
            is RecordingState.Recording -> stopRecording()
            is RecordingState.Initializing -> {
                Log.w(TAG, "Cannot toggle recording while initializing")
            }
        }
    }

    @RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    private fun startRecording() {
        val isStreaming = _uiState.value.selectedEngine.isStreaming

        // Validate that appropriate engine is initialized
        if (isStreaming) {
            if (streamingEngine == null) {
                _uiState.update {
                    it.copy(recordingState = RecordingState.Error("Streaming engine not initialized"))
                }
                return
            }
        } else {
            if (vadProcessor == null || asrEngine == null) {
                _uiState.update {
                    it.copy(recordingState = RecordingState.Error("Models not initialized"))
                }
                return
            }
        }

        _uiState.update {
            it.copy(
                recordingState = RecordingState.Recording,
                transcriptLines = emptyList(),
                audioLevel = 0f,
                sessionStats = SessionStats()
            )
        }

        // Reset state
        if (isStreaming) {
            currentStreamingText.clear()
            streamingSegmentCount = 0
        } else {
            vadProcessor?.reset()
        }

        recordingJob = viewModelScope.launch {
            var transcriptIndex = 0

            try {
                audioRecorder.audioFlow().collect { samples ->
                    // Update audio level
                    val level = calculateAudioLevel(samples)
                    _uiState.update { it.copy(audioLevel = level) }

                    if (isStreaming) {
                        processStreamingAudioSamples(samples) { line ->
                            val indexedLine = line.copy(index = transcriptIndex++)
                            _uiState.update { state ->
                                val newLines = state.transcriptLines + indexedLine
                                val newStats = updateSessionStats(state.sessionStats, indexedLine)
                                state.copy(
                                    transcriptLines = newLines,
                                    sessionStats = newStats
                                )
                            }
                        }
                    } else {
                        processAudioSamples(samples) { line ->
                            val indexedLine = line.copy(index = transcriptIndex++)
                            _uiState.update { state ->
                                val newLines = state.transcriptLines + indexedLine
                                val newStats = updateSessionStats(state.sessionStats, indexedLine)
                                state.copy(
                                    transcriptLines = newLines,
                                    sessionStats = newStats
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Recording error", e)
                _uiState.update {
                    it.copy(recordingState = RecordingState.Error("Recording error: ${e.message}"))
                }
            }
        }

        Log.i(TAG, "Recording started (streaming=$isStreaming)")
    }

    private fun stopRecording() {
        recordingJob?.cancel()
        recordingJob = null
        _uiState.update { it.copy(recordingState = RecordingState.Idle, audioLevel = 0f) }
        Log.i(TAG, "Recording stopped")
    }

    /**
     * Clear the current transcript.
     */
    fun clearTranscript() {
        _uiState.update {
            it.copy(
                transcriptLines = emptyList(),
                sessionStats = SessionStats()
            )
        }
    }

    /**
     * Get shareable transcript text.
     */
    fun getShareableTranscript(): String {
        val state = _uiState.value
        if (state.transcriptLines.isEmpty()) {
            return ""
        }

        val header = "ASR Transcript (${state.selectedEngine.displayName})\n" +
                "Generated: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}\n" +
                "Stats: ${state.sessionStats.formatted()}\n" +
                "---\n\n"

        val transcript = state.transcriptLines.joinToString("\n") { line ->
            "${line.index + 1}. ${line.text}"
        }

        return header + transcript
    }

    /**
     * Create a share intent for the transcript.
     */
    fun createShareIntent(): Intent? {
        val text = getShareableTranscript()
        if (text.isEmpty()) return null

        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "ASR Transcript")
            putExtra(Intent.EXTRA_TEXT, text)
        }
    }

    /**
     * Calculate normalized audio level (0.0 to 1.0) from samples.
     */
    private fun calculateAudioLevel(samples: FloatArray): Float {
        if (samples.isEmpty()) return 0f

        // Calculate RMS (Root Mean Square)
        var sumSquares = 0.0
        for (sample in samples) {
            sumSquares += sample * sample
        }
        val rms = sqrt(sumSquares / samples.size).toFloat()

        // Normalize to 0-1 range (typical speech is around 0.1-0.3 RMS)
        // Use log scale for better visual representation
        val minDb = -60f
        val maxDb = 0f
        val db = if (rms > 0) 20 * kotlin.math.log10(rms) else minDb
        val normalized = (db - minDb) / (maxDb - minDb)

        return normalized.coerceIn(0f, 1f)
    }

    /**
     * Update session statistics with a new transcript line.
     */
    private fun updateSessionStats(current: SessionStats, line: TranscriptLine): SessionStats {
        val newTotal = current.totalSegments + 1
        val newAudioDuration = current.totalAudioDurationMs + line.segmentDurationMs
        val newDecodeTime = current.totalDecodeTimeMs + line.decodeTimeMs
        val newAvgRtf = if (newAudioDuration > 0) newDecodeTime / newAudioDuration else 0.0

        return SessionStats(
            totalSegments = newTotal,
            totalAudioDurationMs = newAudioDuration,
            totalDecodeTimeMs = newDecodeTime,
            averageRtf = newAvgRtf
        )
    }

    private suspend fun processAudioSamples(
        samples: FloatArray,
        onTranscript: suspend (TranscriptLine) -> Unit
    ) {
        val vad = vadProcessor ?: return
        val engine = asrEngine ?: return

        vad.acceptWaveform(samples)

        while (vad.hasSegments()) {
            val segment = vad.popSegment()
            val segmentDurationMs = segment.samples.size * 1000.0 / sampleRateHz

            // Decode on IO dispatcher
            val (text, decodeTimeMs) = withContext(Dispatchers.IO) {
                val startNs = SystemClock.elapsedRealtimeNanos()
                val decodedText = engine.decode(segment.samples, sampleRateHz)
                val endNs = SystemClock.elapsedRealtimeNanos()
                decodedText to (endNs - startNs) / 1_000_000.0
            }

            if (text.isNotBlank()) {
                val rtf = if (segmentDurationMs > 0) decodeTimeMs / segmentDurationMs else 0.0

                onTranscript(
                    TranscriptLine(
                        index = 0, // Will be set by caller
                        text = text.trim().lowercase(),
                        engineName = engine.name,
                        segmentDurationMs = segmentDurationMs,
                        decodeTimeMs = decodeTimeMs,
                        rtf = rtf
                    )
                )
            }
        }
    }

    private fun initAsrEngine(type: EngineType) {
        try {
            asrEngine?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing previous engine", e)
        }
        asrEngine = null

        val app = getApplication<Application>()
        val voskCandidates = buildList {
            app.getExternalFilesDir(null)?.let { add(File(it, "vosk-model")) }
            add(File(app.filesDir, "vosk-model"))
        }

        asrEngine = AsrEngineFactory.create(
            type = type,
            assetManager = app.assets,
            sampleRateHz = sampleRateHz,
            voskModelCandidates = voskCandidates
        )

        if (asrEngine == null) {
            val errorMsg = AsrEngineFactory.getErrorMessage(type, voskCandidates)
            _uiState.update { it.copy(recordingState = RecordingState.Error(errorMsg)) }
        }
    }

    /**
     * Initialize a streaming ASR engine.
     */
    private fun initStreamingEngine(type: EngineType) {
        try {
            streamingEngine?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing previous streaming engine", e)
        }
        streamingEngine = null

        val app = getApplication<Application>()

        val modelType = when (type) {
            EngineType.STREAMING_EN -> StreamingModelType.ZIPFORMER_EN_SMALL
            EngineType.STREAMING_ZH_EN -> StreamingModelType.BILINGUAL_ZH_EN
            else -> {
                Log.e(TAG, "Unsupported streaming engine type: $type")
                return
            }
        }

        try {
            streamingEngine = SherpaStreamingEngine(app.assets, modelType)
            Log.i(TAG, "Streaming engine initialized: ${streamingEngine?.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize streaming engine", e)
            _uiState.update {
                it.copy(recordingState = RecordingState.Error("Streaming engine init failed: ${e.message}"))
            }
        }
    }

    /**
     * Process audio samples in streaming mode.
     */
    private suspend fun processStreamingAudioSamples(
        samples: FloatArray,
        onTranscript: suspend (TranscriptLine) -> Unit
    ) {
        val engine = streamingEngine ?: return

        withContext(Dispatchers.IO) {
            val startNs = SystemClock.elapsedRealtimeNanos()

            // Feed audio to streaming recognizer
            engine.acceptWaveform(samples, sampleRateHz)

            // Decode while ready
            while (engine.isReady()) {
                engine.decode()
            }

            // Get current result
            val result = engine.getResult()

            // Check for endpoint (end of utterance)
            if (result.isFinal && result.text.isNotBlank()) {
                val endNs = SystemClock.elapsedRealtimeNanos()
                val decodeTimeMs = (endNs - startNs) / 1_000_000.0

                // Calculate segment duration estimate (based on audio chunks processed)
                val segmentDurationMs = (currentStreamingText.length + result.text.length) * 50.0 // rough estimate

                onTranscript(
                    TranscriptLine(
                        index = 0,
                        text = result.text.trim().lowercase(),
                        engineName = engine.name,
                        segmentDurationMs = segmentDurationMs,
                        decodeTimeMs = decodeTimeMs,
                        rtf = if (segmentDurationMs > 0) decodeTimeMs / segmentDurationMs else 0.0
                    )
                )

                // Reset for next utterance
                engine.reset()
                currentStreamingText.clear()
                streamingSegmentCount++
            } else if (result.text.isNotBlank()) {
                // Update partial text (for potential UI display of partial results)
                currentStreamingText.clear()
                currentStreamingText.append(result.text)
            }
            // Return Unit explicitly
            Unit
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.i(TAG, "ViewModel cleared, releasing resources")

        recordingJob?.cancel()

        try {
            streamingEngine?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing streaming engine", e)
        }

        try {
            asrEngine?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing ASR engine", e)
        }

        try {
            vadProcessor?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing VAD processor", e)
        }
    }
}
