package com.skylink.backend.dto.event

import com.skylink.backend.dto.celestial.SpaceObjectSummary
import java.time.Instant

data class EventDetailsResponse(
    val id: Long,
    val title: String,
    val description: String?,
    val eventType: String,
    val startAt: Instant,
    val endAt: Instant?,
    val creatorId: Long,
    val participantsCount: Int,
    val isParticipant: Boolean,
    val maxCapacity: Int?
)