# Quick Start Guide

Get started with the ASR Library in 5 minutes!

## Prerequisites

- Android Studio
- Android device or emulator (API 21+)
- `RECORD_AUDIO` permission

## Installation

### Add to your project

In `settings.gradle`:
```gradle
include ':app', ':asr-lib'
```

In `app/build.gradle`:
```gradle
dependencies {
    implementation project(':asr-lib')
}
```

## Hello ASR - Minimal Example

```kotlin
import com.k2fsa.sherpa.onnx.asr.AsrLibrary

class MainActivity : AppCompatActivity() {
    
    private lateinit var asrEngine: AsrEngine
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Create ASR engine
        asrEngine = AsrLibrary.createOfflineEngine(this)
        
        // 2. Transcribe audio (you provide the audio samples)
        val audioSamples: FloatArray = getAudioSamples() // Your audio data
        val transcript = asrEngine.decode(audioSamples, sampleRate = 16000)
        
        // 3. Use the transcript
        println("Recognized: $transcript")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        asrEngine.close()
    }
}
```

## Complete Example - Record & Transcribe

```kotlin
import android.Manifest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.k2fsa.sherpa.onnx.asr.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class AsrActivity : AppCompatActivity() {
    
    private lateinit var asrEngine: AsrEngine
    private lateinit var vadProcessor: VadProcessor
    private val audioRecorder = AsrLibrary.createAudioRecorder()
    
    // Permission launcher
    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startRecording()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize engines
        asrEngine = AsrLibrary.createOfflineEngine(this)
        vadProcessor = AsrLibrary.createVadProcessor(this)
        
        // Request permission and start
        if (AsrLibrary.hasRecordAudioPermission(this)) {
            startRecording()
        } else {
            requestPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    
    private fun startRecording() {
        lifecycleScope.launch {
            audioRecorder.audioFlow().collect { audioChunk ->
                // Feed to VAD
                vadProcessor.acceptWaveform(audioChunk)
                
                // Process speech segments
                while (vadProcessor.hasSegments()) {
                    val segment = vadProcessor.popSegment()
                    val transcript = asrEngine.decode(segment.samples, 16000)
                    
                    // Display transcript
                    runOnUiThread {
                        showTranscript(transcript)
                    }
                }
            }
        }
    }
    
    private fun showTranscript(text: String) {
        // Update your UI
        println("Transcript: $text")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        asrEngine.close()
        vadProcessor.close()
    }
}
```

## Streaming Example - Real-time

```kotlin
import com.k2fsa.sherpa.onnx.asr.*

class StreamingAsrActivity : AppCompatActivity() {
    
    private lateinit var streamingEngine: StreamingAsrEngine
    private val audioRecorder = AsrLibrary.createAudioRecorder()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create streaming engine
        streamingEngine = AsrLibrary.createStreamingEngine(this)
        
        // Start streaming
        lifecycleScope.launch {
            audioRecorder.audioFlow().collect { audioChunk ->
                streamingEngine.acceptWaveform(audioChunk, 16000)
                
                if (streamingEngine.isReady()) {
                    streamingEngine.decode()
                    val result = streamingEngine.getResult()
                    
                    // Show partial results
                    showPartialResult(result.text)
                    
                    // Handle final results
                    if (streamingEngine.isEndpoint()) {
                        showFinalResult(result.text)
                        streamingEngine.reset()
                    }
                }
            }
        }
    }
    
    private fun showPartialResult(text: String) {
        // Update UI with partial transcript (gray color)
        println("Partial: $text")
    }
    
    private fun showFinalResult(text: String) {
        // Update UI with final transcript (black color)
        println("Final: $text")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        streamingEngine.close()
    }
}
```

## Don't Forget!

### 1. Add Permission to Manifest

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

### 2. Build Native Libraries

Before using the library, build native libraries:

```bash
cd path/to/project
./scripts/bootstrap_android_asr.sh
./scripts/build_sherpa_onnx_android.sh
```

### 3. Clean Up Resources

Always close engines in `onDestroy()`:

```kotlin
override fun onDestroy() {
    super.onDestroy()
    asrEngine?.close()
    vadProcessor?.close()
    streamingEngine?.close()
}
```

## Next Steps

- 📖 Read the [Full Documentation](README.md)
- 🔧 Check the [Integration Guide](INTEGRATION_GUIDE.md)
- 💡 Explore [Sample Code](SAMPLES.kt)
- 🐛 [Troubleshooting](INTEGRATION_GUIDE.md#troubleshooting)

## Common Issues

### "Engine failed to initialize"
→ Build native libraries and models first

### "Permission denied"
→ Request `RECORD_AUDIO` permission at runtime

### "No such file or directory"
→ Ensure models are in `asr-lib/src/main/assets/`

## API Cheat Sheet

```kotlin
// Create engines
AsrLibrary.createOfflineEngine(context)
AsrLibrary.createStreamingEngine(context)
AsrLibrary.createVadProcessor(context)
AsrLibrary.createAudioRecorder()

// Offline ASR
asrEngine.decode(samples, 16000)

// Streaming ASR
streamingEngine.acceptWaveform(samples, 16000)
streamingEngine.isReady()
streamingEngine.decode()
streamingEngine.getResult()
streamingEngine.isEndpoint()
streamingEngine.reset()

// VAD
vadProcessor.acceptWaveform(samples)
vadProcessor.hasSegments()
vadProcessor.popSegment()

// Audio Recording
audioRecorder.audioFlow().collect { samples ->
    // Process samples
}

// Cleanup
engine.close()
```

---

Happy coding! 🎤🎯
