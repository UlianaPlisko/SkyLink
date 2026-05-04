package com.codepalace.accelerometer

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.annotation.RequiresApi
import com.codepalace.accelerometer.api.ApiClient
import com.codepalace.accelerometer.ui.theme.RedModeController

class SkylinkApplication : Application() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        ApiClient.init(this)
        RedModeController.applySavedMode(this)

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Channel for Events
            val eventChannel = NotificationChannel(
                "channel_events",
                "Events & Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications about upcoming sky events"
            }

            val chatChannel = NotificationChannel(
                "channel_chat",
                "Chat Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "New messages in chat rooms you follow"
            }

            notificationManager.createNotificationChannels(listOf(eventChannel, chatChannel))
        }
    }
}
