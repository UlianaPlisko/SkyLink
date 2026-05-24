package com.skylink.backend.service

import com.skylink.backend.dto.event.CreateEventRequest
import com.skylink.backend.model.EventParticipantId
import com.skylink.backend.model.entity.Event
import com.skylink.backend.model.entity.EventParticipant
import com.skylink.backend.model.entity.User
import com.skylink.backend.model.enums.EventSource
import com.skylink.backend.model.enums.EventType
import com.skylink.backend.model.enums.UserRole
import com.skylink.backend.repository.EventParticipantRepository
import com.skylink.backend.repository.EventRepository
import com.skylink.backend.repository.ParticipantCountProjection
import com.skylink.backend.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.time.LocalDate
import java.util.Optional

class EventServiceTest {

    private val eventRepository: EventRepository = mock()
    private val userRepository: UserRepository = mock()
    private val eventParticipantRepository: EventParticipantRepository = mock()
    private val service = EventService(eventRepository, userRepository, eventParticipantRepository)
    private val now = Instant.parse("2026-05-23T08:00:00Z")

    @Test
    fun `create event allows contributors and admins and rejects other users`() {
        val contributor = user(role = UserRole.CONTRIBUTOR)
        val admin = user(id = 2L, email = "admin@test.com", role = UserRole.ADMIN)
        val observer = user(id = 3L, email = "observer@test.com", role = UserRole.OBSERVER)
        val request = createRequest()
        whenever(userRepository.findByEmail(contributor.email)).thenReturn(Optional.of(contributor))
        whenever(userRepository.findByEmail(admin.email)).thenReturn(Optional.of(admin))
        whenever(userRepository.findByEmail(observer.email)).thenReturn(Optional.of(observer))
        whenever(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty())
        whenever(eventRepository.save(any())).thenAnswer { invocation ->
            val event = invocation.arguments[0] as Event
            event.copy(id = 10L)
        }

        val response = service.createEvent(request, contributor.email)
        val adminResponse = service.createEvent(request, admin.email)

        assertEquals(10L, response.id)
        assertEquals("User", response.creatorDisplayName)
        assertEquals("Admin", adminResponse.creatorDisplayName)
        assertThrows(AccessDeniedException::class.java) { service.createEvent(request, observer.email) }
        assertThrows(BadCredentialsException::class.java) { service.createEvent(request, "missing@test.com") }
    }

    @Test
    fun `enroll validates user event duplicate and capacity`() {
        val user = user()
        val event = event(maxCapacity = 2)
        val unlimitedEvent = event(id = 11L, maxCapacity = null)
        whenever(userRepository.findByEmail(user.email)).thenReturn(Optional.of(user))
        whenever(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty())
        whenever(eventRepository.findById(event.id)).thenReturn(Optional.of(event))
        whenever(eventRepository.findById(unlimitedEvent.id)).thenReturn(Optional.of(unlimitedEvent))
        whenever(eventRepository.findById(404L)).thenReturn(Optional.empty())
        whenever(eventParticipantRepository.existsByIdEventIdAndIdUserId(event.id, user.id))
            .thenReturn(false, true, false)
        whenever(eventParticipantRepository.existsByIdEventIdAndIdUserId(unlimitedEvent.id, user.id)).thenReturn(false)
        whenever(eventParticipantRepository.countByIdEventId(event.id)).thenReturn(1L, 2L)
        whenever(eventParticipantRepository.save(any())).thenAnswer { it.arguments[0] }

        service.enrollToEvent(event.id, user.email)
        service.enrollToEvent(unlimitedEvent.id, user.email)
        assertThrows(ResponseStatusException::class.java) { service.enrollToEvent(event.id, user.email) }
        assertThrows(ResponseStatusException::class.java) { service.enrollToEvent(404L, user.email) }
        assertThrows(ResponseStatusException::class.java) { service.enrollToEvent(event.id, "missing@test.com") }
        assertThrows(ResponseStatusException::class.java) { service.enrollToEvent(event.id, user.email) }
    }

    @Test
    fun `sign out deletes participant or throws`() {
        val user = user()
        val event = event()
        val participant = participant(user, event)
        whenever(userRepository.findByEmail(user.email)).thenReturn(Optional.of(user))
        whenever(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty())
        whenever(eventRepository.findById(event.id)).thenReturn(Optional.of(event))
        whenever(eventRepository.findById(404L)).thenReturn(Optional.empty())
        whenever(eventParticipantRepository.findByIdEventIdAndIdUserId(event.id, user.id))
            .thenReturn(participant, null)

        service.signOutFromEvent(event.id, user.email)
        verify(eventParticipantRepository).delete(participant)
        assertThrows(ResponseStatusException::class.java) { service.signOutFromEvent(event.id, user.email) }
        assertThrows(ResponseStatusException::class.java) { service.signOutFromEvent(404L, user.email) }
        assertThrows(ResponseStatusException::class.java) { service.signOutFromEvent(event.id, "missing@test.com") }
    }

