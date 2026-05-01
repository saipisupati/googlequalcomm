package com.roost.app

import android.app.Application
import com.roost.app.runtime.ModelInstaller

class RoostApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // Models live in app-private external storage. push-models-to-device.sh
        // pushes directly there. We just log what's present so demo-day debugging
        // is fast.
        ModelInstaller.logStatus(this)
        container = AppContainer(this)
    }
}
