package com.codepalace.accelerometer.data.model.calendar

import java.time.LocalDate

data class WeekDay(
    val date: LocalDate,
    val dayName: String,
    val dayNumber: String,
    val isSelected: Boolean = false
)