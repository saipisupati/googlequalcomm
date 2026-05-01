package com.roost.app.runtime

import android.util.Log

/**
 * Wraps a primary LLM engine and falls back to a secondary if the primary throws.
 * Used to silently degrade from real Gemma to MockLocalLLMEngine when the model file
 * isn't on the device or the NPU refuses to initialize — protects the demo from a
 * model-loading mishap without changing user-facing UI.
 *
 * The fallback is sticky: once the primary fails, we don't keep retrying it. This
 * avoids ~10 seconds of init-then-fail latency on every "Ask Dragon" tap.
 */
class FallbackLLMEngine(
    private val primary: LocalLLMEngine,
    private val secondary: LocalLLMEngine,
) : LocalLLMEngine {

    @Volatile
    private var primaryFailed = false

    override suspend fun generateBudgetAdvice(prompt: String): String {
        if (!primaryFailed) {
            try {
                return primary.generateBudgetAdvice(prompt)
            } catch (t: Throwable) {
                Log.w(TAG, "Primary LLM failed, switching to fallback for the rest of this session", t)
                primaryFailed = true
            }
        }
        return secondary.generateBudgetAdvice(prompt)
    }

    private companion object {
        const val TAG = "FallbackLLMEngine"
    }
}
