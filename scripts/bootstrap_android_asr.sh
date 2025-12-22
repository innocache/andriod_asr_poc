#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VENDOR_DIR="$ROOT_DIR/vendor"
SHERPA_DIR="$VENDOR_DIR/sherpa-onnx"
ANDROID_DIR="$ROOT_DIR/android"
EXAMPLE_DST="$ANDROID_DIR/SherpaOnnxVadAsr"

SHERPA_TAG="${SHERPA_TAG:-v1.12.20}"
ABI="${ABI:-arm64-v8a}"

ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}"
ANDROID_NDK_VERSION="${ANDROID_NDK_VERSION:-22.1.7171670}"
ANDROID_NDK="${ANDROID_NDK:-$ANDROID_SDK_ROOT/ndk/$ANDROID_NDK_VERSION}"

CMAKE_VERSION="${ANDROID_CMAKE_VERSION:-}"

say() { printf "\n==> %s\n" "$*"; }

die() { echo "ERROR: $*" >&2; exit 1; }

need_cmd() {
  command -v "$1" >/dev/null 2>&1 || die "Missing required command: $1"
}

need_cmd git
need_cmd rsync
need_cmd curl

mkdir -p "$VENDOR_DIR" "$ANDROID_DIR"

if [[ ! -d "$SHERPA_DIR/.git" ]]; then
  say "Cloning sherpa-onnx into vendor/"
  git clone https://github.com/k2-fsa/sherpa-onnx "$SHERPA_DIR"
fi

say "Checking out sherpa-onnx tag: $SHERPA_TAG"
(
  cd "$SHERPA_DIR"
  git fetch --tags --force
  git checkout -f "$SHERPA_TAG"
)

say "Copying Android example project (SherpaOnnxVadAsr)"
rm -rf "$EXAMPLE_DST"
mkdir -p "$EXAMPLE_DST"
rsync -a --delete "$SHERPA_DIR/android/SherpaOnnxVadAsr/" "$EXAMPLE_DST/"

say "Enforcing offline-only manifest (remove INTERNET permission if present)"
MANIFEST="$EXAMPLE_DST/app/src/main/AndroidManifest.xml"
if [[ -f "$MANIFEST" ]]; then
  # Remove INTERNET uses-permission lines
  perl -0pi -e 's@\s*<uses-permission\s+android:name="android\.permission\.INTERNET"\s*/>\s*@@g' "$MANIFEST"
fi

say "Writing toolchain pins (configurable via env vars)"
cat > "$ROOT_DIR/scripts/toolchain.env.example" <<EOF
# Copy to scripts/toolchain.env and export before running build scripts.
# Example:
#   set -a; source scripts/toolchain.env; set +a
#   ./scripts/build_sherpa_onnx_android.sh

SHERPA_TAG=v1.12.20
ABI=arm64-v8a

ANDROID_SDK_ROOT=$HOME/Library/Android/sdk
ANDROID_NDK_VERSION=22.1.7171670
ANDROID_NDK=$HOME/Library/Android/sdk/ndk/22.1.7171670

# Optional: pin cmake version for Android Studio builds
# ANDROID_CMAKE_VERSION=3.22.1
EOF

say "Done. Next: run ./scripts/build_sherpa_onnx_android.sh to build native libs and download models"
