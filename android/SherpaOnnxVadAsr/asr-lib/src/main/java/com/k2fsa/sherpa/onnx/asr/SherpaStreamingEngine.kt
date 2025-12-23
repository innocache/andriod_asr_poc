package com.k2fsa.sherpa.onnx.asr

import android.content.res.AssetManager
import android.util.Log
import com.k2fsa.sherpa.onnx.*

/**
 * Streaming ASR engine using sherpa-onnx OnlineRecognizer.
 * Provides real-time transcription with endpoint detection.
 */
class SherpaStreamingEngine(
    assetManager: AssetManager,
    private val modelType: StreamingModelType = StreamingModelType.ZIPFORMER_EN_SMALL
) : StreamingAsrEngine {

    override val name: String = "Sherpa Streaming (${modelType.displayName})"

    private var recognizer: OnlineRecognizer? = null
    private var stream: OnlineStream? = null

    init {
        val config = createConfig(modelType)
        recognizer = OnlineRecognizer(assetManager, config)
        stream = recognizer?.createStream()
        Log.i(TAG, "Initialized $name")
    }

    override fun acceptWaveform(samples: FloatArray, sampleRate: Int) {
        stream?.acceptWaveform(samples, sampleRate)
    }

    override fun inputFinished() {
        stream?.inputFinished()
    }

    override fun isReady(): Boolean {
        val r = recognizer ?: return false
        val s = stream ?: return false
        return r.isReady(s)
    }

    override fun decode() {
        val r = recognizer ?: return
        val s = stream ?: return
        if (r.isReady(s)) {
            r.decode(s)
        }
    }

    override fun isEndpoint(): Boolean {
        val r = recognizer ?: return false
        val s = stream ?: return false
        return r.isEndpoint(s)
    }

    override fun reset() {
        val r = recognizer ?: return
        val s = stream ?: return
        r.reset(s)
    }

    override fun getResult(): StreamingResult {
        val r = recognizer ?: return StreamingResult("", false)
        val s = stream ?: return StreamingResult("", false)

        val result = r.getResult(s)
        val isFinal = isEndpoint()

        return StreamingResult(
            text = result.text.trim(),
            isFinal = isFinal,
            timestamps = result.timestamps.toList()
        )
    }

    override fun close() {
        Log.i(TAG, "Closing $name")
        stream?.release()
        stream = null
        recognizer?.release()
        recognizer = null
    }

    companion object {
        private const val TAG = "SherpaStreamingEngine"

        private fun createConfig(modelType: StreamingModelType): OnlineRecognizerConfig {
            val modelConfig = when (modelType) {
                StreamingModelType.ZIPFORMER_EN_SMALL -> {
                    val modelDir = "sherpa-onnx-streaming-zipformer-en-20M-2023-02-17"
                    OnlineModelConfig(
                        transducer = OnlineTransducerModelConfig(
                            encoder = "$modelDir/encoder-epoch-99-avg-1.int8.onnx",
                            decoder = "$modelDir/decoder-epoch-99-avg-1.onnx",
                            joiner = "$modelDir/joiner-epoch-99-avg-1.int8.onnx",
                        ),
                        tokens = "$modelDir/tokens.txt",
                        modelType = "zipformer",
                        numThreads = 2,
                    )
                }
                StreamingModelType.ZIPFORMER_EN_2023_06_26 -> {
                    val modelDir = "sherpa-onnx-streaming-zipformer-en-2023-06-26"
                    OnlineModelConfig(
                        transducer = OnlineTransducerModelConfig(
                            encoder = "$modelDir/encoder-epoch-99-avg-1-chunk-16-left-128.int8.onnx",
                            decoder = "$modelDir/decoder-epoch-99-avg-1-chunk-16-left-128.onnx",
                            joiner = "$modelDir/joiner-epoch-99-avg-1-chunk-16-left-128.onnx",
                        ),
                        tokens = "$modelDir/tokens.txt",
                        modelType = "zipformer2",
                        numThreads = 2,
                    )
                }
                StreamingModelType.BILINGUAL_ZH_EN -> {
                    val modelDir = "sherpa-onnx-streaming-zipformer-bilingual-zh-en-2023-02-20"
                    OnlineModelConfig(
                        transducer = OnlineTransducerModelConfig(
                            encoder = "$modelDir/encoder-epoch-99-avg-1.int8.onnx",
                            decoder = "$modelDir/decoder-epoch-99-avg-1.onnx",
                            joiner = "$modelDir/joiner-epoch-99-avg-1.int8.onnx",
                        ),
                        tokens = "$modelDir/tokens.txt",
                        modelType = "zipformer",
                        numThreads = 2,
                    )
                }
                StreamingModelType.PARAFORMER_BILINGUAL -> {
                    val modelDir = "sherpa-onnx-streaming-paraformer-bilingual-zh-en"
                    OnlineModelConfig(
                        paraformer = OnlineParaformerModelConfig(
                            encoder = "$modelDir/encoder.int8.onnx",
                            decoder = "$modelDir/decoder.int8.onnx",
                        ),
                        tokens = "$modelDir/tokens.txt",
                        modelType = "paraformer",
                        numThreads = 2,
                    )
                }
            }

            return OnlineRecognizerConfig(
                modelConfig = modelConfig,
                enableEndpoint = true,
                endpointConfig = EndpointConfig(
                    // Rule 1: Must not contain speech, min trailing silence 2.4s
                    rule1 = EndpointRule(false, 2.4f, 0.0f),
                    // Rule 2: Must contain speech, min trailing silence 1.2s (reduced for faster response)
                    rule2 = EndpointRule(true, 1.2f, 0.0f),
                    // Rule 3: Max utterance length 15s
                    rule3 = EndpointRule(false, 0.0f, 15.0f)
                ),
                decodingMethod = "greedy_search",
            )
        }
    }
}

/**
 * Available streaming model types.
 */
enum class StreamingModelType(val displayName: String, val modelDir: String) {
    /** Small English model (~20M parameters) - fast and lightweight */
    ZIPFORMER_EN_SMALL("English Small", "sherpa-onnx-streaming-zipformer-en-20M-2023-02-17"),

    /** Better quality English model (2023-06-26) */
    ZIPFORMER_EN_2023_06_26("English 2023-06-26", "sherpa-onnx-streaming-zipformer-en-2023-06-26"),

    /** Bilingual Chinese + English */
    BILINGUAL_ZH_EN("Bilingual ZH-EN", "sherpa-onnx-streaming-zipformer-bilingual-zh-en-2023-02-20"),

    /** Paraformer bilingual - different architecture */
    PARAFORMER_BILINGUAL("Paraformer Bilingual", "sherpa-onnx-streaming-paraformer-bilingual-zh-en");

    companion object {
        fun fromOrdinal(ordinal: Int): StreamingModelType =
            values().getOrElse(ordinal) { ZIPFORMER_EN_SMALL }
    }
}
