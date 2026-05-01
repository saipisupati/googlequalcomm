package com.example.roost

import android.content.Context
import com.example.roost.data.RoostDatabase
import com.example.roost.data.RoostRepository
import com.example.roost.engine.*

/**
 * Simple dependency container. No Hilt/Dagger for hackathon simplicity.
 *
 * AI Stack:
 * - LLM: LiteRT-LM Gemma 4 on Qualcomm NPU
 * - Vision: ML Kit OCR → Smart Parser → (optional) Gemma refinement
 */
class AppContainer(context: Context) {
    val database = RoostDatabase.getDatabase(context)
    val repository = RoostRepository(database)

    // LiteRT-LM Manager (Qualcomm NPU inference)
    val liteRTLMManager = com.example.qnn_litertlm_gemma.LiteRTLMManager.getInstance(context)

    // LLM Engine: Gemma 4 via LiteRT-LM on Snapdragon NPU
    val llmEngine: LocalLLMEngine = LiteRtGemmaEngine(liteRTLMManager)
    
    // Vision Engine: ML Kit OCR → Smart Parser → Gemma refinement pipeline
    val visionEngine: ReceiptVisionEngine = MLKitReceiptVisionEngine(context, liteRTLMManager)

    // Callback to re-trigger initialization from UI
    var initAction: (() -> Unit)? = null
}
