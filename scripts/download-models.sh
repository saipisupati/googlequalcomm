#!/usr/bin/env bash
# Download all three PawAI model files into ~/pawai-models/.
# Run from anywhere; the script chdirs itself.
#
# Total ~4.1 GB. On Google office wifi expect 5-15 min.
# Models:
#   - Gemma 4 E2B (LLM) — 3.02 GB
#   - FastVLM 0.5B (vision) — 943 MB
#   - EmbeddingGemma 300M (embeddings) — 184 MB
set -euo pipefail

DEST="${HOME}/pawai-models"
mkdir -p "$DEST"
cd "$DEST"

echo "==> Downloading Gemma 4 E2B (3.02 GB)..."
hf download litert-community/gemma-4-E2B-it-litert-lm gemma-4-E2B-it_qualcomm_sm8750.litertlm --local-dir .

echo "==> Downloading FastVLM 0.5B (943 MB)..."
hf download litert-community/FastVLM-0.5B FastVLM-0.5B.qualcomm.sm8750.litertlm --local-dir .

echo "==> Downloading EmbeddingGemma 300M (184 MB)..."
hf download litert-community/embeddinggemma-300m embeddinggemma-300M_seq512_mixed-precision.qualcomm.sm8750.tflite --local-dir .

echo
echo "==> Done. Files in $DEST:"
ls -lh "$DEST"
