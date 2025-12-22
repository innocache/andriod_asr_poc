#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VENDOR_DIR="$ROOT_DIR/vendor"
SHERPA_DIR="$VENDOR_DIR/sherpa-onnx"
EXAMPLE_DST="$ROOT_DIR/android/SherpaOnnxVadAsr"

SHERPA_TAG="${SHERPA_TAG:-v1.12.20}"
ABI="${ABI:-arm64-v8a}"

ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}"
ANDROID_NDK_VERSION="${ANDROID_NDK_VERSION:-22.1.7171670}"
ANDROID_NDK="${ANDROID_NDK:-$ANDROID_SDK_ROOT/ndk/$ANDROID_NDK_VERSION}"

say() { printf "\n==> %s\n" "$*"; }

die() { echo "ERROR: $*" >&2; exit 1; }

need_cmd() {
  command -v "$1" >/dev/null 2>&1 || die "Missing required command: $1"
}

need_cmd git
need_cmd find
need_cmd cp
need_cmd curl
need_cmd tar

[[ -d "$SHERPA_DIR/.git" ]] || die "Missing vendor checkout. Run ./scripts/bootstrap_android_asr.sh first."
[[ -d "$EXAMPLE_DST" ]] || die "Missing Android example in ./android. Run ./scripts/bootstrap_android_asr.sh first."

if [[ ! -d "$ANDROID_NDK" ]]; then
  die "ANDROID_NDK not found at: $ANDROID_NDK (set ANDROID_NDK or ANDROID_SDK_ROOT/ANDROID_NDK_VERSION)"
fi

say "Ensuring sherpa-onnx tag is checked out: $SHERPA_TAG"
(
  cd "$SHERPA_DIR"
  git fetch --tags --force
  git checkout -f "$SHERPA_TAG"
)

say "Building sherpa-onnx native libs for ABI=$ABI"
export ANDROID_NDK
(
  cd "$SHERPA_DIR"
  case "$ABI" in
    arm64-v8a)
      ./build-android-arm64-v8a.sh
      ;;
    armeabi-v7a)
      ./build-android-armv7-eabi.sh
      ;;
    x86_64)
      ./build-android-x86-64.sh
      ;;
    *)
      die "Unsupported ABI: $ABI"
      ;;
  esac
)

say "Copying .so libs into Android project jniLibs/$ABI"
JNI_DIR="$EXAMPLE_DST/app/src/main/jniLibs/$ABI"
mkdir -p "$JNI_DIR"

# Robustly locate install/lib output
INSTALL_LIB_DIR=""
if [[ -d "$SHERPA_DIR/build-android-$ABI/install/lib" ]]; then
  INSTALL_LIB_DIR="$SHERPA_DIR/build-android-$ABI/install/lib"
else
  # fallback search
  INSTALL_LIB_DIR="$(find "$SHERPA_DIR" -type d -path "*/build-android-*/install/lib" | head -n 1 || true)"
fi

[[ -n "$INSTALL_LIB_DIR" ]] || die "Could not locate sherpa-onnx install/lib output."

cp -f "$INSTALL_LIB_DIR"/libonnxruntime.so "$JNI_DIR/"
cp -f "$INSTALL_LIB_DIR"/libsherpa-onnx-jni.so "$JNI_DIR/"

say "Downloading models into app assets (Moonshine Tiny INT8 + Silero VAD)"
ASSETS_DIR="$EXAMPLE_DST/app/src/main/assets"
mkdir -p "$ASSETS_DIR"

# Moonshine model tarball
MOON_TARBALL_URL="https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-moonshine-tiny-en-int8.tar.bz2"
MOON_TARBALL="$ASSETS_DIR/sherpa-onnx-moonshine-tiny-en-int8.tar.bz2"
if [[ ! -d "$ASSETS_DIR/sherpa-onnx-moonshine-tiny-en-int8" ]]; then
  curl -L "$MOON_TARBALL_URL" -o "$MOON_TARBALL"
  tar -xjf "$MOON_TARBALL" -C "$ASSETS_DIR"
  rm -f "$MOON_TARBALL"
fi

# Silero VAD
VAD_URL="https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/silero_vad.onnx"
VAD_PATH="$ASSETS_DIR/silero_vad.onnx"
if [[ ! -f "$VAD_PATH" ]]; then
  curl -L "$VAD_URL" -o "$VAD_PATH"
fi

say "Done. Open the project in Android Studio: android/SherpaOnnxVadAsr"
