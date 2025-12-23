# Android ASR Proof of Concept

An Android speech recognition application supporting both **offline batch processing** (VAD + ASR) and **real-time streaming transcription** using [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx).

## 🎯 Key Components

This project consists of:

1. **`asr-lib`** - Reusable Android library for ASR functionality
2. **`app`** - Demo application showcasing the library

## Features

- **Offline ASR Engines**
  - **Moonshine**: sherpa-onnx Moonshine Tiny INT8 model (~50MB)
  - **Vosk**: Alternative engine using Vosk models
  
- **Streaming ASR Engines** (real-time transcription)
  - **Streaming EN**: English streaming model with endpoint detection
  - **Streaming ZH-EN**: Bilingual Chinese + English streaming

- **Voice Activity Detection**: Silero VAD for accurate speech segmentation
- **MVVM Architecture**: Clean separation with ViewModel + StateFlow
- **Real-time Audio Level**: Visual feedback during recording
- **Session Statistics**: Segments, duration, and RTF metrics
- **Export/Share**: Share transcripts via Android share sheet

## 📚 Library Usage

To integrate ASR functionality into your existing Android app, use the **asr-lib** library module.

See [asr-lib/README.md](android/SherpaOnnxVadAsr/asr-lib/README.md) for detailed integration guide and API documentation.

### Quick Integration Example

```kotlin
// 1. Add library dependency in your app/build.gradle
dependencies {
    implementation project(':asr-lib')
}

// 2. Use the library in your code
val asrEngine = AsrLibrary.createOfflineEngine(context)
val transcript = asrEngine.decode(audioSamples, sampleRate = 16000)
asrEngine.close()
```

## Project Structure

```
android/
└── SherpaOnnxVadAsr/
    ├── asr-lib/                    # ASR Library Module (reusable)
    │   └── src/main/
    │       ├── java/com/k2fsa/sherpa/onnx/
    │       │   ├── asr/            # Library public API
    │       │   │   ├── AsrLibrary.kt     # Main entry point
    │       │   │   ├── AsrEngine.kt      # Offline ASR interface
    │       │   │   ├── StreamingAsrEngine.kt # Streaming ASR interface
    │       │   │   ├── SherpaMoonshineEngine.kt
    │       │   │   ├── SherpaStreamingEngine.kt
    │       │   │   ├── VoskEngine.kt
    │       │   │   ├── VadProcessor.kt
    │       │   │   ├── AudioRecorder.kt
    │       │   │   └── AsrEngineFactory.kt
    │       │   ├── OnlineRecognizer.kt   # Streaming JNI bindings
    │       │   ├── OfflineRecognizer.kt  # Offline JNI bindings
    │       │   └── Vad.kt                # VAD JNI bindings
    │       └── assets/             # Models (downloaded via scripts)
    │
    └── app/                        # Demo Application
        └── src/main/
            ├── java/com/k2fsa/sherpa/onnx/vad/asr/
            │   ├── MainActivity.kt
            │   ├── MainViewModel.kt
            │   └── RecordingState.kt
            └── res/                # Android resources

scripts/                           # Setup and build scripts
vendor/                           # Third-party sources (sherpa-onnx)
models/                           # Downloaded model cache
```

## Quick Start

### Prerequisites

- Android Studio (Arctic Fox or later) with NDK
- macOS/Linux for build scripts
- Android device or emulator (API 21+)

### 1. Bootstrap Project

```bash
# Clone sherpa-onnx and set up project structure
./scripts/bootstrap_android_asr.sh
```

### 2. Build Native Libraries & Download Models

```bash
# Configure toolchain (copy and edit)
cp scripts/toolchain.env.example scripts/toolchain.env
# Edit toolchain.env with your NDK path

# Source toolchain config
set -a; source scripts/toolchain.env; set +a

# Build native libs and download Moonshine + VAD models
./scripts/build_sherpa_onnx_android.sh
```

### 3. (Optional) Download Streaming Model

```bash
# Download streaming model for real-time transcription
./scripts/download_streaming_model.sh
```

### 4. Build and Install

