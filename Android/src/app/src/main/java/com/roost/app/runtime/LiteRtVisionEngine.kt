package com.roost.app.runtime

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.roost.app.data.Category
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Real FastVLM 0.5B engine. Takes an image Uri, runs vision inference on the NPU, and
 * returns a structured PurchaseDraft.
 *
 * Strategy notes:
 *   - We send a strict, structured prompt asking for merchant + amount on separate lines.
 *     Free-text JSON requests hallucinate worse on small VLMs than line-formatted output.
 *   - Output is parsed with regex; the category is computed locally via CategoryGuesser
 *     because we don't trust the VLM to pick from our fixed taxonomy.
 *   - Confidence is heuristic: 0.9 if both fields parsed cleanly, less if either is missing.
 *
 * Failure modes (caller should fall back to mock):
 *   - Model file missing → throws IllegalStateException at first call
 *   - Engine.initialize() throws (NPU contention, OOM) → propagates up
 *   - Output unparseable → returns a draft with confidence=0 and empty fields, which the
 *     ViewModel's runCatching will still accept (user edits manually).
 */
class LiteRtVisionEngine(private val context: Context) : ReceiptVisionEngine {

    private val mutex = Mutex()

    @Volatile
    private var engine: Engine? = null

    override suspend fun extractPurchaseFromImage(imageUri: Uri?): PurchaseDraft = withContext(Dispatchers.IO) {
        val uri = imageUri ?: error("LiteRtVisionEngine requires a non-null imageUri")
        val imagePath = uri.path ?: uri.toString()
        val started = System.currentTimeMillis()

        mutex.withLock {
            val eng = engine ?: initEngineLocked().also { engine = it }
            eng.createConversation().use { conv ->
                val response = StringBuilder()
                conv.sendMessageAsync(
                    Contents.of(
                        Content.ImageFile(imagePath),
                        Content.Text(VISION_PROMPT),
                    )
                )
                    .catch { e ->
                        Log.e(TAG, "FastVLM streaming error", e)
                        throw e
                    }
                    .collect { msg -> response.append(msg.toString()) }

                val raw = response.toString().trim()
                Log.i(TAG, "FastVLM raw output (${System.currentTimeMillis() - started}ms): $raw")
                parseDraft(raw)
            }
        }
    }

    private fun initEngineLocked(): Engine {
        val modelFile = File(ModelPaths.fastVlmPath)
        if (!modelFile.exists() || modelFile.length() < 1_000_000L) {
            throw IllegalStateException(
                "FastVLM model not found at ${ModelPaths.fastVlmPath}. " +
                    "Run scripts/push-models-to-device.sh or set DemoMode.FORCE_MOCK_VISION = true."
            )
        }

        val config = EngineConfig(
            modelPath = ModelPaths.fastVlmPath,
            backend = Backend.NPU(nativeLibraryDir = context.applicationInfo.nativeLibraryDir),
            visionBackend = Backend.NPU(nativeLibraryDir = context.applicationInfo.nativeLibraryDir),
            cacheDir = context.cacheDir.absolutePath,
        )
        Log.i(TAG, "Initializing FastVLM engine: ${ModelPaths.FAST_VLM_FILENAME} on NPU")
        val started = System.currentTimeMillis()
        val eng = Engine(config)
        eng.initialize()
        Log.i(TAG, "FastVLM engine ready in ${System.currentTimeMillis() - started}ms")
        return eng
    }

    /**
     * Parse FastVLM output of the form:
     *   MERCHANT: <name>
     *   AMOUNT: <number>
     * with some tolerance for the model embellishing or paraphrasing.
     */
    private fun parseDraft(raw: String): PurchaseDraft {
        val merchant = MERCHANT_REGEX.find(raw)?.groupValues?.getOrNull(1)?.trim().orEmpty()
        val amountStr = AMOUNT_REGEX.find(raw)?.groupValues?.getOrNull(1).orEmpty()
        val amount = amountStr.toDoubleOrNull() ?: 0.0

        val category = if (merchant.isNotBlank()) {
            CategoryGuesser.guess(merchant + " " + raw)
        } else {
            CategoryGuesser.guess(raw)
        }

        val confidence = when {
            merchant.isNotBlank() && amount > 0 -> 0.85f
            merchant.isNotBlank() || amount > 0 -> 0.55f
            else -> 0.0f
        }

        return PurchaseDraft(
            merchant = merchant,
            amount = amount,
            suggestedCategory = category,
            confidence = confidence,
        )
    }

    fun close() {
        engine?.close()
        engine = null
    }

    private companion object {
        const val TAG = "LiteRtVisionEngine"

        // Strict prompt: ask for two named fields on separate lines. FastVLM 0.5B is small,
        // so we keep this short and give a one-shot example to anchor the format.
        const val VISION_PROMPT = """Read this receipt image and extract:
MERCHANT: <store or restaurant name>
AMOUNT: <total dollar amount, numbers only, no currency symbol>

Example output:
MERCHANT: Chipotle
AMOUNT: 14.25

Now read the image and respond in exactly that format."""

        val MERCHANT_REGEX = Regex("""MERCHANT:\s*(.+?)(?:\n|$)""", RegexOption.IGNORE_CASE)
        val AMOUNT_REGEX = Regex("""AMOUNT:\s*\$?(\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)
    }
}
