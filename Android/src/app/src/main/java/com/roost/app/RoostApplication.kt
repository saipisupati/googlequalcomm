package com.roost.app

import android.app.Application
import com.roost.app.runtime.ModelInstaller
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RoostApplication : Application() {
    lateinit var container: AppContainer
        private set

    private val installerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Copy model files from /sdcard/Download to app-private storage on first launch.
        // Files in /sdcard/Download require runtime permission on Android 11+; the native
        // LiteRT-LM engine opens via plain open() and can't go through SAF, so we copy
        // once and load from filesDir which needs no permission.
        installerScope.launch { ModelInstaller.ensureInstalled(this@RoostApplication) }
        container = AppContainer(this)
    }
}
