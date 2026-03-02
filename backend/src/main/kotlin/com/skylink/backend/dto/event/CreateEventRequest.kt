package com.skylink.backend.dto.event

import com.skylink.backend.model.enums.EventType
import jakarta.validation.constraints.Future
import jakarta.validation.constraints.NotBlank
import java.time.Instant

data class CreateEventRequest(
    @field:NotBlank val title: String,
    val description: String?,
    val eventType: EventType,
    @field:Future val startAt: Instant,
    val endAt: Instant?,
    val visibilityGeom: String? = null,
    val locationGeom: String? = null
)