```bash
cd android/SherpaOnnxVadAsr
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 5. (Optional) Set Up Vosk

```bash
# Download Vosk model
./scripts/vosk_model_en_full_download.sh

# Push to device
./scripts/vosk_model_push_device.sh
```

## Architecture

### Engine Types

| Engine | Mode | Description |
|--------|------|-------------|
| `MOONSHINE` | Offline | VAD segments audio → batch decode |
| `VOSK` | Offline | VAD segments audio → batch decode |
| `STREAMING_EN` | Streaming | Real-time transcription with endpoint detection |
| `STREAMING_ZH_EN` | Streaming | Bilingual streaming (Chinese + English) |

### Key Components

- **`MainViewModel`**: Orchestrates recording, VAD, and ASR processing
- **`AudioRecorder`**: Encapsulates `AudioRecord` as Kotlin Flow
- **`VadProcessor`**: Wrapper for Silero VAD with segment extraction
- **`AsrEngine`**: Interface for offline (batch) ASR engines
- **`StreamingAsrEngine`**: Interface for streaming ASR engines

### Data Flow

**Offline Mode:**
```
AudioRecorder → VAD → Speech Segments → ASR Engine → Transcript
```

**Streaming Mode:**
```
AudioRecorder → Streaming Engine (decode + endpoint) → Transcript
```

## Testing

```bash
cd android/SherpaOnnxVadAsr

# Unit tests
./gradlew testDebugUnitTest

# Instrumented tests (requires emulator/device)
./gradlew connectedDebugAndroidTest
```

## CI/CD

GitHub Actions workflow (`.github/workflows/android-ci.yml`) runs:
- Unit tests
- Lint checks
- Debug APK build
- Instrumented tests on emulator

## Scripts Reference

| Script | Description |
|--------|-------------|
| `bootstrap_android_asr.sh` | Clone sherpa-onnx, set up project structure |
| `build_sherpa_onnx_android.sh` | Build native libs, download Moonshine + VAD |
| `download_streaming_model.sh` | Download streaming ASR models |
| `vosk_model_en_full_download.sh` | Download Vosk English model |
| `vosk_model_push_device.sh` | Push Vosk model to device |
| `sanity_launch_emulator.sh` | Quick test launch on emulator |

See [scripts/README.md](scripts/README.md) for detailed script documentation.

## Models

All models are downloaded at build time. The app runs **fully offline** after installation.

### Model Inventory

| Model | Size | Use |
|-------|------|-----|
| Moonshine Tiny INT8 | ~119MB | Default offline ASR |
| Silero VAD | ~632KB | Voice activity detection |
| Streaming Zipformer EN | ~20MB | Real-time English ASR |
| Streaming Bilingual ZH-EN | ~100MB | Real-time Chinese + English ASR |
| Vosk EN | ~1.8GB | Alternative offline ASR |

### Storage Locations

Models are stored in two locations based on size:

| Location | Models | Reason |
|----------|--------|--------|
| `android/.../src/main/assets/` | Moonshine, Silero VAD, Streaming | Bundled into APK, deployed automatically |
| `models/vosk/` | Vosk EN | Too large for APK (~1.8GB), pushed to device separately |

**APK-bundled models** (`assets/`):
- Downloaded by `build_sherpa_onnx_android.sh` and `download_streaming_model.sh`
- Automatically included in the APK during build
- No extra deployment steps needed
- Directory is gitignored (models are downloaded on each build)

**Device-side models** (`models/`):
- Downloaded by `vosk_model_en_full_download.sh`
- Pushed to device via `vosk_model_push_device.sh`
- Stored at `/sdcard/vosk-model-en-us-0.22/` on device
- Local `models/` directory serves as download cache

## License

This project uses sherpa-onnx which is licensed under Apache 2.0.

## Acknowledgments

- [k2-fsa/sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) - Next-gen Kaldi ASR
- [alphacep/vosk-api](https://github.com/alphacep/vosk-api) - Offline speech recognition
- [snakers4/silero-vad](https://github.com/snakers4/silero-vad) - Voice activity detection
