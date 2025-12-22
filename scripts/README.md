# Scripts

Helper scripts for setting up, building, and testing the Android ASR application.

## Quick Start

```bash
# 1. Bootstrap (clone sherpa-onnx, set up project)
./scripts/bootstrap_android_asr.sh

# 2. Configure toolchain
cp scripts/toolchain.env.example scripts/toolchain.env
# Edit toolchain.env with your paths

# 3. Source toolchain config
set -a; source scripts/toolchain.env; set +a

# 4. Build native libs + download models
./scripts/build_sherpa_onnx_android.sh

# 5. (Optional) Download streaming model
./scripts/download_streaming_model.sh
```

---

## Bootstrap

### `bootstrap_android_asr.sh`

Clones `k2-fsa/sherpa-onnx` into `vendor/sherpa-onnx` and sets up the Android project.

- **Tag**: Configurable via `SHERPA_TAG` (default: `v1.12.20`)
- Copies upstream Android example into `android/SherpaOnnxVadAsr`
- Removes `android.permission.INTERNET` to enforce offline-only runtime

---

## Build Native Libraries + Models

### `build_sherpa_onnx_android.sh`

Builds sherpa-onnx native libraries and downloads required models.

- Builds for selected ABI (default: `arm64-v8a`)
- Copies `.so` files to `android/SherpaOnnxVadAsr/app/src/main/jniLibs/<abi>`
- Downloads Moonshine Tiny INT8 + Silero VAD to assets

---

## Streaming Model Download

### `download_streaming_model.sh`

Downloads streaming ASR models for real-time transcription.

```bash
# Download default English model (~20MB)
./scripts/download_streaming_model.sh

# Or specify a model
./scripts/download_streaming_model.sh sherpa-onnx-streaming-zipformer-en-20M-2023-02-17
```

**Available Models:**
- `sherpa-onnx-streaming-zipformer-en-20M-2023-02-17` - English small (~20MB)
- `sherpa-onnx-streaming-zipformer-bilingual-zh-en-2023-02-20` - Bilingual ZH-EN (~100MB)

---

## Vosk Model Provisioning

### `vosk_model_en_full_download.sh`

Downloads and unpacks the Vosk English model.

**Environment Variables:**
- `VOSK_MODEL_DIR` - Use existing local model directory
- `VOSK_MODEL_ZIP_URL` - Alternative model URL
- `VOSK_MODELS_ROOT` - Local cache root (default: `models/vosk`)

### `vosk_model_push_device.sh`

Pushes the Vosk model to device/emulator.

```bash
./scripts/vosk_model_push_device.sh
```

**Environment Variables:**
- `VOSK_DEVICE_DIR` - Device target directory

---

## Testing & Launch

### `sanity_launch_emulator.sh`

Quick test launch on emulator/device.

- Installs debug APK
- Resolves launcher activity dynamically
- Launches app and prints filtered init logs

**Environment Variables:**
- `ADB_SERIAL` - Target device (default: `emulator-5554`)
- `APP_PACKAGE` - App package (default: `com.k2fsa.sherpa.onnx`)
- `FORCE_ASR_ENGINE` - Force engine: `MOONSHINE`, `VOSK`, `STREAMING_EN`, `STREAMING_ZH_EN`

---

## Toolchain Configuration

### `toolchain.env.example`

Template for toolchain configuration. Copy to `toolchain.env` and customize:

```bash
cp scripts/toolchain.env.example scripts/toolchain.env
# Edit with your NDK path, etc.

# Source before running build scripts
set -a; source scripts/toolchain.env; set +a
```

---

## Notes

- All models are downloaded at setup time
- The app runs **fully offline** after installation
- Native libraries are built from source for reproducibility
