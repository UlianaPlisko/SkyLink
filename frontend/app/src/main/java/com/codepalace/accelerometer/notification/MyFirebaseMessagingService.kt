package com.codepalace.accelerometer.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.codepalace.accelerometer.R
import com.codepalace.accelerometer.api.ApiClient
import com.codepalace.accelerometer.data.model.dto.FcmTokenRequest
import com.codepalace.accelerometer.ui.activity.CalendarActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import android.content.SharedPreferences

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New FCM token received: $token")
        saveTokenLocally(token)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val data = remoteMessage.data
        val type = data["type"] ?: return

        when (type) {
            "event" -> {
                // When app is in FOREGROUND, system won't show the notification payload
                // automatically, so we still need to show it manually here
                val title = data["title"] ?: "Event Reminder"
                val body = data["body"] ?: ""
                val eventId = data["event_id"]
                val eventDate = data["event_date"]
                Log.d("FCM", "Foreground event message received, showing manually")
                showNotification(title, body, "channel_events", eventId, eventDate)
            }
            "chat" -> {
                Log.d("FCM", "Chat message received (not implemented yet)")
            }
        }
    }
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showNotification(
        title: String,
        body: String,
        channelId: String,
        eventId: String?,
        eventDate: String?
    ) {
        val intent = Intent(this, CalendarActivity::class.java).apply {
            action = "OPEN_CALENDAR"
            putExtra("event_id", eventId)
            putExtra("event_date", eventDate)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        val notificationId = eventId?.hashCode() ?: System.currentTimeMillis().toInt()
        NotificationManagerCompat.from(this).notify(notificationId, notification)
    }

    private fun saveTokenLocally(token: String) {
        val prefs = getSharedPreferences("fcm_prefs", MODE_PRIVATE)
        prefs.edit().putString("fcm_token", token).apply()
        Log.d("FCM", "Token saved locally → will be sent after login")
    }

    companion object {
        @RequiresApi(Build.VERSION_CODES.O)
        suspend fun sendSavedTokenIfExists(context: Context) {
            val prefs = context.getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)
            val token = prefs.getString("fcm_token", null) ?: return
            try {
                val request = FcmTokenRequest(fcmToken = token)
                val response = ApiClient.profileApi.registerFcmToken(request)
                if (response.isSuccessful) {
                    prefs.edit().remove("fcm_token").apply()
                    Log.d("FCM", "Token sent successfully")
                }
            } catch (e: Exception) {
                Log.e("FCM", "Failed to send token", e)
            }
        }
    }
}