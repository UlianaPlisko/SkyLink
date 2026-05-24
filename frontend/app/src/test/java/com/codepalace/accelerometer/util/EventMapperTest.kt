package com.codepalace.accelerometer.util

import com.codepalace.accelerometer.data.local.EventEntity
import com.codepalace.accelerometer.data.model.dto.EventResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class EventMapperTest {

    private val start = Instant.parse("2026-05-23T08:15:00Z")
    private val end = Instant.parse("2026-05-23T09:45:00Z")

    @Test
    fun `event response maps to scheduled event preserving enrollment and capacity`() {
        val scheduled = response().toScheduledEvent()

        assertEquals("10", scheduled.id)
        assertEquals("Star party", scheduled.name)
        assertEquals("OTHER", scheduled.location)
        assertEquals(7, scheduled.participantsCount)
        assertTrue(scheduled.isEnrolled)
        assertEquals("7/12", scheduled.capacityDisplay)
    }

    @Test
    fun `event response maps to entity and back`() {
        val entity = response().toEntity()

        assertEquals(10L, entity.id)
        assertEquals(start.toString(), entity.startAt)
        assertEquals(end.toString(), entity.endAt)
        assertTrue(entity.isParticipant)

        val eventResponse = entity.toEventResponse()
        assertEquals(response(), eventResponse)
        assertEquals("Star party", entity.toDomain().name)
    }

    @Test
    fun `event entity handles nullable end and unlimited capacity`() {
        val entity = EventEntity(
            id = 11L,
            title = "Open event",
            description = null,
            eventType = "COMET",
            startAt = start.toString(),
            endAt = null,
            creatorId = 3L,
            participantsCount = 1,
            isParticipant = false,
            maxCapacity = null
        )

        val scheduled = entity.toDomain()

        assertNull(scheduled.endTime)
        assertNull(scheduled.capacityDisplay)
        assertEquals(false, scheduled.isEnrolled)
    }

    private fun response() = EventResponse(
        id = 10L,
        title = "Star party",
        description = "Watch the sky",
        eventType = "OTHER",
        startAt = start,
        endAt = end,
        creatorId = 3L,
        participantsCount = 7,
        participant = true,
        maxCapacity = 12
    )
}
