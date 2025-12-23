package com.k2fsa.sherpa.onnx.asr

import android.content.res.AssetManager
import android.util.Log
import java.io.File

private const val TAG = "AsrEngineFactory"

/**
 * Factory for creating ASR engine instances.
 */
object AsrEngineFactory {

    /**
     * Create an ASR engine of the specified type.
     * Note: Streaming engines should be created via SherpaStreamingEngine directly,
     * not through this factory.
     *
     * @param type The engine type to create.
     * @param assetManager Asset manager for loading models from assets.
     * @param sampleRateHz Sample rate for audio processing.
     * @param voskModelCandidates List of candidate directories for Vosk model.
     * @return The created engine, or null if creation failed.
     */
    fun create(
        type: EngineType,
        assetManager: AssetManager,
        sampleRateHz: Int,
        voskModelCandidates: List<File> = emptyList(),
    ): AsrEngine? {
        return try {
            when (type) {
                EngineType.MOONSHINE -> {
                    Log.i(TAG, "Creating Moonshine (sherpa-onnx) engine")
                    SherpaMoonshineEngine(assetManager, sampleRateHz)
                }

                EngineType.VOSK -> {
                    val modelDir = voskModelCandidates.firstOrNull { it.isDirectory }
                    if (modelDir == null) {
                        val checked = voskModelCandidates.joinToString("\n") { it.absolutePath }
                        throw IllegalStateException("Vosk model dir not found. Checked:\n$checked")
                    }
                    Log.i(TAG, "Creating Vosk engine with model: ${modelDir.absolutePath}")
                    VoskEngine(modelDir, sampleRateHz)
                }

                // Streaming engines are not created through this factory
                EngineType.STREAMING_EN,
                EngineType.STREAMING_ZH_EN -> {
                    Log.w(TAG, "Streaming engines should be created via SherpaStreamingEngine")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create ${type.displayName} engine", e)
            null
        }
    }

    /**
     * Get a human-readable error message for engine creation failure.
     */
    fun getErrorMessage(type: EngineType, voskModelCandidates: List<File>): String {
        return when (type) {
            EngineType.MOONSHINE -> "Failed to initialize Moonshine ASR engine"
            EngineType.VOSK -> {
                val checked = voskModelCandidates.joinToString("\n") { it.absolutePath }
                "Vosk model not found. Checked:\n$checked\n\nProvision it with scripts/vosk_model_push_device.sh"
            }
            EngineType.STREAMING_EN,
            EngineType.STREAMING_ZH_EN -> {
                "Streaming model not found. Download the model and add to assets."
            }
        }
    }

    /**
     * Create a Moonshine offline ASR engine.
     */
    fun createMoonshineEngine(
        assetManager: AssetManager,
        sampleRateHz: Int = 16000
    ): AsrEngine {
        return SherpaMoonshineEngine(assetManager, sampleRateHz)
    }

    /**
     * Create a Vosk offline ASR engine.
     */
    fun createVoskEngine(
        modelPath: String,
        sampleRateHz: Int = 16000
    ): AsrEngine {
        val modelDir = File(modelPath)
        if (!modelDir.isDirectory) {
            throw IllegalArgumentException("Vosk model directory not found: $modelPath")
        }
        return VoskEngine(modelDir, sampleRateHz)
    }

    /**
     * Create a streaming ASR engine.
     */
    fun createStreamingEngine(
        assetManager: AssetManager,
        modelType: Int = 0  // 0 = STREAMING_EN, 1 = STREAMING_ZH_EN
    ): StreamingAsrEngine {
        return SherpaStreamingEngine(assetManager, modelType)
    }
}
