package com.codepalace.accelerometer

import android.app.Application
import com.codepalace.accelerometer.api.ApiClient
import com.codepalace.accelerometer.ui.theme.RedModeController

class SkylinkApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        ApiClient.init(this)
        RedModeController.applySavedMode(this)
    }
}
