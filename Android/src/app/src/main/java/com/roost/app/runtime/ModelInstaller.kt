package com.roost.app.runtime

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Copies LiteRT model files from /sdcard/Download/ (where the user pushes them via adb)
 * into the app's private storage. Required because:
 *   - Android 11+ blocks raw filesystem reads from /sdcard/Download/ without
 *     READ_MEDIA_VISUAL_USER_SELECTED or MANAGE_EXTERNAL_STORAGE.
 *   - The native LiteRT-LM engine opens files via plain open(), so it can't use
 *     SAF or MediaStore.
 *
 * App-private storage (context.filesDir) needs no permission. We copy once on
 * first run, after which inference loads from the private path.
 *
 * The copy is incremental: if the destination file already exists with a matching
 * size, we skip it. So pushing a new model variant via adb just requires deleting
 * the cached copy or bumping the source size.
 */
object ModelInstaller {
    private const val TAG = "ModelInstaller"
    private const val SOURCE_DIR = "/sdcard/Download"

    /**
     * Ensure all three model files are available in the app's private storage.
     * Returns true if all expected files are present after the call.
     *
     * Safe to call multiple times — does the minimum work.
     */
    fun ensureInstalled(context: Context): Boolean {
        val targetDir = File(context.filesDir, "models").apply { mkdirs() }
        val files = listOf(
            ModelPaths.GEMMA_FILENAME,
            ModelPaths.FAST_VLM_FILENAME,
            ModelPaths.EMBEDDING_GEMMA_FILENAME,
        )
        var allPresent = true
        for (filename in files) {
            val src = File(SOURCE_DIR, filename)
            val dst = File(targetDir, filename)
            when {
                dst.exists() && src.exists() && dst.length() == src.length() -> {
                    Log.i(TAG, "$filename already installed (${dst.length() / 1_000_000}MB)")
                }
                src.exists() -> {
                    Log.i(TAG, "Copying $filename (${src.length() / 1_000_000}MB) to private storage…")
                    val started = System.currentTimeMillis()
                    try {
                        src.copyTo(dst, overwrite = true)
                        Log.i(TAG, "$filename copied in ${System.currentTimeMillis() - started}ms")
                    } catch (t: Throwable) {
                        Log.e(TAG, "Failed to copy $filename", t)
                        allPresent = false
                    }
                }
                dst.exists() -> {
                    // Source missing but we already have a copy — fine.
                    Log.i(TAG, "$filename present in private storage; source missing")
                }
                else -> {
                    Log.w(TAG, "$filename missing in both /sdcard/Download and private storage")
                    allPresent = false
                }
            }
        }
        return allPresent
    }

    fun gemmaPath(context: Context): String =
        File(File(context.filesDir, "models"), ModelPaths.GEMMA_FILENAME).absolutePath

    fun fastVlmPath(context: Context): String =
        File(File(context.filesDir, "models"), ModelPaths.FAST_VLM_FILENAME).absolutePath

    fun embeddingGemmaPath(context: Context): String =
        File(File(context.filesDir, "models"), ModelPaths.EMBEDDING_GEMMA_FILENAME).absolutePath
}
