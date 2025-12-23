# ASR Library (asr-lib)

A reusable Android library for Automatic Speech Recognition (ASR) that can be easily integrated into any Android application.

## Features

- **Offline ASR Engines**
  - **Moonshine**: Sherpa-ONNX Moonshine Tiny INT8 model (~50MB)
  - **Vosk**: Alternative engine using Vosk models (~1.8GB)
  
- **Streaming ASR Engines** (real-time transcription)
  - **Streaming EN**: English streaming model with endpoint detection
  - **Streaming ZH-EN**: Bilingual Chinese + English streaming

- **Voice Activity Detection**: Silero VAD for accurate speech segmentation
- **Audio Recording**: Built-in audio recorder with Kotlin Flow support
- **Clean API**: Simple, well-documented public API

## Installation

### Step 1: Add the library to your project

Add the library module as a dependency in your app's `build.gradle`:

```gradle
dependencies {
    implementation project(':asr-lib')
}
```

### Step 2: Include the library module

In your `settings.gradle`, include the library module:

```gradle
include ':asr-lib'
project(':asr-lib').projectDir = new File('/path/to/asr-lib')
```

### Step 3: Sync Gradle

Sync your project with Gradle files.

## Quick Start

### Basic Offline ASR (Moonshine)

```kotlin
import com.k2fsa.sherpa.onnx.asr.AsrLibrary
import com.k2fsa.sherpa.onnx.asr.AsrLibrary.OfflineConfig

// 1. Create ASR engine
val asrEngine = AsrLibrary.createOfflineEngine(
    context = context,
    config = OfflineConfig(engineType = AsrLibrary.EngineType.MOONSHINE)
)

// 2. Use for speech recognition
// audioSamples: FloatArray - normalized audio samples [-1, 1]
val transcript = asrEngine.decode(audioSamples, sampleRate = 16000)
println("Recognized: $transcript")

// 3. Clean up when done
asrEngine.close()
```

### Streaming ASR (Real-time)

```kotlin
import com.k2fsa.sherpa.onnx.asr.AsrLibrary
import com.k2fsa.sherpa.onnx.asr.AsrLibrary.StreamingConfig

// 1. Create streaming engine
val streamingEngine = AsrLibrary.createStreamingEngine(
    context = context,
    config = StreamingConfig(
        modelType = AsrLibrary.StreamingModelType.STREAMING_EN,
        enableEndpoint = true
    )
)

// 2. Process audio chunks in real-time
while (hasMoreAudio) {
    val audioChunk: FloatArray = getNextAudioChunk()
    streamingEngine.acceptWaveform(audioChunk, sampleRate = 16000)
    
    if (streamingEngine.isReady()) {
        streamingEngine.decode()
        val result = streamingEngine.getResult()
        println("Partial: ${result.text}")
        
        if (streamingEngine.isEndpoint()) {
            println("Final: ${result.text}")
            streamingEngine.reset()
        }
    }
}

// 3. Clean up
streamingEngine.close()
```

### VAD + Offline ASR Pipeline

```kotlin
import com.k2fsa.sherpa.onnx.asr.AsrLibrary
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

// 1. Create components
val vadProcessor = AsrLibrary.createVadProcessor(context)
val asrEngine = AsrLibrary.createOfflineEngine(context)
val audioRecorder = AsrLibrary.createAudioRecorder()

// 2. Check permission
if (!AsrLibrary.hasRecordAudioPermission(context)) {
    // Request RECORD_AUDIO permission
    return
}

// 3. Process audio stream
lifecycleScope.launch {
    audioRecorder.audioFlow().collect { audioChunk ->
        // Feed to VAD
        vadProcessor.acceptWaveform(audioChunk)
        
        // Check for speech segments
        while (vadProcessor.hasSegments()) {
            val segment = vadProcessor.popSegment()
            
            // Recognize speech segment
            val transcript = asrEngine.decode(segment.samples, sampleRate = 16000)
            println("Segment: $transcript")
        }
    }
}

// 4. Clean up
vadProcessor.close()
asrEngine.close()
```

### Audio Recording with Flow

```kotlin
import com.k2fsa.sherpa.onnx.asr.AsrLibrary
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

val audioRecorder = AsrLibrary.createAudioRecorder(
    sampleRateHz = 16000,
    bufferSizeSamples = 512
)

lifecycleScope.launch {
    audioRecorder.audioFlow().collect { samples ->
        // Process audio samples (FloatArray)
        // samples are normalized to [-1, 1]
        processSamples(samples)
    }
}
```

## API Reference

### AsrLibrary

Main entry point for the library.

#### Methods

