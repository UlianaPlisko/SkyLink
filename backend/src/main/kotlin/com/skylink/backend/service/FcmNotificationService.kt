package com.skylink.backend.service

import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.AndroidNotification
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class FcmNotificationService(private val firebaseApp: FirebaseApp) {

    private val log = LoggerFactory.getLogger(FcmNotificationService::class.java)

    fun sendEventReminder(
        fcmToken: String,
        eventTitle: String,
        eventId: Long,
        eventDate: String,
        minutesBefore: Long
    ) {
        val body = when (minutesBefore) {
            1440L -> "Tomorrow: $eventTitle"
            720L  -> "In 12 hours: $eventTitle"
            30L   -> "Starting in 30 minutes: $eventTitle"
            5L    -> "Starting in 5 minutes: $eventTitle"
            else  -> "Upcoming: $eventTitle"
        }

        val message = Message.builder()
            .setToken(fcmToken)
            .setAndroidConfig(
                AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .setNotification(
                        AndroidNotification.builder()
                            .setTitle("Event Reminder")
                            .setBody(body)
                            .setChannelId("channel_events")
                            .setClickAction("OPEN_CALENDAR")  // ← add this
                            .build()
                    )
                    .build()
            )
            .putData("type", "event")
            .putData("title", "Event Reminder")
            .putData("body", body)
            .putData("event_id", eventId.toString())
            .putData("event_date", eventDate)
            .build()

        try {
            val response = FirebaseMessaging.getInstance(firebaseApp).send(message)
            log.info("FCM sent for event $eventId ($minutesBefore min before): $response")
        } catch (e: Exception) {
            log.error("FCM send failed for event $eventId, token $fcmToken: ${e.message}")
            throw e
        }
    }
}