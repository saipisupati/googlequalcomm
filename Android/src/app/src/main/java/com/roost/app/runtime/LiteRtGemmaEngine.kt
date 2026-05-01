package com.roost.app.runtime

import android.content.Context

/**
 * Real LiteRT-LM Gemma 4 E2B engine — placeholder body for now.
 *
 * To wire this up:
 *   1. Add to app/build.gradle.kts:
 *        implementation("com.google.ai.edge.litertlm:litertlm-android:0.10.0")
 *   2. Push the model:
 *        scripts/push-models-to-device.sh
 *      pushes /sdcard/Download/gemma-4-E2B-it_qualcomm_sm8750.litertlm
 *   3. Replace the body of generateBudgetAdvice with:
 *
 *        val config = EngineConfig(
 *            modelPath = ModelPaths.gemmaPath,
 *            backend = Backend.NPU(nativeLibraryDir = context.applicationInfo.nativeLibraryDir),
 *            maxNumTokens = 256,
 *            cacheDir = context.cacheDir.absolutePath,
 *        )
 *        val engine = Engine(config).also { it.initialize() }
 *        engine.createConversation().use { conv ->
 *            val sb = StringBuilder()
 *            conv.sendMessageAsync(prompt).collect { token -> sb.append(token) }
 *            return sb.toString().trim()
 *        }
 *
 *   4. Flip DemoMode.ENABLED to false.
 *
 * Source pattern: github.com/google-ai-edge/gallery LlmChatModelHelper.kt.
 */
class LiteRtGemmaEngine(
    @Suppress("UNUSED_PARAMETER") private val context: Context,
) : LocalLLMEngine {

    override suspend fun generateBudgetAdvice(prompt: String): String {
        // TODO: replace with LiteRT-LM Engine.createConversation().sendMessageAsync flow.
        // Until that's wired, throw so callers know to fall back to the mock via DemoMode.
        throw NotImplementedError(
            "LiteRtGemmaEngine not yet wired. Set DemoMode.ENABLED = true or finish the TODO in this file."
        )
    }
}
