package com.codepalace.accelerometer.data.model.dto

import java.time.Instant

data class EventResponse(
    val id: Long,
    val title: String,
    val description: String?,
    val eventType: String,
    val startAt: Instant,
    val endAt: Instant?,
    val creatorId: Long,
    val participantsCount: Int,
    val participant: Boolean
)