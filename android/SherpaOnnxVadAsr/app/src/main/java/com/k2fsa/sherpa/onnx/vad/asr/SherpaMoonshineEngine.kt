package com.k2fsa.sherpa.onnx.vad.asr

import android.content.res.AssetManager
import android.util.Log
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.getFeatureConfig
import com.k2fsa.sherpa.onnx.getOfflineModelConfig

private const val TAG = "SherpaMoonshineEngine"

class SherpaMoonshineEngine(
    assetManager: AssetManager,
    sampleRateInHz: Int,
) : AsrEngine {

    // 21 - sherpa-onnx-moonshine-tiny-en-int8
    private val asrModelType = 21

    private var offlineRecognizer: OfflineRecognizer?

    override val name: String = "Moonshine"

    init {
        val config = OfflineRecognizerConfig(
            featConfig = getFeatureConfig(sampleRate = sampleRateInHz, featureDim = 80),
            modelConfig = getOfflineModelConfig(type = asrModelType)!!,
        )

        offlineRecognizer = OfflineRecognizer(
            assetManager = assetManager,
            config = config,
        )
        Log.i(TAG, "Initialized Moonshine engine")
    }

    override fun decode(samples: FloatArray, sampleRate: Int): String {
        val recognizer = offlineRecognizer
            ?: throw IllegalStateException("Engine has been closed")

        val stream = recognizer.createStream()
        return try {
            stream.acceptWaveform(samples, sampleRate)
            recognizer.decode(stream)
            recognizer.getResult(stream).text
        } finally {
            stream.release()
        }
    }

    override fun close() {
        try {
            offlineRecognizer?.release()
            Log.i(TAG, "Released Moonshine engine")
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing recognizer", e)
        } finally {
            offlineRecognizer = null
        }
    }
}
