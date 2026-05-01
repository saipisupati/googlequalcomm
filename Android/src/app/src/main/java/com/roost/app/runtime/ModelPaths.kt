package com.roost.app.runtime

/**
 * Canonical filenames + on-device paths for the three LiteRT-LM model files
 * sourced from huggingface.co/litert-community.
 */
object ModelPaths {
    private const val DOWNLOAD_DIR = "/sdcard/Download"

    const val GEMMA_FILENAME = "gemma-4-E2B-it_qualcomm_sm8750.litertlm"
    const val FAST_VLM_FILENAME = "FastVLM-0.5B.qualcomm.sm8750.litertlm"
    const val EMBEDDING_GEMMA_FILENAME = "embeddinggemma-300M_seq512_mixed-precision.qualcomm.sm8750.tflite"

    val gemmaPath: String = "$DOWNLOAD_DIR/$GEMMA_FILENAME"
    val fastVlmPath: String = "$DOWNLOAD_DIR/$FAST_VLM_FILENAME"
    val embeddingGemmaPath: String = "$DOWNLOAD_DIR/$EMBEDDING_GEMMA_FILENAME"
}
