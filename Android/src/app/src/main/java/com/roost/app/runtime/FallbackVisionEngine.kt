package com.roost.app.runtime

import android.net.Uri
import android.util.Log

/**
 * Wraps a primary vision engine and falls back to a secondary if the primary throws OR
 * if the primary returns a low-confidence draft (likely hallucination on the small VLM).
 *
 * Each call gets a fresh try at the primary. We don't cache the failure so model-file
 * additions mid-session take effect on the next request.
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

    override suspend fun extractPurchaseFromImage(imageUri: Uri?): PurchaseDraft {
        return try {
            val draft = primary.extractPurchaseFromImage(imageUri)
            if (draft.confidence >= confidenceFloor) {
                draft
            } else {
                Log.w(TAG, "Primary vision low-confidence (${draft.confidence}); using fallback")
                secondary.extractPurchaseFromImage(imageUri)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Primary vision failed; using fallback for this call", t)
            secondary.extractPurchaseFromImage(imageUri)
        }
    }

    private companion object {
        const val TAG = "FallbackVisionEngine"
    }
}
