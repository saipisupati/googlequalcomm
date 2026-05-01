#!/usr/bin/env bash
# Push downloaded LiteRT-LM model files from ~/pawai-models/ to /sdcard/Download/
# on the connected Android device.
#
# Prerequisite: ./download-models.sh has been run successfully.
#
# Total ~4.1 GB. Over USB 3 expect 1-2 min.
set -euo pipefail

SRC="${HOME}/pawai-models"
DEST="/sdcard/Download"

if [ ! -d "$SRC" ]; then
  echo "Error: $SRC does not exist. Run download-models.sh first."
  exit 1
fi

# Find adb. Try ANDROID_HOME, then the standard Mac SDK location.
ADB="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"
if [ ! -x "$ADB" ]; then
  if command -v adb >/dev/null 2>&1; then
    ADB="adb"
  else
    echo "Error: adb not found. Set ANDROID_HOME or install platform-tools."
    exit 1
  fi
fi

echo "==> Connected devices:"
"$ADB" devices

for f in \
  "gemma-4-E2B-it_qualcomm_sm8750.litertlm" \
  "FastVLM-0.5B.qualcomm.sm8750.litertlm" \
  "embeddinggemma-300M_seq512_mixed-precision.qualcomm.sm8750.tflite"
do
  if [ ! -f "$SRC/$f" ]; then
    echo "==> SKIP: $f not in $SRC"
    continue
  fi
  echo "==> Pushing $f ($(du -h "$SRC/$f" | cut -f1))..."
  "$ADB" push "$SRC/$f" "$DEST/$f"
done

echo
echo "==> Verifying files on device:"
"$ADB" shell "ls -lh $DEST/*.litertlm $DEST/*.tflite 2>/dev/null"
