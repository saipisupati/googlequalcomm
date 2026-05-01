package com.roost.app.runtime

import android.net.Uri
import android.util.Log

/**
 * Wraps a primary vision engine and falls back to a secondary if the primary throws OR
 * if the primary returns a low-confidence draft (likely hallucination on the small VLM).
 *
 * Failure is sticky like the LLM fallback: once we know FastVLM isn't going to work
 * this session, no more 10-second init attempts.
 *
 * Confidence threshold: 0.4 — anything below means FastVLM read the image but couldn't
 * parse a merchant or amount. In that case we'd rather give the user a believable demo
 * draft than a half-empty form.
 */
class FallbackVisionEngine(
    private val primary: ReceiptVisionEngine,
    private val secondary: ReceiptVisionEngine,
    private val confidenceFloor: Float = 0.4f,
) : ReceiptVisionEngine {

    @Volatile
    private var primaryFailed = false

    override suspend fun extractPurchaseFromImage(imageUri: Uri?): PurchaseDraft {
        if (!primaryFailed) {
            try {
                val draft = primary.extractPurchaseFromImage(imageUri)
                if (draft.confidence >= confidenceFloor) return draft
                Log.w(TAG, "Primary vision returned low-confidence (${draft.confidence}); falling back")
            } catch (t: Throwable) {
                Log.w(TAG, "Primary vision failed, switching to fallback for the rest of this session", t)
                primaryFailed = true
            }
        }
        return secondary.extractPurchaseFromImage(imageUri)
    }

    private companion object {
        const val TAG = "FallbackVisionEngine"
    }
}
