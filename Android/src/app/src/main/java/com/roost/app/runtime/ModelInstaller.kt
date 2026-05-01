package com.roost.app.runtime

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Resolves the on-device path for each LiteRT model file. The user pushes models
 * directly to the app's private external storage via scripts/push-models-to-device.sh,
 * which targets /sdcard/Android/data/com.roost.app/files/models/. The app can read
 * that location without any runtime permission, so no copying or permission flow needed.
 *
 * The legacy /sdcard/Download path was rejected because Android 11+ requires
 * READ_MEDIA_* permissions for raw filesystem reads there, and the native LiteRT-LM
 * engine opens via plain open() (not SAF/MediaStore).
 */
object ModelInstaller {
    private const val TAG = "ModelInstaller"

    /**
     * Returns the directory where models should live on-device. Creates it if missing.
     * Uses the app's external files dir which is readable without runtime permission.
     */
    private fun modelsDir(context: Context): File {
        val external = context.getExternalFilesDir(null)
            ?: error("External files dir unavailable; device storage may be full or shared storage detached.")
        return File(external, "models").apply { if (!exists()) mkdirs() }
    }

    /**
     * Logs which model files are present and their sizes. Useful for debugging
     * "why did the engine fall back to mock?" issues during the demo.
     */
    fun logStatus(context: Context) {
        val dir = modelsDir(context)
        Log.i(TAG, "Models dir: ${dir.absolutePath}")
        listOf(
            ModelPaths.GEMMA_FILENAME,
            ModelPaths.FAST_VLM_FILENAME,
            ModelPaths.EMBEDDING_GEMMA_FILENAME,
        ).forEach { name ->
            val f = File(dir, name)
            if (f.exists()) {
                Log.i(TAG, "  $name: ${f.length() / 1_000_000}MB")
            } else {
                Log.w(TAG, "  $name: MISSING")
            }
        }
    }

    fun gemmaPath(context: Context): String =
        File(modelsDir(context), ModelPaths.GEMMA_FILENAME).absolutePath

    fun fastVlmPath(context: Context): String =
        File(modelsDir(context), ModelPaths.FAST_VLM_FILENAME).absolutePath

    fun embeddingGemmaPath(context: Context): String =
        File(modelsDir(context), ModelPaths.EMBEDDING_GEMMA_FILENAME).absolutePath

    fun gemmaPresent(context: Context): Boolean =
        File(modelsDir(context), ModelPaths.GEMMA_FILENAME).exists()

    fun fastVlmPresent(context: Context): Boolean =
        File(modelsDir(context), ModelPaths.FAST_VLM_FILENAME).exists()
}
