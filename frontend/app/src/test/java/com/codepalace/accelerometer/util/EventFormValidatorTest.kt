package com.codepalace.accelerometer.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class EventFormValidatorTest {

    private val today = LocalDate.of(2026, 5, 24)
    private val futureDate = LocalDate.of(2026, 5, 25)

    @Test
    fun `valid event data passes and parses capacity`() {
        val result = EventFormValidator.validate(
            eventName = "Meteor night",
            description = "Watch the sky together",
            dateText = "2026-05-25",
            selectedDate = futureDate,
            startTime = LocalTime.of(21, 0),
            endTime = LocalTime.of(23, 0),
            location = "Bratislava",
            capacityText = "12",
            today = today
        )

        assertTrue(result.isValid)
        assertEquals(12, result.parsedCapacity)
        assertNull(result.capacityError)
    }

    @Test
    fun `missing mandatory event fields return field errors`() {
        val result = EventFormValidator.validate(
            eventName = " ",
            description = "",
            dateText = "",
            selectedDate = today,
            startTime = null,
            endTime = null,
            location = "",
            capacityText = "",
            today = today
        )

        assertFalse(result.isValid)
        assertEquals(EventFormValidator.EVENT_NAME_REQUIRED, result.eventNameError)
        assertEquals(EventFormValidator.DESCRIPTION_REQUIRED, result.descriptionError)
        assertEquals(EventFormValidator.DATE_REQUIRED, result.dateError)
        assertEquals(EventFormValidator.START_TIME_REQUIRED, result.startTimeError)
        assertEquals(EventFormValidator.LOCATION_REQUIRED, result.locationError)
    }

    @Test
    fun `past event date is rejected`() {
        val result = EventFormValidator.validate(
            eventName = "Past event",
            description = "Already happened",
            dateText = "2026-05-23",
            selectedDate = LocalDate.of(2026, 5, 23),
            startTime = LocalTime.of(20, 0),
            endTime = null,
            location = "Kosice",
            capacityText = "",
            today = today
        )

        assertFalse(result.isValid)
        assertEquals(EventFormValidator.DATE_IN_PAST, result.dateError)
    }

    @Test
    fun `capacity must be a positive whole number when provided`() {
        val negative = validResultWithCapacity("-1")
        val text = validResultWithCapacity("many")

        assertEquals(EventFormValidator.CAPACITY_POSITIVE, negative.capacityError)
        assertEquals(EventFormValidator.CAPACITY_INTEGER, text.capacityError)
    }

    @Test
    fun `end time must be after start time`() {
        val result = EventFormValidator.validate(
            eventName = "Short event",
            description = "Invalid time range",
            dateText = "2026-05-25",
            selectedDate = futureDate,
            startTime = LocalTime.of(22, 0),
            endTime = LocalTime.of(21, 30),
            location = "Nitra",
            capacityText = "5",
            today = today
        )

        assertFalse(result.isValid)
        assertEquals(EventFormValidator.END_TIME_AFTER_START, result.endTimeError)
    }

    private fun validResultWithCapacity(capacity: String) = EventFormValidator.validate(
        eventName = "Meteor night",
        description = "Watch the sky together",
        dateText = "2026-05-25",
        selectedDate = futureDate,
        startTime = LocalTime.of(21, 0),
        endTime = null,
        location = "Bratislava",
        capacityText = capacity,
        today = today
    )
}
