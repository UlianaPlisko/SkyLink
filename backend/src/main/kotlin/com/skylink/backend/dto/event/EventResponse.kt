package com.skylink.backend.dto.event

import java.time.Instant

data class EventResponse(
    val id: Long,
    val title: String,
    val eventType: String,
    val startAt: Instant,
    val creatorDisplayName: String
)