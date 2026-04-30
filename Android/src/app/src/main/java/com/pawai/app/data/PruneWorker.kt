package com.pawai.app.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class PruneWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = runCatching {
        val repo = PawRepository(PawDb.getInstance(applicationContext))
        repo.pruneOldData()
        Result.success()
    }.getOrElse { Result.retry() }

    companion object {
        const val UNIQUE_NAME = "paw_prune_worker"
    }
}
