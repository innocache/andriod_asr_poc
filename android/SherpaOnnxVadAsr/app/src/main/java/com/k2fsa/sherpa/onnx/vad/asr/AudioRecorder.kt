package com.k2fsa.sherpa.onnx.vad.asr

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

private const val TAG = "AudioRecorder"

/**
 * Encapsulates AudioRecord lifecycle and emits PCM samples as a Flow.
 */
class AudioRecorder(
    private val sampleRateHz: Int = 16000,
    private val bufferSizeSamples: Int = 512,
) {
    private val audioSource = MediaRecorder.AudioSource.MIC
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    /**
     * Returns a Flow that emits FloatArray audio samples when collected.
     * The Flow completes when the collector is cancelled.
     *
     * @throws SecurityException if RECORD_AUDIO permission is not granted.
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun audioFlow(): Flow<FloatArray> = callbackFlow {
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRateHz, channelConfig, audioFormat)
        Log.i(TAG, "Min buffer size: $minBufferSize bytes (${minBufferSize * 1000.0f / sampleRateHz}ms)")

        val audioRecord = AudioRecord(
            audioSource,
            sampleRateHz,
            channelConfig,
            audioFormat,
            minBufferSize * 2 // Double buffer for safety
        )

        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            close(IllegalStateException("Failed to initialize AudioRecord"))
            return@callbackFlow
        }

        val buffer = ShortArray(bufferSizeSamples)

        try {
            audioRecord.startRecording()
            Log.i(TAG, "Started recording")

            while (isActive) {
                val samplesRead = audioRecord.read(buffer, 0, buffer.size)
                if (samplesRead > 0) {
                    val floatSamples = AudioPcm.pcm16ToFloat(buffer, samplesRead)
                    send(floatSamples)
                } else if (samplesRead < 0) {
                    Log.e(TAG, "AudioRecord.read() returned error: $samplesRead")
                    break
                }
            }
        } finally {
            Log.i(TAG, "Stopping recording")
            audioRecord.stop()
            audioRecord.release()
        }

        awaitClose {
            Log.i(TAG, "Audio flow closed")
        }
    }.flowOn(Dispatchers.IO)

    companion object {
        fun hasPermission(context: android.content.Context): Boolean {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}
