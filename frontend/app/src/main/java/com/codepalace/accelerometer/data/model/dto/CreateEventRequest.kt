package com.codepalace.accelerometer.data.model.dto

import com.codepalace.accelerometer.data.model.enums.EventType

data class CreateEventRequest(
    val title: String,
    val description: String,
    val eventType: EventType,
    val startAt: String,
    val endAt: String? = null,
    val location: String? = null,
    val capacity: Int? = null,
    val chatRoomName: String? = null   // only sent if switch is ON and name is filled
)