package com.codepalace.accelerometer.ui.activity

import com.codepalace.accelerometer.R
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.codepalace.accelerometer.api.ApiClient
import com.codepalace.accelerometer.data.local.AppSettingsStorage
import com.codepalace.accelerometer.ui.MessageKind
import com.codepalace.accelerometer.ui.showAppMessage
import com.codepalace.accelerometer.ui.theme.RedModeController

class SettingsActivity : AppCompatActivity() {

    private lateinit var settingsStorage: AppSettingsStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiClient.init(this)
        settingsStorage = AppSettingsStorage(this)
        setContentView(R.layout.activity_settings)

        applyTopBarInsets(findViewById(R.id.headerBar), extraTopDp = 0)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<LinearLayout>(R.id.itemAccount).setOnClickListener {
            if (ApiClient.getSessionStorage().isLoggedIn()) {
                startActivity(Intent(this, AccountSettingsActivity::class.java))
            } else {
                showAppMessage("Log in to open account settings.", MessageKind.INFO)
                startActivity(Intent(this, AuthActivity::class.java))
            }
        }

        findViewById<LinearLayout>(R.id.itemLocation).setOnClickListener {
            startActivity(Intent(this, LocationSettingsActivity::class.java))
        }

        setupSwitches()
    }

    private fun setupSwitches() {
        findViewById<SwitchCompat>(R.id.switchNotifications).apply {
            isChecked = settingsStorage.notificationsEnabled
            setOnCheckedChangeListener { _, checked ->
                settingsStorage.notificationsEnabled = checked
                showAppMessage(
                    if (checked) "Notifications enabled." else "Notifications disabled.",
                    MessageKind.INFO
                )
            }
        }

        findViewById<SwitchCompat>(R.id.switchRedMode).apply {
            isChecked = settingsStorage.redModeEnabled
            setOnCheckedChangeListener { _, checked ->
                RedModeController.setEnabled(this@SettingsActivity, checked)
                showAppMessage(
                    if (checked) "Red mode enabled." else "Red mode disabled.",
                    MessageKind.INFO
                )
            }
        }

        findViewById<SwitchCompat>(R.id.switchSensors).apply {
            isChecked = settingsStorage.automaticSensors
            setOnCheckedChangeListener { _, checked ->
                settingsStorage.automaticSensors = checked
                showAppMessage(
                    if (checked) "Automatic sensors enabled." else "Manual sky control enabled.",
                    MessageKind.INFO
                )
            }
        }
    }
}
