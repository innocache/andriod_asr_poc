#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ANDROID_DIR="$ROOT_DIR/android/SherpaOnnxVadAsr"

ADB_SERIAL="${ADB_SERIAL:-emulator-5554}"
APP_PACKAGE_DEFAULT="${APP_PACKAGE:-com.k2fsa.sherpa.onnx}"
FORCE_ASR_ENGINE="${FORCE_ASR_ENGINE:-}" # optional: MOONSHINE or VOSK

# Optional: point PATH at platform-tools if the user hasn’t already.
if ! command -v adb >/dev/null 2>&1; then
  if [[ -d "$HOME/Library/Android/sdk/platform-tools" ]]; then
    export PATH="$HOME/Library/Android/sdk/platform-tools:$PATH"
  fi
fi

if ! command -v adb >/dev/null 2>&1; then
  echo "ERROR: adb not found in PATH. Install Android platform-tools and/or set PATH." >&2
  exit 1
fi

if ! adb -s "$ADB_SERIAL" get-state >/dev/null 2>&1; then
  echo "ERROR: adb device '$ADB_SERIAL' not available. Set ADB_SERIAL or start an emulator." >&2
  adb devices >&2 || true
  exit 1
fi

if [[ -n "${JAVA_HOME:-}" ]]; then
  :
else
  # Best-effort default for macOS + Android Studio.
  if [[ -d "/Applications/Android Studio.app/Contents/jbr/Contents/Home" ]]; then
    export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
  fi
fi

pushd "$ANDROID_DIR" >/dev/null
./gradlew :app:installDebug --no-daemon
popd >/dev/null

installed_packages="$(adb -s "$ADB_SERIAL" shell pm list packages | tr -d '\r')"

pkg="$APP_PACKAGE_DEFAULT"
if ! grep -q "^package:${pkg}$" <<<"$installed_packages"; then
  # If the default package name isn't installed, try to auto-pick a sherpa-ish one.
  candidates="$(grep -E '^package:com\\.k2fsa\\.sherpa\\.' <<<"$installed_packages" || true)"
  if [[ "$(wc -l <<<"$candidates" | tr -d ' ')" == "1" ]]; then
    pkg="${candidates#package:}"
  fi
fi

if ! grep -q "^package:${pkg}$" <<<"$installed_packages"; then
  echo "ERROR: Expected package '$pkg' not installed." >&2
  echo "Installed sherpa-ish packages:" >&2
  grep -i sherpa <<<"$installed_packages" >&2 || true
  exit 1
fi

resolve_out="$(adb -s "$ADB_SERIAL" shell cmd package resolve-activity --brief -c android.intent.category.LAUNCHER -a android.intent.action.MAIN "$pkg" 2>/dev/null | tr -d '\r' || true)"

component="$(grep '/' <<<"$resolve_out" | tail -n 1 || true)"

adb -s "$ADB_SERIAL" logcat -c

if [[ -n "$component" ]]; then
  if [[ -n "$FORCE_ASR_ENGINE" ]]; then
    adb -s "$ADB_SERIAL" shell am start -n "$component" --es com.k2fsa.sherpa.onnx.extra.FORCE_ASR_ENGINE "$FORCE_ASR_ENGINE" >/dev/null
  else
    adb -s "$ADB_SERIAL" shell am start -n "$component" >/dev/null
  fi
else
  # Fallback if resolve-activity doesn't return a component.
  if [[ -n "$FORCE_ASR_ENGINE" ]]; then
    adb -s "$ADB_SERIAL" shell am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -p "$pkg" --es com.k2fsa.sherpa.onnx.extra.FORCE_ASR_ENGINE "$FORCE_ASR_ENGINE" >/dev/null
  else
    adb -s "$ADB_SERIAL" shell monkey -p "$pkg" -c android.intent.category.LAUNCHER 1 >/dev/null
  fi
fi

sleep 4

echo "== app init logs (filtered) =="
adb -s "$ADB_SERIAL" logcat -d -v time \
  | egrep -i "Select model type|Finished initializing|Initializing (Moonshine|Vosk)|Forced ASR engine|Vosk model|Finished initializing non-streaming recognizer|FATAL EXCEPTION|Fatal signal|SIGABRT|Read binary file|Load 'sherpa-onnx|E/AndroidRuntime" \
  | tail -n 200
