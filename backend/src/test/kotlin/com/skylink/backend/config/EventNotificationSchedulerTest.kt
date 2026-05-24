package com.skylink.backend.config

import com.skylink.backend.model.EventParticipantId
import com.skylink.backend.model.entity.Event
import com.skylink.backend.model.entity.EventParticipant
import com.skylink.backend.model.entity.User
import com.skylink.backend.model.enums.EventSource
import com.skylink.backend.model.enums.EventType
import com.skylink.backend.model.enums.UserRole
import com.skylink.backend.repository.EventParticipantRepository
import com.skylink.backend.repository.EventRepository
import com.skylink.backend.repository.UserRepository
import com.skylink.backend.service.FcmNotificationService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.Optional

class EventNotificationSchedulerTest {

    private val eventRepository: EventRepository = mock()
    private val participantRepository: EventParticipantRepository = mock()
    private val userRepository: UserRepository = mock()
    private val fcmService: FcmNotificationService = mock()
    private val scheduler = EventNotificationScheduler(
        eventRepository,
        participantRepository,
        userRepository,
        fcmService
    )

    @Test
    fun `send reminders skips empty states and sends available tokens`() {
        val eventNoParticipants = event(1L, "Empty")
        val eventMissingUser = event(2L, "Missing user")
        val eventNoToken = event(3L, "No token")
        val eventWithToken = event(4L, "With token")
        val eventFailure = event(5L, "Failure")
        val eventNullToken = event(6L, "Null token")
        whenever(eventRepository.findByStartAtBetween(any(), any()))
            .thenReturn(
                listOf(eventNoParticipants, eventMissingUser, eventNoToken, eventWithToken, eventFailure, eventNullToken),
                emptyList(),
                emptyList(),
                emptyList()
            )
        whenever(participantRepository.findByIdEventId(1L)).thenReturn(emptyList())
        whenever(participantRepository.findByIdEventId(2L)).thenReturn(listOf(participant(2L, 20L, eventMissingUser)))
        whenever(participantRepository.findByIdEventId(3L)).thenReturn(listOf(participant(3L, 30L, eventNoToken)))
        whenever(participantRepository.findByIdEventId(4L)).thenReturn(listOf(participant(4L, 40L, eventWithToken)))
        whenever(participantRepository.findByIdEventId(5L)).thenReturn(listOf(participant(5L, 50L, eventFailure)))
        whenever(participantRepository.findByIdEventId(6L)).thenReturn(listOf(participant(6L, 60L, eventNullToken)))
        whenever(userRepository.findById(20L)).thenReturn(Optional.empty())
        whenever(userRepository.findById(30L)).thenReturn(Optional.of(user(30L, "")))
        whenever(userRepository.findById(40L)).thenReturn(Optional.of(user(40L, "token-40")))
        whenever(userRepository.findById(50L)).thenReturn(Optional.of(user(50L, "token-50")))
        whenever(userRepository.findById(60L)).thenReturn(Optional.of(user(60L, null)))
        doThrow(IllegalStateException("boom")).whenever(fcmService)
            .sendEventReminder("token-50", "Failure", 5L, "2026-05-24", 5L)

        scheduler.sendReminders()

        verify(fcmService).sendEventReminder("token-40", "With token", 4L, "2026-05-24", 5L)
        verify(fcmService).sendEventReminder("token-50", "Failure", 5L, "2026-05-24", 5L)
    }

    private fun event(id: Long, title: String) = Event(
        id = id,
        title = title,
        description = null,
        eventType = EventType.OTHER,
        startAt = Instant.parse("2026-05-24T08:00:00Z"),
        endAt = null,
        creatorId = 1L,
        source = EventSource.USER
    )

    private fun participant(eventId: Long, userId: Long, event: Event) = EventParticipant(
        id = EventParticipantId(eventId, userId),
        event = event,
        user = user(userId, "token"),
        enrolledAt = Instant.parse("2026-05-23T08:00:00Z")
    )

    private fun user(id: Long, token: String?) = User(
        id = id,
        email = "user$id@test.com",
        displayName = "User $id",
        role = UserRole.OBSERVER,
        fcmToken = token
    )
}
