package com.skylink.backend.service

import com.skylink.backend.dto.chat.CreateChatRoomRequest
import com.skylink.backend.dto.chat.UpdateChatRoomRequest
import com.skylink.backend.dto.chat.UpdateReadStateRequest
import com.skylink.backend.model.UserChatSubscriptionId
import com.skylink.backend.model.entity.ChatMessage
import com.skylink.backend.model.entity.ChatRoom
import com.skylink.backend.model.entity.User
import com.skylink.backend.model.entity.UserChatSubscription
import com.skylink.backend.model.enums.ChatRoomType
import com.skylink.backend.model.enums.UserRole
import com.skylink.backend.repository.ChatMessageRepository
import com.skylink.backend.repository.ChatRoomRepository
import com.skylink.backend.repository.UserChatSubscriptionRepository
import com.skylink.backend.repository.UserRepository
import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.access.AccessDeniedException
import java.time.Instant
import java.util.Optional

class ChatRoomServiceTest {

    private val chatRoomRepository: ChatRoomRepository = mock()
    private val chatMessageRepository: ChatMessageRepository = mock()
    private val userChatSubscriptionRepository: UserChatSubscriptionRepository = mock()
    private val userRepository: UserRepository = mock()
    private val service = ChatRoomService(
        chatRoomRepository,
        chatMessageRepository,
        userChatSubscriptionRepository,
        userRepository
    )

    @Test
    fun `create room validates event room, saves room and creator subscription`() {
        val user = user()
        whenever(userRepository.findByEmail(user.email)).thenReturn(Optional.of(user))
        whenever(chatRoomRepository.existsByEventId(9L)).thenReturn(false)
        whenever(chatRoomRepository.save(any())).thenAnswer { invocation ->
            val room = invocation.arguments[0] as ChatRoom
            room.copy(id = 2L)
        }
        whenever(userChatSubscriptionRepository.save(any())).thenAnswer { it.arguments[0] }

        val response = service.createRoom(
            user.email,
            CreateChatRoomRequest("  Event Room  ", ChatRoomType.EVENT, eventId = 9L)
        )

        assertEquals(2L, response.id)
        assertEquals("Event Room", response.name)
        assertEquals(9L, response.eventId)
    }

    @Test
    fun `create room rejects invalid event request duplicate event and missing user`() {
        assertThrows(IllegalArgumentException::class.java) {
            service.createRoom("user@test.com", CreateChatRoomRequest("Event", ChatRoomType.EVENT))
        }

        whenever(chatRoomRepository.existsByEventId(9L)).thenReturn(true)
        assertThrows(IllegalArgumentException::class.java) {
            service.createRoom("user@test.com", CreateChatRoomRequest("Event", ChatRoomType.EVENT, eventId = 9L))
        }

        whenever(chatRoomRepository.existsByEventId(10L)).thenReturn(false)
        whenever(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty())
        assertThrows(EntityNotFoundException::class.java) {
            service.createRoom("missing@test.com", CreateChatRoomRequest("Event", ChatRoomType.EVENT, eventId = 10L))
        }

        whenever(userRepository.findByEmail("regional@test.com")).thenReturn(Optional.of(user(email = "regional@test.com")))
        whenever(chatRoomRepository.save(any())).thenAnswer { invocation ->
            val room = invocation.arguments[0] as ChatRoom
            room.copy(id = 6L)
        }
        whenever(userChatSubscriptionRepository.save(any())).thenAnswer { it.arguments[0] }
        assertEquals(ChatRoomType.REGIONAL, service.createRoom("regional@test.com", CreateChatRoomRequest("Regional", ChatRoomType.REGIONAL)).type)

        whenever(userRepository.findByEmail("global@test.com")).thenReturn(Optional.of(user(email = "global@test.com")))
        assertEquals(ChatRoomType.GLOBAL, service.createRoom("global@test.com", CreateChatRoomRequest("Global", ChatRoomType.GLOBAL)).type)
    }

    @Test
    fun `get rooms uses all or type filtered repositories and get by id maps room`() {
        val global = room()
        val regional = room(id = 3L, type = ChatRoomType.REGIONAL)
        whenever(chatRoomRepository.findAll()).thenReturn(listOf(global, regional))
        whenever(chatRoomRepository.findByType(ChatRoomType.GLOBAL)).thenReturn(listOf(global))
        whenever(chatRoomRepository.findById(2L)).thenReturn(Optional.of(global))
        whenever(chatRoomRepository.findById(404L)).thenReturn(Optional.empty())

        assertEquals(2, service.getAllRooms(null).size)
        assertEquals(2, (service as ChatRoomServiceInterface).getAllRooms().size)
        assertEquals(2, defaultGetAllRooms(service).size)
        assertEquals(listOf(global.id), service.getAllRooms(ChatRoomType.GLOBAL).map { it.id })
        assertEquals("Global", service.getRoomById(2L).name)
        assertThrows(EntityNotFoundException::class.java) { service.getRoomById(404L) }
    }