- `createOfflineEngine(context, config)` - Create offline ASR engine
- `createStreamingEngine(context, config)` - Create streaming ASR engine
- `createVadProcessor(context, vadModelType)` - Create VAD processor
- `createAudioRecorder(sampleRateHz, bufferSizeSamples)` - Create audio recorder
- `hasRecordAudioPermission(context)` - Check if recording permission is granted

### AsrEngine

Interface for offline ASR engines.

#### Methods

- `decode(samples: FloatArray, sampleRate: Int): String` - Decode audio segment
- `close()` - Release resources

### StreamingAsrEngine

Interface for streaming ASR engines.

#### Methods

- `acceptWaveform(samples: FloatArray, sampleRate: Int)` - Feed audio
- `isReady(): Boolean` - Check if ready to decode
- `decode()` - Perform decoding
- `isEndpoint(): Boolean` - Check if endpoint detected
- `getResult(): StreamingResult` - Get transcription result
- `reset()` - Reset for new utterance
- `close()` - Release resources

### VadProcessor

Voice Activity Detection processor.

#### Methods

- `acceptWaveform(samples: FloatArray)` - Feed audio
- `hasSegments(): Boolean` - Check for speech segments
- `popSegment(): SpeechSegment` - Pop next segment
- `popAllSegments(): List<SpeechSegment>` - Pop all segments
- `reset()` - Reset VAD state
- `isSpeechDetected(): Boolean` - Check if speech is detected
- `close()` - Release resources

### AudioRecorder

Audio recording with Kotlin Flow.

#### Methods

- `audioFlow(): Flow<FloatArray>` - Get audio stream as Flow

## Configuration

### OfflineConfig

Configuration for offline ASR engines.

```kotlin
data class OfflineConfig(
    val engineType: EngineType = EngineType.MOONSHINE,
    val voskModelPath: String? = null  // Required for VOSK
)
```

### StreamingConfig

Configuration for streaming ASR engines.

```kotlin
data class StreamingConfig(
    val modelType: StreamingModelType = StreamingModelType.STREAMING_EN,
    val enableEndpoint: Boolean = true
)
```

## Required Permissions

Add to your app's `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

Request at runtime:

```kotlin
if (!AsrLibrary.hasRecordAudioPermission(context)) {
    ActivityCompat.requestPermissions(
        activity,
        arrayOf(Manifest.permission.RECORD_AUDIO),
        RECORD_AUDIO_REQUEST_CODE
    )
}
```

## Models Setup

### Bundled Models (Automatically included)

The following models are bundled in the library's assets:
- Moonshine Tiny INT8 (~50MB)
- Silero VAD (~632KB)
- Streaming models (if downloaded via build scripts)

These are automatically deployed with your APK.

### External Models (Manual setup)

For Vosk engine (~1.8GB model):

1. Download Vosk model using the provided scripts:
   ```bash
   ./scripts/vosk_model_en_full_download.sh
   ```

2. Push to device:
   ```bash
   ./scripts/vosk_model_push_device.sh
   ```

3. Use in your app:
   ```kotlin
   val config = OfflineConfig(
       engineType = AsrLibrary.EngineType.VOSK,
       voskModelPath = "/sdcard/vosk-model-en-us-0.22/"
   )
   val engine = AsrLibrary.createOfflineEngine(context, config)
   ```

## Building the Library

To build the library module:

```bash
cd android/SherpaOnnxVadAsr
./gradlew :asr-lib:assembleRelease
```

The AAR file will be generated at:
```
asr-lib/build/outputs/aar/asr-lib-release.aar
```

## ProGuard

The library includes consumer ProGuard rules. If you use ProGuard/R8 in your app, the necessary rules are automatically applied.

## Example App

See the `app` module in this project for a complete example implementation using the library.

## Architecture

The library follows clean architecture principles:

- **Public API Layer**: `AsrLibrary` facade provides simple API
- **Engine Layer**: `AsrEngine`, `StreamingAsrEngine` interfaces
- **Implementation Layer**: Sherpa-ONNX and Vosk engine implementations
- **JNI Layer**: Native bindings to sherpa-onnx C++ library

## Dependencies

- AndroidX Core KTX
- AndroidX AppCompat
- Kotlinx Coroutines
- Vosk Android (for Vosk engine support)
- Sherpa-ONNX native libraries (bundled)

## License

This library uses sherpa-onnx which is licensed under Apache 2.0.

## Acknowledgments

- [k2-fsa/sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) - Next-gen Kaldi ASR
- [alphacep/vosk-api](https://github.com/alphacep/vosk-api) - Offline speech recognition
- [snakers4/silero-vad](https://github.com/snakers4/silero-vad) - Voice activity detection

## Support

For issues, questions, or contributions, please refer to the main project repository.
