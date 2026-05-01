#!/usr/bin/env bash
# Push downloaded LiteRT-LM model files to the app's private external storage.
#
# IMPORTANT: pushes to /sdcard/Android/data/com.roost.app/files/models/
# instead of /sdcard/Download/. This is the app's "external files" directory,
# which the app can read without any runtime permission. /sdcard/Download
# requires READ_MEDIA_VISUAL_USER_SELECTED on Android 11+, and the native
# LiteRT-LM engine opens via plain open() which can't go through SAF.
#
# The app must be installed before running this script (the destination
# directory is created when the app first launches).
#
# Total ~4.1 GB. Over USB 3 expect 1-2 min.
set -euo pipefail

SRC="${HOME}/pawai-models"
PACKAGE="com.roost.app"
DEST="/sdcard/Android/data/${PACKAGE}/files/models"

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

# Make sure the destination dir exists. The app creates it on first launch
# but if the user runs this before launching the app, we have to mkdir.
echo "==> Ensuring destination dir exists on device..."
"$ADB" shell "mkdir -p $DEST" || true

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
"$ADB" shell "ls -lh $DEST/ 2>/dev/null"
