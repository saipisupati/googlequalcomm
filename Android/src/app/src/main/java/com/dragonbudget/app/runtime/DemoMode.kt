package com.dragonbudget.app.runtime

/**
 * Feature flag: when true the app uses canned responses instead of LiteRT-LM inference.
 *
 * Why this exists:
 *   - Compose previews / emulators have no NPU; demo mode keeps them functional.
 *   - On hackathon demo day, if Gemma fails to load (OOM, missing file, NPU contention)
 *     we flip ENABLED = true and the demo still works. The user-facing UI is identical.
 *
 * Flip to false once the LiteRT-LM Gemma engine is loaded and stable AND the
 * gemma-4-E2B-it_qualcomm_sm8750.litertlm file is present in /sdcard/Download.
 */
object DemoMode {
    const val ENABLED: Boolean = true
}
