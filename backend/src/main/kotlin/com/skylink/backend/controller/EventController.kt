package com.skylink.backend.controller

import com.skylink.backend.dto.event.CreateEventRequest
import com.skylink.backend.dto.event.EventResponse
import com.skylink.backend.service.EventServiceInterface
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@Tag(
    name = "Events",
    description = "Operations related to event creation, enrollment, and retrieval"
)
@RestController
@RequestMapping("/api/events")
class EventController(
    private val eventService: EventServiceInterface
) {
    @Operation(summary = "Create a new event")
    @PostMapping
    fun createEvent(
        @Valid @RequestBody request: CreateEventRequest,
        @AuthenticationPrincipal creatorEmail: String
    ): EventResponse {
        return eventService.createEvent(request, creatorEmail)
    }

    @Operation(summary = "Enroll current user to an event")
    @PostMapping("/{eventId}/enroll")
    @ResponseStatus(HttpStatus.CREATED)
    fun enrollToEvent(
        @PathVariable eventId: Long,
        @AuthenticationPrincipal userEmail: String
    ) {
        eventService.enrollToEvent(eventId, userEmail)
    }

    @Operation(summary = "Sign out current user from an event")
    @DeleteMapping("/{eventId}/enroll")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun signOutFromEvent(
        @PathVariable eventId: Long,
        @AuthenticationPrincipal userEmail: String
    ) {
        eventService.signOutFromEvent(eventId, userEmail)
    }

    @Operation(summary = "Get all events")
    @GetMapping
    fun listAllEvents(): List<EventResponse> = eventService.listAllEvents()

    @Operation(summary = "Get events for a specific date")
    @GetMapping("/date")
    fun listEventsForDate(@RequestParam date: LocalDate): List<EventResponse> =
        eventService.listEventsForDate(date)

    @Operation(summary = "Get all future events for current user")
    @GetMapping("/my")
    fun listMyEvents(@AuthenticationPrincipal userEmail: String): List<EventResponse> =
        eventService.listUserFutureEvents(userEmail)
}