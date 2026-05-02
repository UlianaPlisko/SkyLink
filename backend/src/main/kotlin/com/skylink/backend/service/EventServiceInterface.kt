package com.skylink.backend.service

import com.skylink.backend.dto.event.CreateEventRequest
import com.skylink.backend.dto.event.EventDetailsResponse
import com.skylink.backend.dto.event.EventResponse
import java.time.LocalDate

interface EventServiceInterface {

    fun createEvent(request: CreateEventRequest, creatorEmail: String): EventResponse

    fun enrollToEvent(eventId: Long, userEmail: String)

    fun signOutFromEvent(eventId: Long, userEmail: String)

    fun listAllEvents(userEmail: String): List<EventDetailsResponse>

    fun listEventsForDate(date: LocalDate, userEmail: String): List<EventDetailsResponse>

    fun listUserFutureEvents(userEmail: String): List<EventDetailsResponse>

    fun isUserParticipant(eventId: Long, userEmail: String): Boolean
}