    @Test
    fun `list methods map event details including participant state and counts`() {
        val user = user()
        val creator = user(id = 7L, email = "creator@test.com", displayName = "Creator")
        val event = event(creatorId = creator.id, maxCapacity = 5)
        val date = LocalDate.of(2026, 5, 23)
        val projection = object : ParticipantCountProjection {
            override fun getEventId(): Long = event.id
            override fun getParticipantCount(): Long = 3
        }
        whenever(userRepository.findByEmail(user.email)).thenReturn(Optional.of(user))
        whenever(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty())
        whenever(userRepository.findById(creator.id)).thenReturn(Optional.of(creator))
        whenever(userRepository.findById(99L)).thenReturn(Optional.empty())
        whenever(eventRepository.findByStartAtAfter(any())).thenReturn(listOf(event))
        whenever(eventRepository.findByStartAtBetween(any(), any())).thenReturn(listOf(event))
        whenever(eventRepository.findUserEventsFromToday(any(), any())).thenReturn(listOf(event), emptyList())
        whenever(eventParticipantRepository.findParticipatedEventIds(user.id, listOf(event.id))).thenReturn(listOf(event.id))
        whenever(eventParticipantRepository.countParticipantsByEventIds(listOf(event.id))).thenReturn(listOf(projection))

        val all = service.listAllEvents(user.email).single()
        val forDate = service.listEventsForDate(date, user.email).single()
        val mine = service.listUserFutureEvents(user.email).single()
        val emptyMine = service.listUserFutureEvents(user.email)

        assertEquals(3, all.participantsCount)
        assertTrue(all.isParticipant)
        assertEquals("Creator", creator.displayName)
        assertEquals(event.id, forDate.id)
        assertEquals(event.id, mine.id)
        assertEquals(emptyList<Any>(), emptyMine)
        assertThrows(BadCredentialsException::class.java) { service.listAllEvents("missing@test.com") }
        assertThrows(BadCredentialsException::class.java) { service.listEventsForDate(date, "missing@test.com") }
        assertThrows(BadCredentialsException::class.java) { service.listUserFutureEvents("missing@test.com") }
        whenever(eventRepository.findByStartAtAfter(any())).thenReturn(listOf(event(creatorId = 99L)))
        assertThrows(BadCredentialsException::class.java) {
            service.listAllEvents(user.email)
        }
    }

    @Test
    fun `list events defaults participant count to zero when projection is absent`() {
        val user = user()
        val creator = user(id = 7L, email = "creator@test.com", displayName = "Creator")
        val event = event(creatorId = creator.id)
        whenever(userRepository.findByEmail(user.email)).thenReturn(Optional.of(user))
        whenever(userRepository.findById(creator.id)).thenReturn(Optional.of(creator))
        whenever(eventRepository.findByStartAtAfter(any())).thenReturn(listOf(event))
        whenever(eventParticipantRepository.findParticipatedEventIds(user.id, listOf(event.id))).thenReturn(emptyList())
        whenever(eventParticipantRepository.countParticipantsByEventIds(listOf(event.id))).thenReturn(emptyList())

        val response = service.listAllEvents(user.email).single()

        assertEquals(0, response.participantsCount)
        assertFalse(response.isParticipant)
    }

    @Test
    fun `private event response mapper maps creator display name`() {
        val creator = user(id = 7L, email = "creator@test.com", displayName = "Creator")
        val event = event(creatorId = creator.id)
        whenever(userRepository.findById(creator.id)).thenReturn(Optional.of(creator))
        whenever(userRepository.findById(99L)).thenReturn(Optional.empty())
        val method = EventService::class.java.getDeclaredMethod("mapToEventResponse", Event::class.java)
        method.isAccessible = true

        val response = method.invoke(service, event) as com.skylink.backend.dto.event.EventResponse

        assertEquals("Creator", response.creatorDisplayName)
        assertThrows(java.lang.reflect.InvocationTargetException::class.java) {
            method.invoke(service, event(creatorId = 99L))
        }
    }

    @Test
    fun `participant check validates user and event`() {
        val user = user()
        val event = event()
        whenever(userRepository.findByEmail(user.email)).thenReturn(Optional.of(user))
        whenever(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty())
        whenever(eventRepository.findById(event.id)).thenReturn(Optional.of(event))
        whenever(eventRepository.findById(404L)).thenReturn(Optional.empty())
        whenever(eventParticipantRepository.existsByIdEventIdAndIdUserId(event.id, user.id)).thenReturn(true, false)

        assertTrue(service.isUserParticipant(event.id, user.email))
        assertFalse(service.isUserParticipant(event.id, user.email))
        assertThrows(ResponseStatusException::class.java) { service.isUserParticipant(event.id, "missing@test.com") }
        assertThrows(ResponseStatusException::class.java) { service.isUserParticipant(404L, user.email) }
    }

    private fun createRequest() = CreateEventRequest(
        title = "Star party",
        description = "desc",
        eventType = EventType.OTHER,
        startAt = now.plusSeconds(3600),
        endAt = now.plusSeconds(7200),
        visibilityGeom = "{}",
        locationGeom = "{}",
        maxCapacity = 10
    )

    private fun user(
        id: Long = 1L,
        email: String = "user@test.com",
        displayName: String = id.takeIf { it == 1L }?.let { "User" } ?: "Admin",
        role: UserRole = UserRole.OBSERVER
    ) = User(
        id = id,
        email = email,
        displayName = displayName,
        role = role
    )

    private fun event(
        id: Long = 10L,
        creatorId: Long = 1L,
        maxCapacity: Int? = null
    ) = Event(
        id = id,
        title = "Star party",
        description = "desc",
        eventType = EventType.OTHER,
        startAt = now.plusSeconds(3600),
        endAt = now.plusSeconds(7200),
        visibilityGeom = "{}",
        locationGeom = "{}",
        creatorId = creatorId,
        source = EventSource.USER,
        metadata = "{}",
        maxCapacity = maxCapacity
    )

    private fun participant(user: User, event: Event) = EventParticipant(
        id = EventParticipantId(eventId = event.id, userId = user.id),
        event = event,
        user = user,
        enrolledAt = now
    )
}
