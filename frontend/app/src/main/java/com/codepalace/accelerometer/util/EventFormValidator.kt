package com.codepalace.accelerometer.util

import java.time.LocalDate
import java.time.LocalTime

data class EventFormValidationResult(
    val eventNameError: String? = null,
    val descriptionError: String? = null,
    val dateError: String? = null,
    val startTimeError: String? = null,
    val endTimeError: String? = null,
    val locationError: String? = null,
    val capacityError: String? = null,
    val parsedCapacity: Int? = null
) {
    val isValid: Boolean
        get() = listOf(
            eventNameError,
            descriptionError,
            dateError,
            startTimeError,
            endTimeError,
            locationError,
            capacityError
        ).all { it == null }

    val firstErrorMessage: String
        get() = listOfNotNull(
            eventNameError,
            descriptionError,
            dateError,
            startTimeError,
            endTimeError,
            locationError,
            capacityError
        ).firstOrNull() ?: "Please fix the highlighted fields."
}

object EventFormValidator {
    const val EVENT_NAME_REQUIRED = "Event name is required"
    const val DESCRIPTION_REQUIRED = "Description is required"
    const val DATE_REQUIRED = "Date is required"
    const val DATE_IN_PAST = "Date cannot be in the past"
    const val START_TIME_REQUIRED = "Start time is required"
    const val END_TIME_AFTER_START = "End time must be after start time"
    const val LOCATION_REQUIRED = "Location is required"
    const val CAPACITY_INTEGER = "Capacity must be a whole number"
    const val CAPACITY_POSITIVE = "Capacity must be greater than 0"

    fun validate(
        eventName: String,
        description: String,
        dateText: String,
        selectedDate: LocalDate,
        startTime: LocalTime?,
        endTime: LocalTime?,
        location: String,
        capacityText: String,
        today: LocalDate = LocalDate.now()
    ): EventFormValidationResult {
        val normalizedCapacity = capacityText.trim()
        val parsedCapacity = normalizedCapacity.takeIf { it.isNotBlank() }?.toIntOrNull()
        val capacityError = when {
            normalizedCapacity.isBlank() -> null
            parsedCapacity == null -> CAPACITY_INTEGER
            parsedCapacity <= 0 -> CAPACITY_POSITIVE
            else -> null
        }

        return EventFormValidationResult(
            eventNameError = EVENT_NAME_REQUIRED.takeIf { eventName.isBlank() },
            descriptionError = DESCRIPTION_REQUIRED.takeIf { description.isBlank() },
            dateError = when {
                dateText.isBlank() -> DATE_REQUIRED
                selectedDate.isBefore(today) -> DATE_IN_PAST
                else -> null
            },
            startTimeError = START_TIME_REQUIRED.takeIf { startTime == null },
            endTimeError = END_TIME_AFTER_START.takeIf {
                startTime != null && endTime != null && !endTime.isAfter(startTime)
            },
            locationError = LOCATION_REQUIRED.takeIf { location.isBlank() },
            capacityError = capacityError,
            parsedCapacity = parsedCapacity.takeIf { capacityError == null }
        )
    }
}
