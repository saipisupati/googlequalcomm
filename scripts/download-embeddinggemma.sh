#!/usr/bin/env bash
# Download just EmbeddingGemma. Standalone script to avoid paste-mangling issues.
set -euo pipefail
mkdir -p "${HOME}/pawai-models"
cd "${HOME}/pawai-models"
hf download litert-community/embeddinggemma-300m embeddinggemma-300M_seq512_mixed-precision.qualcomm.sm8750.tflite --local-dir .
