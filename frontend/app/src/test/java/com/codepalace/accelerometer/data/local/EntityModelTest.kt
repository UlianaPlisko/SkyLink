package com.codepalace.accelerometer.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EntityModelTest {

    @Test
    fun `room and message entities preserve defaults and copy values`() {
        val room = ChatRoomEntity(1L, 7L, "Global", "GLOBAL")
        val message = ChatMessageEntity(
            id = 1L,
            roomId = room.id,
            content = "Hello",
            userId = 7L,
            senderEmail = "user@example.com",
            createdAt = "2026-05-23T10:00:00Z"
        )

        assertNull(room.regionGeom)
        assertFalse(room.isSubscribed)
        assertEquals(0, room.unreadCount)
        assertEquals("Updated", room.copy(name = "Updated").name)
        assertEquals("Hello", message.content)
    }

    @Test
    fun `space object and detail entities preserve optional fields`() {
        val summary = SpaceObjectEntity(1L, "Sirius", "STAR", 1.0, 2.0, -1.4, timestampCached = 100L)
        val detail = SpaceObjectDetailEntity(
            id = 1L,
            displayName = "Sirius",
            objectClass = null,
            spectralType = "A1V",
            constellation = "Canis Major",
            magnitude = -1.4,
            distanceLy = 8.6,
            raDeg = 1.0,
            decDeg = 2.0,
            description = null,
            wikiSummary = null,
            wikiUrl = null,
            imageUrl = null,
            orbitalModel = null,
            lastComputed = null,
            catalogId = null,
            angularSize = null,
            timestamp = 200L
        )

        assertEquals(100L, summary.timestampCached)
        assertEquals("STAR", summary.objectType)
        assertNull(detail.objectClass)
        assertEquals("A1V", detail.spectralType)
        assertEquals(200L, detail.timestamp)
    }

    @Test
    fun `favorite event and pending action entities keep values stable`() {
        val favorite = FavoriteEntity(
            spaceObjectId = 1L,
            favoriteId = 10L,
            displayName = "Vega",
            magnitude = 0.03,
            objectType = "STAR",
            raDeg = 1.0,
            decDeg = 2.0,
            description = null,
            note = null,
            visibility = 0.9,
            addedAt = null,
            timestamp = 100L
        )
        val event = EventEntity(
            id = 2L,
            title = "Meteor",
            description = null,
            eventType = "METEOR_SHOWER",
            startAt = "2026-05-23T10:00:00Z",
            endAt = null,
            creatorId = 7L,
            participantsCount = 5,
            isParticipant = true,
            maxCapacity = null
        )
        val pendingFavorite = PendingFavoriteActionEntity(1L, "ADD", timestamp = 1L)
        val pendingEvent = PendingEventActionEntity(eventId = 2L, action = "ENROLL", timestamp = 2L)
        val pendingChat = PendingChatActionEntity(roomId = 3L, action = "SUBSCRIBE", timestamp = 3L)

        assertNull(favorite.description)
        assertEquals(0.9, favorite.visibility, 0.0001)
        assertTrue(event.isParticipant)
        assertNull(event.maxCapacity)
        assertEquals("ADD", pendingFavorite.action)
        assertEquals(0L, pendingEvent.id)
        assertEquals(0L, pendingChat.id)
    }
}
