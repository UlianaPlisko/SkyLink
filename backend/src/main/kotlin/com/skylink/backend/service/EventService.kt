package com.skylink.backend.service

import com.skylink.backend.dto.event.CreateEventRequest
import com.skylink.backend.dto.event.EventDetailsResponse
import com.skylink.backend.dto.event.EventResponse
import com.skylink.backend.model.EventParticipantId
import com.skylink.backend.model.entity.Event
import com.skylink.backend.model.entity.EventParticipant
import com.skylink.backend.model.enums.EventSource
import com.skylink.backend.model.enums.UserRole
import com.skylink.backend.repository.EventParticipantRepository
import com.skylink.backend.repository.EventRepository
import com.skylink.backend.repository.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.Instant

@Service
class EventService(
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository,
    private val eventParticipantRepository: EventParticipantRepository
) : EventServiceInterface {

    override fun createEvent(request: CreateEventRequest, creatorEmail: String): EventResponse {

        val user = userRepository.findByEmail(creatorEmail)
            .orElseThrow { BadCredentialsException("User not found") }

        if (user.role != UserRole.CONTRIBUTOR && user.role != UserRole.ADMIN) {
            throw AccessDeniedException("Only contributors (or admins) can create events")
        }

        val event = Event(
            title = request.title,
            description = request.description,
            eventType = request.eventType,
            startAt = request.startAt,
            endAt = request.endAt,
            visibilityGeom = request.visibilityGeom,
            locationGeom = request.locationGeom,
            creatorId = user.id,
            source = EventSource.USER,
            metadata = "{}",
            chatRoomId = null
        )

        val savedEvent = eventRepository.save(event)

        return EventResponse(
            id = savedEvent.id,
            title = savedEvent.title,
            eventType = savedEvent.eventType.name,
            startAt = savedEvent.startAt,
            creatorDisplayName = user.displayName
        )
    }

    override fun enrollToEvent(eventId: Long, userEmail: String) {
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found") }

        val event = eventRepository.findById(eventId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found") }

        val participantId = EventParticipantId(eventId = eventId, userId = user.id)

        if (eventParticipantRepository.existsByIdEventIdAndIdUserId(eventId, user.id)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "User is already enrolled in this event")
        }

        val participant = EventParticipant(
            id = participantId,
            event = event,
            user = user,
            enrolledAt = Instant.now()
        )

        eventParticipantRepository.save(participant)
    }

    override fun signOutFromEvent(eventId: Long, userEmail: String) {
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found") }

        val event = eventRepository.findById(eventId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found") }

        val participant = eventParticipantRepository.findByIdEventIdAndIdUserId(eventId, user.id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User is not enrolled in this event")

        eventParticipantRepository.delete(participant)
    }

    override fun listAllEvents(userEmail: String): List<EventDetailsResponse> {
        val events = eventRepository.findByStartAtAfter(Instant.now())
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { BadCredentialsException("User not found") }
        return mapToEventDetailResponses(events, user.id)
    }

    override fun listEventsForDate(date: LocalDate, userEmail: String): List<EventDetailsResponse> {
        val startOfDay = date.atStartOfDay(ZoneOffset.UTC).toInstant()
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { BadCredentialsException("User not found") }
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
        val events = eventRepository.findByStartAtBetween(startOfDay, endOfDay)
        return mapToEventDetailResponses(events, user.id)
    }

    override fun listUserFutureEvents(userEmail: String): List<EventDetailsResponse> {
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { BadCredentialsException("User not found") }

        val todayStart = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant()
        val events = eventRepository.findUserEventsFromToday(user.id, todayStart)
        return mapToEventDetailResponses(events, user.id)
    }

    override fun isUserParticipant(eventId: Long, userEmail: String): Boolean {

        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found") }

        val event = eventRepository.findById(eventId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found") }

        return eventParticipantRepository.existsByIdEventIdAndIdUserId(event.id, user.id)
    }

    private fun mapToEventResponse(event: Event): EventResponse {
        val creatorName = userRepository.findById(event.creatorId)
            .orElseThrow { BadCredentialsException("Creator not found") }
            .displayName

        return EventResponse(
            id = event.id,
            title = event.title,
            eventType = event.eventType.name,
            startAt = event.startAt,
            creatorDisplayName = creatorName
        )
    }

    private fun mapToEventDetailResponses(
        events: List<Event>,
        userId: Long
    ): List<EventDetailsResponse> {

        if (events.isEmpty()) return emptyList()

        val eventIds = events.mapNotNull { it.id }

        val participatedIds = eventParticipantRepository
            .findParticipatedEventIds(userId, eventIds)
            .toSet()

        val countMap = eventParticipantRepository
            .countParticipantsByEventIds(eventIds)
            .associate { it.getEventId() to it.getParticipantCount() }

        return events.map { event ->
            val creator = userRepository.findById(event.creatorId)
                .orElseThrow { BadCredentialsException("Creator not found") }

            EventDetailsResponse(
                id = event.id,
                title = event.title,
                description = event.description,
                eventType = event.eventType.name,
                startAt = event.startAt,
                endAt = event.endAt,
                creatorId = creator.id,
                participantsCount = (countMap[event.id] ?: 0L).toInt(),
                isParticipant = participatedIds.contains(event.id) // ✅ HERE
            )
        }
    }
}