    @Test
    fun `update room saves changes and rejects duplicate event room`() {
        val room = room()
        val other = room(id = 99L, eventId = 9L, type = ChatRoomType.EVENT)
        whenever(chatRoomRepository.findById(2L)).thenReturn(Optional.of(room))
        whenever(chatRoomRepository.findByEventId(9L)).thenReturn(other)
        whenever(chatRoomRepository.save(room)).thenAnswer { it.arguments[0] }

        val response = service.updateRoom(2L, UpdateChatRoomRequest("  Regional  ", ChatRoomType.REGIONAL, "{}"))

        assertEquals("Regional", response.name)
        assertEquals(ChatRoomType.REGIONAL, response.type)
        assertEquals("{}", response.regionGeom)

        assertThrows(IllegalArgumentException::class.java) {
            service.updateRoom(2L, UpdateChatRoomRequest("Event", ChatRoomType.EVENT, eventId = 9L))
        }

        whenever(chatRoomRepository.findByEventId(2L)).thenReturn(room.copy(type = ChatRoomType.EVENT, eventId = 2L))
        val sameEventResponse = service.updateRoom(2L, UpdateChatRoomRequest("Same Event", ChatRoomType.EVENT, eventId = 2L))
        assertEquals(2L, sameEventResponse.eventId)

        whenever(chatRoomRepository.findByEventId(3L)).thenReturn(null)
        val newEventResponse = service.updateRoom(2L, UpdateChatRoomRequest("New Event", ChatRoomType.EVENT, eventId = 3L))
        assertEquals(3L, newEventResponse.eventId)
    }

    @Test
    fun `delete room checks creator access`() {
        val owner = user()
        val stranger = user(id = 7L, email = "stranger@test.com")
        val room = room(createdBy = owner.id)
        whenever(chatRoomRepository.findById(2L)).thenReturn(Optional.of(room))
        whenever(userRepository.findByEmail(owner.email)).thenReturn(Optional.of(owner))
        whenever(userRepository.findByEmail(stranger.email)).thenReturn(Optional.of(stranger))

        service.deleteRoom(owner.email, 2L)
        verify(chatRoomRepository).delete(room)
        assertThrows(AccessDeniedException::class.java) { service.deleteRoom(stranger.email, 2L) }

        whenever(chatRoomRepository.findById(3L)).thenReturn(Optional.of(room(id = 3L, createdBy = null)))
        assertThrows(AccessDeniedException::class.java) { service.deleteRoom(owner.email, 3L) }
    }

    @Test
    fun `subscribe returns existing or creates new subscription`() {
        val user = user()
        val room = room()
        val existing = subscription(user, room)
        whenever(chatRoomRepository.findById(2L)).thenReturn(Optional.of(room))
        whenever(userRepository.findByEmail(user.email)).thenReturn(Optional.of(user))
        whenever(userChatSubscriptionRepository.findByUserIdAndChatRoomId(user.id, room.id))
            .thenReturn(existing, null)
        whenever(userChatSubscriptionRepository.save(any())).thenAnswer { it.arguments[0] }

        assertEquals(room.id, service.subscribeCurrentUser(user.email, room.id).chatRoomId)
        assertEquals(room.id, service.subscribeCurrentUser(user.email, room.id).chatRoomId)
    }

    @Test
    fun `unsubscribe deletes existing subscription or throws when absent`() {
        val user = user()
        val room = room()
        val existing = subscription(user, room)
        whenever(userRepository.findByEmail(user.email)).thenReturn(Optional.of(user))
        whenever(chatRoomRepository.findById(room.id)).thenReturn(Optional.of(room))
        whenever(userChatSubscriptionRepository.findByUserIdAndChatRoomId(user.id, room.id))
            .thenReturn(existing, null)

        service.unsubscribeCurrentUser(user.email, room.id)
        verify(userChatSubscriptionRepository).delete(existing)
        assertThrows(EntityNotFoundException::class.java) {
            service.unsubscribeCurrentUser(user.email, room.id)
        }
    }

