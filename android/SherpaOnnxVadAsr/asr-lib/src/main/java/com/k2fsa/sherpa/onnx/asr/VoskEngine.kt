package com.k2fsa.sherpa.onnx.asr

import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File

class VoskEngine(
    modelDir: File,
    private val sampleRateInHz: Int,
) : AsrEngine {

    private val model: Model
    private val recognizer: Recognizer

    override val name: String = "Vosk"

    init {
        require(modelDir.isDirectory) { "Vosk model dir does not exist: ${modelDir.absolutePath}" }

        model = Model(modelDir.absolutePath)
        recognizer = Recognizer(model, sampleRateInHz.toFloat())
        recognizer.setWords(false)
        recognizer.setPartialWords(false)
    }

    override fun decode(samples: FloatArray, sampleRate: Int): String {
        if (sampleRate != sampleRateInHz) {
            // This PoC assumes a fixed sample rate end-to-end.
            throw IllegalArgumentException("VoskEngine sampleRate mismatch: $sampleRate != $sampleRateInHz")
        }

        recognizer.acceptWaveForm(samples, samples.size)
        val json = recognizer.finalResult
        recognizer.reset()

        return try {
            JSONObject(json).optString("text", "")
        } catch (_: Exception) {
            ""
        }
    }

    override fun close() {
        try {
            recognizer.close()
        } finally {
            model.close()
        }
    }
}
