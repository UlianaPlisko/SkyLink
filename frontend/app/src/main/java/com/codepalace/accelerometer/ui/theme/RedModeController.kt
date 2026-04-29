package com.codepalace.accelerometer.ui.theme

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.codepalace.accelerometer.data.local.AppSettingsStorage

object RedModeController {

    fun applySavedMode(context: Context) {
        applyMode(AppSettingsStorage(context).redModeEnabled)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        AppSettingsStorage(context).redModeEnabled = enabled
        applyMode(enabled)
    }

    private fun applyMode(enabled: Boolean) {
        AppCompatDelegate.setDefaultNightMode(
            if (enabled) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }
        )
    }
}
