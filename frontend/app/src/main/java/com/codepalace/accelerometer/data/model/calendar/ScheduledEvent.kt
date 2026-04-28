package com.codepalace.accelerometer.data.model.calendar

data class ScheduledEvent(
    val id: String,
    val name: String,
    val description: String,
    val location: String,
    val startTime: String,
    val endTime: String,
    val capacity: String
)