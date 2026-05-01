package com.roost.app.runtime

import android.util.Log

/**
 * Wraps a primary LLM engine and falls back to a secondary if the primary throws.
 * Used to silently degrade from real Gemma to MockLocalLLMEngine when the model file
 * isn't on the device or the NPU refuses to initialize.
 *
 * Each call gets a fresh try at the primary. We deliberately don't cache the failure
 * — if the user pushes a missing model file mid-session, the next request should
 * actually use it. The cost is one ~10s init attempt before falling back; that's
 * fine because the engine itself caches its loaded state once init succeeds.
 */
class FallbackLLMEngine(
    private val primary: LocalLLMEngine,
    private val secondary: LocalLLMEngine,
) : LocalLLMEngine {

    override suspend fun generateBudgetAdvice(prompt: String): String {
        return try {
            primary.generateBudgetAdvice(prompt)
        } catch (t: Throwable) {
            Log.w(TAG, "Primary LLM failed; using fallback for this call", t)
            secondary.generateBudgetAdvice(prompt)
        }
    }

    private companion object {
        const val TAG = "FallbackLLMEngine"
    }
}
