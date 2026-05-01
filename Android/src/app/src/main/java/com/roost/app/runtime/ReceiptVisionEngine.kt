package com.roost.app.runtime

import android.net.Uri
import com.roost.app.data.Category
import kotlinx.coroutines.delay

/**
 * On-device vision: turn a photo of a receipt or item into a structured purchase draft.
 * Implementations: MockReceiptVisionEngine, LiteRtVisionEngine.
 */
interface ReceiptVisionEngine {
    suspend fun extractPurchaseFromImage(imageUri: Uri?): PurchaseDraft
}

data class PurchaseDraft(
    val merchant: String,
    val amount: Double,
    val suggestedCategory: Category,
    val confidence: Float, // 0..1
)

/**
 * Demo-mode vision. Returns a believable purchase draft without ever opening the camera.
 * Cycles through a small set of samples so repeat scans during a demo show variety.
 */
class MockReceiptVisionEngine : ReceiptVisionEngine {
    private val samples = listOf(
        PurchaseDraft("Chipotle", 14.25, Category.Food, 0.93f),
        PurchaseDraft("Target", 32.10, Category.Household, 0.88f),
        PurchaseDraft("Trader Joe's", 47.62, Category.Groceries, 0.91f),
        PurchaseDraft("Shell", 38.50, Category.Gas, 0.95f),
        PurchaseDraft("AMC Theatres", 22.00, Category.Entertainment, 0.87f),
    )
    private var idx = 0

    override suspend fun extractPurchaseFromImage(imageUri: Uri?): PurchaseDraft {
        delay(700) // simulate VLM inference
        val draft = samples[idx % samples.size]
        idx++
        return draft
    }
}

/**
 * Real FastVLM 0.5B engine — placeholder.
 *
 * To wire up:
 *   1. Push /sdcard/Download/FastVLM-0.5B.qualcomm.sm8750.litertlm via scripts/push-models-to-device.sh
 *   2. In replace() body, build an EngineConfig with visionBackend set, run inference on
 *      the image bitmap, parse merchant + amount from the model's structured output.
 *   3. Flip DemoMode.ENABLED = false once stable.
 */
class LiteRtVisionEngine : ReceiptVisionEngine {
    override suspend fun extractPurchaseFromImage(imageUri: Uri?): PurchaseDraft {
        // TODO: load FastVLM, run on imageUri, parse merchant + amount from output.
        throw NotImplementedError("LiteRtVisionEngine not yet wired. Use MockReceiptVisionEngine in DemoMode.")
    }
}
