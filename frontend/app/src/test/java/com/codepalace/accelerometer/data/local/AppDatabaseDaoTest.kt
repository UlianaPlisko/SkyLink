package com.codepalace.accelerometer.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppDatabaseDaoTest {

    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `space object dao inserts lists and deletes all`() = runTest {
        val objects = listOf(
            SpaceObjectEntity(1L, "Sirius", "STAR", 1.0, 2.0, -1.4),
            SpaceObjectEntity(2L, "Vega", "STAR", 3.0, 4.0, 0.03)
        )

        database.spaceObjectDao().insertAll(objects)

        assertEquals(objects, database.spaceObjectDao().getAll().first())

        database.spaceObjectDao().deleteAll()

        assertTrue(database.spaceObjectDao().getAll().first().isEmpty())
    }

    @Test
    fun `favorite dao handles insert replace delete and existence checks`() = runTest {
        val first = favorite(spaceObjectId = 1L, favoriteId = 10L, timestamp = 100L)
        val second = favorite(spaceObjectId = 2L, favoriteId = 20L, timestamp = 200L)

        database.favoriteDao().insert(first)

        assertTrue(database.favoriteDao().exists(1L))
        assertFalse(database.favoriteDao().exists(2L))

        database.favoriteDao().replaceAll(listOf(first, second))

        assertEquals(listOf(second, first), database.favoriteDao().getAll())

        database.favoriteDao().deleteBySpaceObjectId(1L)

        assertEquals(listOf(second), database.favoriteDao().getAll())

        database.favoriteDao().deleteAll()

        assertTrue(database.favoriteDao().getAll().isEmpty())
    }

    @Test
    fun `event dao handles dated events participant events and pending actions`() = runTest {
        val joined = event(id = 1L, title = "Joined", isParticipant = true)
        val other = event(id = 2L, title = "Other", isParticipant = false)

        database.eventDao().insertAll(listOf(joined, other))

        assertEquals(listOf(joined, other), database.eventDao().getEventsByDate("2026-05-23"))
        assertEquals(joined, database.eventDao().getEventById(1L))
        assertNull(database.eventDao().getEventById(99L))
        assertEquals(listOf(joined), database.eventDao().getMyEvents())

        database.eventDao().insertPendingAction(PendingEventActionEntity(eventId = 1L, action = "ENROLL", timestamp = 10L))
        val pending = database.eventDao().getAllPendingActions()

        assertEquals(1, pending.size)
        assertEquals("ENROLL", pending.first().action)

        database.eventDao().deletePendingAction(pending.first().id)
        assertTrue(database.eventDao().getAllPendingActions().isEmpty())

        database.eventDao().insertPendingAction(PendingEventActionEntity(eventId = 2L, action = "SIGN_OUT", timestamp = 20L))
        database.eventDao().deleteAllPendingActions()
        assertTrue(database.eventDao().getAllPendingActions().isEmpty())

        database.eventDao().deleteByDate("2026-05-23")
        assertTrue(database.eventDao().getEventsByDate("2026-05-23").isEmpty())
    }

    @Test
    fun `chat room and message daos keep user data isolated`() = runTest {
        val userRoom = chatRoom(id = 1L, userId = 7L, isSubscribed = true, createdAt = "2026-05-23T10:00:00Z")
        val otherUserRoom = chatRoom(id = 2L, userId = 8L, isSubscribed = true, createdAt = "2026-05-23T11:00:00Z")
        val unsubscribed = chatRoom(id = 3L, userId = 7L, isSubscribed = false, createdAt = "2026-05-23T12:00:00Z")

        database.chatRoomDao().insertAll(listOf(userRoom, otherUserRoom, unsubscribed))

        assertEquals(listOf(userRoom, unsubscribed), database.chatRoomDao().getAllRoomsForUser(7L))
        assertEquals(listOf(userRoom), database.chatRoomDao().getSubscribedRooms(7L))

        database.chatRoomDao().updateSubscription(3L, 7L, true)
        assertEquals(2, database.chatRoomDao().getSubscribedRooms(7L).size)

        database.chatRoomDao().insertPendingAction(PendingChatActionEntity(roomId = 3L, action = "SUBSCRIBE", timestamp = 15L))
        val pending = database.chatRoomDao().getAllPendingActions()
        assertEquals("SUBSCRIBE", pending.first().action)

        database.chatRoomDao().deletePendingAction(pending.first().id)
        assertTrue(database.chatRoomDao().getAllPendingActions().isEmpty())

        database.chatRoomDao().deleteAllForUser(7L)
        assertEquals(listOf(otherUserRoom), database.chatRoomDao().getAllRoomsForUser(8L))
        assertTrue(database.chatRoomDao().getAllRoomsForUser(7L).isEmpty())
    }

    @Test
    fun `chat message dao returns latest messages per room and deletes by room`() = runTest {
        val messages = (1L..25L).map {
            ChatMessageEntity(
                id = it,
                roomId = if (it == 25L) 2L else 1L,
                content = "Message $it",
                userId = 7L,
                senderEmail = "user@example.com",
                createdAt = "2026-05-23T10:${it.toString().padStart(2, '0')}:00Z"
            )
        }

        database.chatDao().insertMessages(messages)
        database.chatDao().insertMessage(messages[23].copy(content = "Updated"))

        val last20 = database.chatDao().getLast20Messages(1L)

        assertEquals(20, last20.size)
        assertEquals(24L, last20.first().id)
        assertEquals("Updated", last20.first().content)

        database.chatDao().deleteAllForRoom(1L)

        assertTrue(database.chatDao().getLast20Messages(1L).isEmpty())
        assertEquals(1, database.chatDao().getLast20Messages(2L).size)
    }

    @Test
    fun `pending favorite dao orders actions and deletes by object id`() = runTest {
        val old = PendingFavoriteActionEntity(1L, "ADD", timestamp = 1L)
        val newer = PendingFavoriteActionEntity(2L, "REMOVE", timestamp = 2L)

        database.pendingFavoriteActionDao().insert(newer)
        database.pendingFavoriteActionDao().insert(old)

        assertEquals(listOf(old, newer), database.pendingFavoriteActionDao().getAll())

        database.pendingFavoriteActionDao().deleteBySpaceObjectId(1L)

        assertEquals(listOf(newer), database.pendingFavoriteActionDao().getAll())
    }

    private fun favorite(spaceObjectId: Long, favoriteId: Long, timestamp: Long) = FavoriteEntity(
        spaceObjectId = spaceObjectId,
        favoriteId = favoriteId,
        displayName = "Object $spaceObjectId",
        magnitude = 1.0,
        objectType = "STAR",
        raDeg = 10.0,
        decDeg = 20.0,
        description = "Description",
        note = "Note",
        visibility = 0.8,
        addedAt = "2026-05-23T10:00:00Z",
        timestamp = timestamp
    )

    private fun event(id: Long, title: String, isParticipant: Boolean) = EventEntity(
        id = id,
        title = title,
        description = "Description",
        eventType = "OTHER",
        startAt = "2026-05-23T10:00:00Z",
        endAt = null,
        creatorId = 7L,
        participantsCount = 3,
        isParticipant = isParticipant,
        maxCapacity = 10
    )

    private fun chatRoom(id: Long, userId: Long, isSubscribed: Boolean, createdAt: String) = ChatRoomEntity(
        id = id,
        userId = userId,
        name = "Room $id",
        type = "GLOBAL",
        createdAt = createdAt,
        isSubscribed = isSubscribed
    )
}