    @Test
    fun `subscription and message queries map responses`() {
        val user = user()
        val room = room()
        val message = message(user, room)
        val sub = subscription(user, room, message)
        whenever(chatRoomRepository.findById(room.id)).thenReturn(Optional.of(room))
        whenever(userRepository.findByEmail(user.email)).thenReturn(Optional.of(user))
        whenever(userChatSubscriptionRepository.findByChatRoomId(room.id)).thenReturn(listOf(sub))
        whenever(userChatSubscriptionRepository.findByUserId(user.id)).thenReturn(emptyList(), listOf(sub))
        whenever(chatMessageRepository.findByRoomIdOrderByCreatedAtAsc(room.id)).thenReturn(listOf(message))
        whenever(chatMessageRepository.findTop1ByRoomIdOrderByCreatedAtDesc(room.id)).thenReturn(message, null)

        assertEquals(message.id, service.getRoomSubscriptions(room.id).single().lastReadMsgId)
        assertEquals(emptyList<Any>(), service.getUserSubscribedRooms(user.email))
        assertEquals(room.id, service.getUserSubscribedRooms(user.email).single().id)
        assertEquals("hello", service.getRoomMessages(room.id).single().content)
        assertEquals("hello", service.getLatestMessage(room.id)?.content)
        assertNull(service.getLatestMessage(room.id))
    }

    @Test
    fun `update read state validates subscription and message room`() {
        val user = user()
        val room = room()
        val otherRoom = room(id = 5L)
        val message = message(user, room)
        val wrongRoomMessage = message(user, otherRoom, id = 100L)
        val sub = subscription(user, room)
        whenever(chatRoomRepository.findById(room.id)).thenReturn(Optional.of(room))
        whenever(userRepository.findByEmail(user.email)).thenReturn(Optional.of(user))
        whenever(userChatSubscriptionRepository.findByUserIdAndChatRoomId(user.id, room.id))
            .thenReturn(sub, sub, sub)
        whenever(chatMessageRepository.findById(message.id)).thenReturn(Optional.of(message))
        whenever(chatMessageRepository.findById(404L)).thenReturn(Optional.empty())
        whenever(chatMessageRepository.findById(100L)).thenReturn(Optional.of(wrongRoomMessage))
        whenever(userChatSubscriptionRepository.save(sub)).thenAnswer { it.arguments[0] }

        assertEquals(message.id, service.updateReadState(user.email, room.id, UpdateReadStateRequest(message.id)).lastReadMsgId)

        whenever(userChatSubscriptionRepository.findByUserIdAndChatRoomId(user.id, room.id)).thenReturn(null)
        assertThrows(EntityNotFoundException::class.java) {
            service.updateReadState(user.email, room.id, UpdateReadStateRequest(message.id))
        }

        whenever(userChatSubscriptionRepository.findByUserIdAndChatRoomId(user.id, room.id)).thenReturn(sub)
        assertThrows(EntityNotFoundException::class.java) {
            service.updateReadState(user.email, room.id, UpdateReadStateRequest(404L))
        }

        whenever(userChatSubscriptionRepository.findByUserIdAndChatRoomId(user.id, room.id)).thenReturn(sub)
        assertThrows(IllegalArgumentException::class.java) {
            service.updateReadState(user.email, room.id, UpdateReadStateRequest(100L))
        }
    }

    private fun user(id: Long = 1L, email: String = "user@test.com") = User(
        id = id,
        email = email,
        displayName = "User",
        role = UserRole.OBSERVER
    )

    private fun room(
        id: Long = 2L,
        type: ChatRoomType = ChatRoomType.GLOBAL,
        eventId: Long? = null,
        createdBy: Long? = 1L
    ) = ChatRoom(
        id = id,
        name = "Global",
        type = type,
        eventId = eventId,
        createdBy = createdBy,
        createdAt = Instant.parse("2026-05-23T08:00:00Z")
    )

    private fun message(user: User, room: ChatRoom, id: Long = 99L) = ChatMessage(
        id = id,
        room = room,
        user = user,
        content = "hello",
        createdAt = Instant.parse("2026-05-23T08:01:00Z")
    )

    private fun subscription(user: User, room: ChatRoom, message: ChatMessage? = null) = UserChatSubscription(
        id = UserChatSubscriptionId(userId = user.id, chatRoomId = room.id),
        user = user,
        chatRoom = room,
        lastReadMessage = message,
        addedAt = Instant.parse("2026-05-23T08:02:00Z")
    )

    private fun defaultGetAllRooms(service: ChatRoomServiceInterface): List<*> {
        val method = Class.forName("com.skylink.backend.service.ChatRoomServiceInterface\$DefaultImpls")
            .getDeclaredMethod(
                "getAllRooms\$default",
                ChatRoomServiceInterface::class.java,
                ChatRoomType::class.java,
                Int::class.javaPrimitiveType,
                Any::class.java
            )
        return method.invoke(null, service, null, 1, null) as List<*>
    }
}
