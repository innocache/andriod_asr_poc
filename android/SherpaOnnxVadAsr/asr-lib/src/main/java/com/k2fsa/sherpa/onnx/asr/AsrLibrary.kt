package com.k2fsa.sherpa.onnx.asr

import android.content.Context
import android.content.res.AssetManager

/**
 * Main entry point for the ASR Library.
 * Provides a simple API for initializing and using Automatic Speech Recognition features.
 * 
 * Example usage:
 * ```kotlin
 * // Create ASR engine
 * val asrEngine = AsrLibrary.createOfflineEngine(
 *     context = context,
 *     engineType = AsrLibrary.EngineType.MOONSHINE
 * )
 * 
 * // Use for speech recognition
 * val transcript = asrEngine.decode(audioSamples, sampleRate = 16000)
 * 
 * // Clean up when done
 * asrEngine.close()
 * ```
 */
object AsrLibrary {
    
    /**
     * Supported offline ASR engine types.
     */
    enum class EngineType {
        /** Sherpa-ONNX Moonshine model (default, ~50MB) */
        MOONSHINE,
        
        /** Vosk model (requires separate model download, ~1.8GB) */
        VOSK
    }
    
    /**
     * Supported streaming ASR model types.
     */
    enum class StreamingModelType {
        /** English streaming model with endpoint detection */
        STREAMING_EN,
        
        /** Bilingual Chinese + English streaming model */
        STREAMING_ZH_EN
    }
    
    /**
     * Configuration for offline ASR engine.
     */
    data class OfflineConfig(
        val engineType: EngineType = EngineType.MOONSHINE,
        val voskModelPath: String? = null  // Required only for VOSK engine
    )
    
    /**
     * Configuration for streaming ASR engine.
     */
    data class StreamingConfig(
        val modelType: StreamingModelType = StreamingModelType.STREAMING_EN,
        val enableEndpoint: Boolean = true
    )
    
    /**
     * Create an offline ASR engine for batch processing.
     * 
     * @param context Android context
     * @param config Offline engine configuration
     * @return Configured offline ASR engine
     * @throws IllegalArgumentException if configuration is invalid
     */
    @JvmStatic
    @JvmOverloads
    fun createOfflineEngine(
        context: Context,
        config: OfflineConfig = OfflineConfig()
    ): AsrEngine {
        return when (config.engineType) {
            EngineType.MOONSHINE -> AsrEngineFactory.createMoonshineEngine(
                assetManager = context.assets
            )
            EngineType.VOSK -> {
                val modelPath = config.voskModelPath
                    ?: throw IllegalArgumentException("voskModelPath is required for VOSK engine")
                AsrEngineFactory.createVoskEngine(modelPath)
            }
        }
    }
    
    /**
     * Create a streaming ASR engine for real-time transcription.
     * 
     * @param context Android context
     * @param config Streaming engine configuration
     * @return Configured streaming ASR engine
     */
    @JvmStatic
    @JvmOverloads
    fun createStreamingEngine(
        context: Context,
        config: StreamingConfig = StreamingConfig()
    ): StreamingAsrEngine {
        return AsrEngineFactory.createStreamingEngine(
            assetManager = context.assets,
            modelType = config.modelType.ordinal
        )
    }
    
    /**
     * Create a VAD (Voice Activity Detection) processor.
     * 
     * @param context Android context
     * @param vadModelType VAD model type (0 = Silero, 1 = Ten)
     * @return Configured VAD processor
     */
    @JvmStatic
    @JvmOverloads
    fun createVadProcessor(
        context: Context,
        vadModelType: Int = 0
    ): VadProcessor {
        return VadProcessor(context.assets, vadModelType)
    }
    
    /**
     * Create an audio recorder for capturing microphone input.
     * 
     * @param sampleRateHz Sample rate in Hz (default: 16000)
     * @param bufferSizeSamples Buffer size in samples (default: 512)
     * @return Configured audio recorder
     */
    @JvmStatic
    @JvmOverloads
    fun createAudioRecorder(
        sampleRateHz: Int = 16000,
        bufferSizeSamples: Int = 512
    ): AudioRecorder {
        return AudioRecorder(sampleRateHz, bufferSizeSamples)
    }
    
    /**
     * Check if audio recording permission is granted.
     * 
     * @param context Android context
     * @return true if permission is granted, false otherwise
     */
    @JvmStatic
    fun hasRecordAudioPermission(context: Context): Boolean {
        return AudioRecorder.hasPermission(context)
    }
}
