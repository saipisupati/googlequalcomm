package com.roost.app.runtime

/**
 * Per-feature flags for choosing between real LiteRT models and demo-mode mocks.
 *
 * Why two flags instead of one:
 *   - Gemma 4 E2B and FastVLM have independent failure modes (different model files,
 *     different memory pressure). Splitting lets us ship the demo with real Gemma
 *     but mock vision, or vice versa — whatever's broken at demo time gets flipped.
 *
 * Runtime safety net:
 *   - FallbackLLMEngine wraps the real engine and catches model-load failures,
 *     silently degrading to the mock. So even if both flags are false (= "use real"),
 *     the demo still works on a phone with no model files pushed yet.
 */
object DemoMode {
    /** When true, skip the real LiteRtGemmaEngine entirely and use MockLocalLLMEngine. */
    const val FORCE_MOCK_LLM: Boolean = false

    /** When true, skip the real LiteRtVisionEngine entirely and use MockReceiptVisionEngine. */
    const val FORCE_MOCK_VISION: Boolean = true // FastVLM not yet wired
}
