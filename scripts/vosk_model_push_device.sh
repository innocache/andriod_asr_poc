#!/usr/bin/env bash
set -euo pipefail

# Pushes a local Vosk model directory to an Android emulator/device.
#
# Overrides:
# - ADB_SERIAL: adb device serial (default: emulator-5554)
# - APP_PACKAGE: target app package (default: com.k2fsa.sherpa.onnx)
# - VOSK_MODEL_DIR: existing local model directory to push (skip download)
# - VOSK_DEVICE_DIR: target device directory (default: /sdcard/Android/data/<pkg>/files/vosk-model)

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

adb_serial="${ADB_SERIAL:-emulator-5554}"
app_package="${APP_PACKAGE:-com.k2fsa.sherpa.onnx}"

device_dir_default="/sdcard/Android/data/${app_package}/files/vosk-model"
device_dir="${VOSK_DEVICE_DIR:-$device_dir_default}"

model_dir="$(${repo_root}/scripts/vosk_model_en_full_download.sh)"

if [[ ! -d "$model_dir" ]]; then
  echo "Model directory not found: $model_dir" >&2
  exit 2
fi

echo "Using local model dir: $model_dir"
echo "Pushing to device: $adb_serial -> $device_dir"

aDB="adb -s ${adb_serial}"

# Ensure the app's external files dir exists (created lazily by Android sometimes).
# This mkdir is safe even if the path doesn't exist yet.
$aDB shell "mkdir -p '${device_dir}'"

# Clean target to avoid stale mixed model contents.
$aDB shell "rm -rf '${device_dir}'/*"

# Push directory contents.
$aDB push "$model_dir/." "$device_dir/" >/dev/null

echo "Done. Device model dir: $device_dir"
