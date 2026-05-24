package com.skylink.backend.service

import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class FcmNotificationServiceTest {

    @Test
    fun `send event reminder builds messages for all reminder labels`() {
        val firebaseApp: FirebaseApp = mock()
        val firebaseMessaging: FirebaseMessaging = mock()

        Mockito.mockStatic(FirebaseMessaging::class.java).use { firebaseMessagingStatic ->
            firebaseMessagingStatic.`when`<FirebaseMessaging> { FirebaseMessaging.getInstance(firebaseApp) }
                .thenReturn(firebaseMessaging)
            whenever(firebaseMessaging.send(any<Message>())).thenReturn("sent")

            val service = FcmNotificationService(firebaseApp)

            listOf(1440L, 720L, 30L, 5L, 1L).forEach { minutesBefore ->
                service.sendEventReminder("token", "Star party", 10L, "2026-05-23", minutesBefore)
            }

            Mockito.verify(firebaseMessaging, Mockito.times(5)).send(any<Message>())
        }
    }

    @Test
    fun `send event reminder rethrows send failures`() {
        val firebaseApp: FirebaseApp = mock()
        val firebaseMessaging: FirebaseMessaging = mock()

        Mockito.mockStatic(FirebaseMessaging::class.java).use { firebaseMessagingStatic ->
            firebaseMessagingStatic.`when`<FirebaseMessaging> { FirebaseMessaging.getInstance(firebaseApp) }
                .thenReturn(firebaseMessaging)
            whenever(firebaseMessaging.send(any<Message>())).thenThrow(IllegalStateException("send failed"))

            assertThrows(IllegalStateException::class.java) {
                FcmNotificationService(firebaseApp)
                    .sendEventReminder("token", "Star party", 10L, "2026-05-23", 5L)
            }
        }
    }
}
