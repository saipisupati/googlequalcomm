package com.roost.app.runtime

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Real LiteRT-LM Gemma 4 E2B engine. Loads the .litertlm file from /sdcard/Download
 * and runs single-shot inference on the Snapdragon NPU.
 *
 * Lifecycle nuance: `engine.initialize()` is reportedly ~10 seconds for Gemma E2B.
 * We init lazily on the first call and keep the Engine resident; subsequent calls
 * just open a fresh Conversation, which is cheap. The whole thing is mutex-guarded
 * because LiteRT-LM is not thread-safe across concurrent inference.
 *
 * Caller contract:
 *   - Model file must exist at ModelPaths.gemmaPath.
 *   - First call may block for several seconds while the engine warms up.
 *   - If init or inference throws, caller (PromptDispatcher / ViewModel) should
 *     fall back to MockLocalLLMEngine via DemoMode flip.
 */
class LiteRtGemmaEngine(private val context: Context) : LocalLLMEngine {

    private val mutex = Mutex()

    @Volatile
    private var engine: Engine? = null

    override suspend fun generateBudgetAdvice(prompt: String): String = withContext(Dispatchers.IO) {
        val started = System.currentTimeMillis()
        mutex.withLock {
            val eng = engine ?: initEngineLocked().also { engine = it }
            eng.createConversation().use { conv ->
                val sb = StringBuilder()
                conv.sendMessageAsync(prompt)
                    .catch { e ->
                        Log.e(TAG, "Gemma streaming error", e)
                        throw e
                    }
                    .collect { msg -> sb.append(msg.toString()) }
                val response = sb.toString().trim()
                Log.i(TAG, "Gemma response in ${System.currentTimeMillis() - started}ms: ${response.length} chars")
                response
            }
        }
    }

    /**
     * Initialize and load the model. Called from inside the mutex so we never load twice.
     * Throws IllegalStateException if the model file is missing — callers should catch
     * and fall back to mock.
     */
    private fun initEngineLocked(): Engine {
        val modelFile = File(ModelPaths.gemmaPath)
        if (!modelFile.exists() || modelFile.length() < 1_000_000L) {
            throw IllegalStateException(
                "Gemma model not found at ${ModelPaths.gemmaPath}. " +
                    "Run scripts/push-models-to-device.sh or set DemoMode.LLM_ENABLED = false."
            )
        }

        val config = EngineConfig(
            modelPath = ModelPaths.gemmaPath,
            backend = Backend.NPU(nativeLibraryDir = context.applicationInfo.nativeLibraryDir),
            cacheDir = context.cacheDir.absolutePath,
        )
        Log.i(TAG, "Initializing Gemma engine: ${ModelPaths.GEMMA_FILENAME} on NPU")
        val started = System.currentTimeMillis()
        val eng = Engine(config)
        eng.initialize()
        Log.i(TAG, "Gemma engine ready in ${System.currentTimeMillis() - started}ms")
        return eng
    }

    /** Optional: release native resources. Not currently called — engine is per-process singleton. */
    fun close() {
        engine?.close()
        engine = null
    }

    private companion object {
        const val TAG = "LiteRtGemmaEngine"
    }
}
