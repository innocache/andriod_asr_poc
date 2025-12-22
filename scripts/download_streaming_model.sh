#!/usr/bin/env bash
# Download streaming ASR models for sherpa-onnx Android app
# These models are used for real-time streaming transcription

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ASSETS_DIR="$PROJECT_ROOT/android/SherpaOnnxVadAsr/app/src/main/assets"

# Model URLs from GitHub releases
declare -A MODELS=(
    # English small model (~20MB) - fast and lightweight
    ["sherpa-onnx-streaming-zipformer-en-20M-2023-02-17"]="https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-streaming-zipformer-en-20M-2023-02-17.tar.bz2"
    
    # Bilingual Chinese + English model (~100MB)
    ["sherpa-onnx-streaming-zipformer-bilingual-zh-en-2023-02-20"]="https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-streaming-zipformer-bilingual-zh-en-2023-02-20.tar.bz2"
)

# Select which model to download (default: English small)
MODEL_TYPE="${1:-sherpa-onnx-streaming-zipformer-en-20M-2023-02-17}"

if [[ ! -v "MODELS[$MODEL_TYPE]" ]]; then
    echo "Unknown model type: $MODEL_TYPE"
    echo "Available models:"
    for model in "${!MODELS[@]}"; do
        echo "  - $model"
    done
    exit 1
fi

URL="${MODELS[$MODEL_TYPE]}"
ARCHIVE_NAME="$(basename "$URL")"

echo "=== Streaming ASR Model Downloader ==="
echo "Model: $MODEL_TYPE"
echo "URL: $URL"
echo "Assets dir: $ASSETS_DIR"
echo

# Create assets directory if needed
mkdir -p "$ASSETS_DIR"

# Check if model already exists
if [[ -d "$ASSETS_DIR/$MODEL_TYPE" ]]; then
    echo "✓ Model already exists at $ASSETS_DIR/$MODEL_TYPE"
    echo "  Remove it manually if you want to re-download."
    exit 0
fi

# Download
TEMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TEMP_DIR"' EXIT

echo "Downloading model..."
curl -L --progress-bar -o "$TEMP_DIR/$ARCHIVE_NAME" "$URL"

echo "Extracting..."
cd "$TEMP_DIR"
tar -xjf "$ARCHIVE_NAME"

# Find the extracted directory
EXTRACTED_DIR=$(find . -maxdepth 1 -type d -name "sherpa-onnx*" | head -1)
if [[ -z "$EXTRACTED_DIR" ]]; then
    echo "Error: Could not find extracted model directory"
    exit 1
fi

# Move to assets
echo "Moving to assets..."
mv "$EXTRACTED_DIR" "$ASSETS_DIR/$MODEL_TYPE"

echo
echo "✓ Model installed to $ASSETS_DIR/$MODEL_TYPE"
echo
echo "Contents:"
ls -la "$ASSETS_DIR/$MODEL_TYPE"
echo
echo "=== Done ==="
echo
echo "Next steps:"
echo "1. Rebuild the app: ./gradlew :app:assembleDebug"
echo "2. Install on device: adb install -r app/build/outputs/apk/debug/app-debug.apk"
echo "3. Select 'Streaming EN' or 'Streaming ZH-EN' engine in the app"
