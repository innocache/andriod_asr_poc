#!/usr/bin/env bash
set -euo pipefail

# Downloads and unpacks the Vosk English "full" model (default: vosk-model-en-us-0.22)
#
# Override inputs:
# - VOSK_MODEL_DIR: if set and exists, no download happens; the script prints the directory.
# - VOSK_MODEL_ZIP_URL: alternative download URL.
# - VOSK_MODELS_ROOT: where downloads/unpacks go (default: repo/models/vosk)

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
models_root="${VOSK_MODELS_ROOT:-"$repo_root/models/vosk"}"

if [[ -n "${VOSK_MODEL_DIR:-}" ]]; then
  if [[ ! -d "$VOSK_MODEL_DIR" ]]; then
    echo "VOSK_MODEL_DIR is set but not a directory: $VOSK_MODEL_DIR" >&2
    exit 2
  fi
  echo "$VOSK_MODEL_DIR"
  exit 0
fi

zip_url="${VOSK_MODEL_ZIP_URL:-https://alphacephei.com/vosk/models/vosk-model-en-us-0.22.zip}"
zip_name="$(basename "$zip_url")"
zip_path="$models_root/$zip_name"

mkdir -p "$models_root"

if [[ ! -f "$zip_path" ]]; then
  echo "Downloading: $zip_url" >&2
  curl -fL --retry 3 --retry-delay 2 -o "$zip_path" "$zip_url"
fi

echo "Unpacking: $zip_path" >&2
# Unpack into models_root; avoid re-unzipping if the model folder already exists.
unzip -q -o "$zip_path" -d "$models_root"

# Heuristic: the zip typically contains a single top-level directory.
model_dir="$(zipinfo -1 "$zip_path" | head -n 1 | cut -d/ -f1)"
if [[ -z "$model_dir" || ! -d "$models_root/$model_dir" ]]; then
  echo "Could not determine model dir after unzip under: $models_root" >&2
  exit 3
fi

echo "$models_root/$model_dir"
