package com.k2fsa.sherpa.onnx.asr.samples

import android.Manifest
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.k2fsa.sherpa.onnx.asr.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Sample integration code showing how to use the ASR library in your Android app.
 * This is a reference implementation - copy and adapt to your needs.
 */

// ============================================================================
// SAMPLE 1: Simple Offline ASR (Record → VAD → Transcribe)
// ============================================================================

class OfflineAsrSample(private val activity: ComponentActivity) {
    
    private var asrEngine: AsrEngine? = null
    private var vadProcessor: VadProcessor? = null
    private val audioRecorder = AsrLibrary.createAudioRecorder()
    
    // Permission launcher
    private val requestPermission = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            initialize()
        }
    }
    
    fun start() {
        if (AsrLibrary.hasRecordAudioPermission(activity)) {
            initialize()
        } else {
            requestPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    
    private fun initialize() {
        // Initialize VAD and ASR
        vadProcessor = AsrLibrary.createVadProcessor(activity.applicationContext)
        asrEngine = AsrLibrary.createOfflineEngine(
            context = activity.applicationContext,
            config = AsrLibrary.OfflineConfig(
                engineType = AsrLibrary.EngineType.MOONSHINE
            )
        )
        
        // Start recording
        startRecording()
    }
    
    private fun startRecording() {
        activity.lifecycleScope.launch {
            audioRecorder.audioFlow().collect { audioChunk ->
                // Feed to VAD
                vadProcessor?.acceptWaveform(audioChunk)
                
                // Process speech segments
                while (vadProcessor?.hasSegments() == true) {
                    val segment = vadProcessor?.popSegment()
                    if (segment != null) {
                        val transcript = asrEngine?.decode(segment.samples, 16000)
                        onTranscriptReady(transcript ?: "")
                    }
                }
            }
        }
    }
    
    private fun onTranscriptReady(text: String) {
        // Update UI or handle transcript
        println("Transcript: $text")
    }
    
    fun cleanup() {
        asrEngine?.close()
        vadProcessor?.close()
    }
}

// ============================================================================
// SAMPLE 2: Streaming ASR (Real-time transcription)
// ============================================================================

class StreamingAsrSample(private val activity: ComponentActivity) {
    
    private var streamingEngine: StreamingAsrEngine? = null
    private val audioRecorder = AsrLibrary.createAudioRecorder()
    
    private val requestPermission = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            initialize()
        }
    }
    
    fun start() {
        if (AsrLibrary.hasRecordAudioPermission(activity)) {
            initialize()
        } else {
            requestPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    
    private fun initialize() {
        // Initialize streaming engine
        streamingEngine = AsrLibrary.createStreamingEngine(
            context = activity.applicationContext,
            config = AsrLibrary.StreamingConfig(
                modelType = AsrLibrary.StreamingModelType.STREAMING_EN,
                enableEndpoint = true
            )
        )
        
        startStreaming()
    }
    
    private fun startStreaming() {
        activity.lifecycleScope.launch {
            audioRecorder.audioFlow().collect { audioChunk ->
                streamingEngine?.acceptWaveform(audioChunk, 16000)
                
                if (streamingEngine?.isReady() == true) {
                    streamingEngine?.decode()
                    val result = streamingEngine?.getResult()
                    
                    if (result != null) {
                        // Update UI with partial results
                        onPartialResult(result.text)
                        
                        // Check for endpoint (final result)
                        if (streamingEngine?.isEndpoint() == true) {
                            onFinalResult(result.text)
                            streamingEngine?.reset()
                        }
                    }
                }
            }
        }
    }
    
    private fun onPartialResult(text: String) {
        // Update UI with partial transcript
        println("Partial: $text")
    }
    
    private fun onFinalResult(text: String) {
        // Handle final transcript
        println("Final: $text")
    }
    
    fun cleanup() {
        streamingEngine?.close()
    }
}

// ============================================================================
// SAMPLE 3: Batch Processing (Process audio file or buffer)
// ============================================================================

class BatchProcessingSample(private val context: Context) {
    
    fun transcribeAudio(audioSamples: FloatArray, sampleRate: Int): String {
        val asrEngine = AsrLibrary.createOfflineEngine(
            context = context,
            config = AsrLibrary.OfflineConfig(
                engineType = AsrLibrary.EngineType.MOONSHINE
            )
        )
        
        return try {
            asrEngine.decode(audioSamples, sampleRate)
        } finally {
            asrEngine.close()
        }
    }
    
    fun transcribeWithVad(audioSamples: FloatArray): List<String> {
        val vadProcessor = AsrLibrary.createVadProcessor(context)
        val asrEngine = AsrLibrary.createOfflineEngine(context)
        
        return try {
            // Feed all audio to VAD
            vadProcessor.acceptWaveform(audioSamples)
            
            // Process all segments
            val transcripts = mutableListOf<String>()
            while (vadProcessor.hasSegments()) {
                val segment = vadProcessor.popSegment()
                val transcript = asrEngine.decode(segment.samples, 16000)
                transcripts.add(transcript)
            }
            
            transcripts
        } finally {
            vadProcessor.close()
            asrEngine.close()
        }
    }
}

// ============================================================================
// SAMPLE 4: Using Vosk Engine
// ============================================================================

class VoskAsrSample(private val context: Context) {
    
    fun transcribeWithVosk(
        audioSamples: FloatArray,
        voskModelPath: String
    ): String {
        val asrEngine = AsrLibrary.createOfflineEngine(
            context = context,
            config = AsrLibrary.OfflineConfig(
                engineType = AsrLibrary.EngineType.VOSK,
                voskModelPath = voskModelPath
            )
        )
        
        return try {
            asrEngine.decode(audioSamples, sampleRate = 16000)
        } finally {
            asrEngine.close()
        }
    }
}

// ============================================================================
// SAMPLE 5: Custom Audio Processing
// ============================================================================

class CustomAudioSample(private val context: Context) {
    
    /**
     * Process audio from a custom source (e.g., file, network, etc.)
     */
    fun processCustomAudio(audioData: ByteArray): String {
        // Convert PCM16 to Float normalized to [-1, 1]
        val floatSamples = FloatArray(audioData.size / 2)
        for (i in floatSamples.indices) {
            val sample = (audioData[i * 2].toInt() and 0xFF) or 
                        (audioData[i * 2 + 1].toInt() shl 8)
            floatSamples[i] = sample / 32768.0f
        }
        
        // Transcribe
        val asrEngine = AsrLibrary.createOfflineEngine(context)
        return try {
            asrEngine.decode(floatSamples, sampleRate = 16000)
        } finally {
            asrEngine.close()
        }
    }
}

// ============================================================================
// SAMPLE 6: Error Handling Best Practices
// ============================================================================

class ErrorHandlingSample(private val context: Context) {
    
    fun robustTranscription(audioSamples: FloatArray): Result<String> {
        return try {
            // Check if audio has content
            if (audioSamples.isEmpty()) {
                return Result.failure(IllegalArgumentException("Empty audio"))
            }
            
            // Initialize engine
            val asrEngine = AsrLibrary.createOfflineEngine(context)
                ?: return Result.failure(IllegalStateException("Failed to create ASR engine"))
            
            // Transcribe
            val transcript = try {
                asrEngine.decode(audioSamples, 16000)
            } finally {
                asrEngine.close()
            }
            
            // Validate result
            if (transcript.isBlank()) {
                Result.failure(IllegalStateException("Empty transcript"))
            } else {
                Result.success(transcript)
            }
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
