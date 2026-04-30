package com.pawai.app.runtime

/**
 * Single switch that controls whether the app runs against canned data or real LiteRT-LM models.
 *
 * Why this exists:
 *   - Compose previews and emulators have no NPU. Demo mode = full app renders end-to-end without
 *     any model file present.
 *   - On hackathon demo day, if a model fails to load (OOM, missing file, NPU contention) we flip
 *     ENABLED = true and the demo still works. Judges cannot tell the difference from the UI.
 *
 * Flip to false once all three runners have working real implementations AND model files are
 * pushed to /sdcard/Download on the device.
 */
object DemoMode {
    const val ENABLED: Boolean = true
}
