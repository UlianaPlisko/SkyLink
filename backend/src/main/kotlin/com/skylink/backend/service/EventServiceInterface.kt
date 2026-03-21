package com.skylink.backend.service

import com.skylink.backend.dto.event.CreateEventRequest
import com.skylink.backend.dto.event.EventResponse
import java.time.LocalDate

interface EventServiceInterface {

    fun createEvent(request: CreateEventRequest, creatorEmail: String): EventResponse

    fun enrollToEvent(eventId: Long, userEmail: String)

    fun signOutFromEvent(eventId: Long, userEmail: String)

    fun listAllEvents(): List<EventResponse>

    fun listEventsForDate(date: LocalDate): List<EventResponse>

    fun listUserFutureEvents(userEmail: String): List<EventResponse>
}