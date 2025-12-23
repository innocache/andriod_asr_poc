# ASR Library Integration Guide

This guide shows you how to integrate the ASR library into your existing Android application.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Installation](#installation)
3. [Basic Integration](#basic-integration)
4. [Advanced Usage](#advanced-usage)
5. [Building Native Libraries](#building-native-libraries)
6. [Troubleshooting](#troubleshooting)

## Prerequisites

- Android Studio (Arctic Fox or later)
- Android SDK API 21+
- Gradle 7.3.1 or later
- NDK (for building native libraries)

## Installation

### Option 1: Include as Module (Recommended)

1. **Copy the library module** to your project:
   ```bash
   cp -r path/to/andriod_asr_poc/android/SherpaOnnxVadAsr/asr-lib \
         your-project/
   ```

2. **Include the module** in your `settings.gradle`:
   ```gradle
   include ':app', ':asr-lib'
   ```

3. **Add dependency** in your app's `build.gradle`:
   ```gradle
   dependencies {
       implementation project(':asr-lib')
   }
   ```

4. **Sync Gradle** and rebuild your project.

### Option 2: Use as AAR

1. **Build the AAR**:
   ```bash
   cd path/to/andriod_asr_poc/android/SherpaOnnxVadAsr
   ./gradlew :asr-lib:assembleRelease
   ```

2. **Copy AAR** to your project's `libs` directory:
   ```bash
   mkdir -p your-project/app/libs
   cp asr-lib/build/outputs/aar/asr-lib-release.aar \
      your-project/app/libs/
   ```

3. **Add AAR dependency** in your app's `build.gradle`:
   ```gradle
   dependencies {
       implementation files('libs/asr-lib-release.aar')
       implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
       implementation 'com.alphacephei:vosk-android:0.3.47'
   }
   ```

## Basic Integration

### Step 1: Add Permissions

Add to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

### Step 2: Request Runtime Permission

```kotlin
import android.Manifest
import androidx.activity.result.contract.ActivityResultContracts
import com.k2fsa.sherpa.onnx.asr.AsrLibrary

class YourActivity : AppCompatActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            initializeAsr()
        } else {
            // Handle permission denied
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (AsrLibrary.hasRecordAudioPermission(this)) {
            initializeAsr()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}
```

### Step 3: Initialize ASR Engine

```kotlin
import com.k2fsa.sherpa.onnx.asr.AsrLibrary
import com.k2fsa.sherpa.onnx.asr.AsrEngine

private var asrEngine: AsrEngine? = null

private fun initializeAsr() {
    try {
        asrEngine = AsrLibrary.createOfflineEngine(
            context = applicationContext,
            config = AsrLibrary.OfflineConfig(
                engineType = AsrLibrary.EngineType.MOONSHINE
            )
        )
        Log.i(TAG, "ASR engine initialized successfully")
    } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize ASR engine", e)
    }
}

override fun onDestroy() {
    super.onDestroy()
    asrEngine?.close()
}
```

### Step 4: Record and Transcribe

```kotlin
import com.k2fsa.sherpa.onnx.asr.AsrLibrary
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

private val audioRecorder = AsrLibrary.createAudioRecorder()
private val vadProcessor = AsrLibrary.createVadProcessor(applicationContext)

private fun startRecording() {
    lifecycleScope.launch {
        audioRecorder.audioFlow().collect { audioChunk ->
            // Feed to VAD
            vadProcessor.acceptWaveform(audioChunk)
            
            // Process speech segments
            while (vadProcessor.hasSegments()) {
                val segment = vadProcessor.popSegment()
                val transcript = asrEngine?.decode(segment.samples, 16000)
                onTranscriptReady(transcript ?: "")
            }
        }
    }
}

private fun onTranscriptReady(text: String) {
    runOnUiThread {
        // Update UI with transcript
        textView.text = text
    }
}
```

## Advanced Usage

### Using Streaming ASR

For real-time transcription without VAD:

```kotlin
import com.k2fsa.sherpa.onnx.asr.AsrLibrary
import com.k2fsa.sherpa.onnx.asr.StreamingAsrEngine

private var streamingEngine: StreamingAsrEngine? = null

private fun initializeStreamingAsr() {
    streamingEngine = AsrLibrary.createStreamingEngine(
        context = applicationContext,
        config = AsrLibrary.StreamingConfig(
            modelType = AsrLibrary.StreamingModelType.STREAMING_EN,
            enableEndpoint = true
        )
    )
}

private fun processStreamingAudio() {
    lifecycleScope.launch {
        audioRecorder.audioFlow().collect { audioChunk ->
            streamingEngine?.acceptWaveform(audioChunk, 16000)
            
            if (streamingEngine?.isReady() == true) {
                streamingEngine?.decode()
                val result = streamingEngine?.getResult()
                
                // Update UI with partial result
                if (result != null && !result.isFinal) {
                    updatePartialTranscript(result.text)
                }
                
                // Handle endpoint (final result)
                if (streamingEngine?.isEndpoint() == true) {
                    onFinalTranscript(result?.text ?: "")
                    streamingEngine?.reset()
                }
            }
        }
    }
}

override fun onDestroy() {
    super.onDestroy()
    streamingEngine?.close()
}
```

### Custom Audio Processing

If you have your own audio source:

```kotlin
// Your custom audio source (e.g., from file, network, etc.)
val audioSamples: FloatArray = loadAudioFromFile()

// Normalize to [-1, 1] if needed
val normalizedSamples = audioSamples.map { it / 32768f }.toFloatArray()

// Transcribe
val transcript = asrEngine?.decode(normalizedSamples, sampleRate = 16000)
```

### Using Vosk Engine

For Vosk engine with external model:

```kotlin
// 1. Ensure model is on device (e.g., /sdcard/vosk-model-en-us-0.22/)

// 2. Create engine with model path
val voskEngine = AsrLibrary.createOfflineEngine(
    context = applicationContext,
    config = AsrLibrary.OfflineConfig(
        engineType = AsrLibrary.EngineType.VOSK,
        voskModelPath = "/sdcard/vosk-model-en-us-0.22/"
    )
)

// 3. Use same as Moonshine
val transcript = voskEngine.decode(audioSamples, 16000)
voskEngine.close()
```

## Building Native Libraries

The library requires native `.so` files from sherpa-onnx. To build them:

### Prerequisites

1. Install Android NDK:
   ```bash
   # Using sdkmanager
   sdkmanager --install "ndk;22.1.7171670"
   ```

2. Set up toolchain:
   ```bash
   cp scripts/toolchain.env.example scripts/toolchain.env
   # Edit toolchain.env with your NDK path
   ```

### Build Steps

1. **Bootstrap** (one-time):
   ```bash
   ./scripts/bootstrap_android_asr.sh
   ```

2. **Build native libraries and download models**:
   ```bash
   set -a; source scripts/toolchain.env; set +a
   ./scripts/build_sherpa_onnx_android.sh
   ```

3. **Copy to library**:
   The build script automatically copies:
   - Native `.so` files to `asr-lib/src/main/jniLibs/`
   - Model files to `asr-lib/src/main/assets/`

4. **Download streaming models** (optional):
   ```bash
   ./scripts/download_streaming_model.sh
   ```

### Generated Files

After building, you should have:

```
asr-lib/
├── src/main/
│   ├── assets/
│   │   ├── moonshine/               # Moonshine model files
│   │   ├── silero_vad.onnx         # VAD model
│   │   └── streaming-*/            # Streaming models (optional)
│   └── jniLibs/
│       ├── arm64-v8a/
│       │   └── libsherpa-onnx-jni.so
│       ├── armeabi-v7a/
│       │   └── libsherpa-onnx-jni.so
│       └── x86_64/
│           └── libsherpa-onnx-jni.so
```

## Troubleshooting

### "ASR engine failed to initialize"

**Cause**: Missing model files or native libraries.

**Solution**:
1. Ensure models are in `asr-lib/src/main/assets/`
2. Ensure `.so` files are in `asr-lib/src/main/jniLibs/`
3. Run build scripts to download/build missing files

### "UnsatisfiedLinkError: dlopen failed"

**Cause**: Missing or incompatible native library for device architecture.

**Solution**:
1. Rebuild native libraries for all architectures
2. Check `abiFilters` in `build.gradle` includes target architecture
3. Verify NDK version matches build requirements

### "RECORD_AUDIO permission denied"

**Cause**: App doesn't have microphone permission.

**Solution**:
1. Add permission to `AndroidManifest.xml`
2. Request at runtime before using audio recorder
3. Use `AsrLibrary.hasRecordAudioPermission()` to check

### Gradle build fails

**Cause**: Missing dependencies or incorrect Gradle configuration.

**Solution**:
1. Sync Gradle files
2. Check all dependencies are available
3. Update Gradle to compatible version (7.3.1+)
4. Verify `settings.gradle` includes `:asr-lib`

### Memory issues during transcription

**Cause**: Large audio files or insufficient memory.

**Solution**:
1. Process audio in chunks rather than entire file
2. Use streaming engine for real-time processing
3. Release engines when done: `engine.close()`
4. Consider using smaller model (Moonshine instead of Vosk)

## Best Practices

### Resource Management

Always close resources when done:

```kotlin
override fun onDestroy() {
    super.onDestroy()
    asrEngine?.close()
    vadProcessor?.close()
    streamingEngine?.close()
}
```

### Background Processing

Use coroutines for audio processing:

```kotlin
private val processingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

fun processAudio() {
    processingScope.launch {
        // Audio processing here
    }
}

override fun onDestroy() {
    super.onDestroy()
    processingScope.cancel()
}
```

### Error Handling

Wrap ASR operations in try-catch:

```kotlin
try {
    val transcript = asrEngine?.decode(samples, 16000)
    onSuccess(transcript ?: "")
} catch (e: Exception) {
    Log.e(TAG, "Transcription failed", e)
    onError(e.message ?: "Unknown error")
}
```

### Testing

Test on real devices with different:
- Android versions (API 21+)
- CPU architectures (arm64-v8a, armeabi-v7a, x86_64)
- Memory constraints

## Next Steps

- Explore the [API Reference](README.md#api-reference) for detailed API documentation
- Check the demo app in `app/` module for complete examples
- Read about [Model Options](README.md#models-setup) for different use cases

## Support

For issues or questions:
1. Check existing [GitHub Issues](https://github.com/innocache/andriod_asr_poc/issues)
2. Review the demo app implementation
3. Open a new issue with details about your problem
