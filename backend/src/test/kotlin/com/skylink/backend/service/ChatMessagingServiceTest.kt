package com.skylink.backend.service

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
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.security.access.AccessDeniedException
import java.time.Instant
import java.util.Optional

class ChatMessagingServiceTest {

    private val chatMessageRepository: ChatMessageRepository = mock()
    private val chatRoomRepository: ChatRoomRepository = mock()
    private val userRepository: UserRepository = mock()
    private val userChatSubscriptionRepository: UserChatSubscriptionRepository = mock()
    private val service = ChatMessagingService(
        chatMessageRepository,
        chatRoomRepository,
        userRepository,
        userChatSubscriptionRepository
    )

    @Test
    fun `send message trims and persists subscribed user message`() {
        val user = user()
        val room = room()
        val subscription = subscription(user, room)
        whenever(userRepository.findByEmail(user.email)).thenReturn(Optional.of(user))
        whenever(chatRoomRepository.findById(room.id)).thenReturn(Optional.of(room))
        whenever(userChatSubscriptionRepository.findByUserIdAndChatRoomId(user.id, room.id)).thenReturn(subscription)
        whenever(chatMessageRepository.save(any())).thenAnswer { invocation ->
            val message = invocation.arguments[0] as ChatMessage
            message.copy(id = 99L, createdAt = Instant.parse("2026-05-23T08:00:00Z"))
        }

        val response = service.sendMessage(user.email, room.id, "  hello sky  ")

        assertEquals(99L, response.id)
        assertEquals(room.id, response.roomId)
        assertEquals(user.id, response.userId)
        assertEquals(user.email, response.senderEmail)
        assertEquals("hello sky", response.content)
        assertEquals(Instant.parse("2026-05-23T08:00:00Z"), response.createdAt)
    }

    @Test
    fun `send message throws when user is missing`() {
        whenever(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty())

        assertThrows(EntityNotFoundException::class.java) {
            service.sendMessage("missing@test.com", 2L, "hello")
        }
    }

    @Test
    fun `send message throws when room is missing`() {
        val user = user()
        whenever(userRepository.findByEmail(user.email)).thenReturn(Optional.of(user))
        whenever(chatRoomRepository.findById(2L)).thenReturn(Optional.empty())

        assertThrows(EntityNotFoundException::class.java) {
            service.sendMessage(user.email, 2L, "hello")
        }
    }

    @Test
    fun `send message throws when user is not subscribed`() {
        val user = user()
        val room = room()
        whenever(userRepository.findByEmail(user.email)).thenReturn(Optional.of(user))
        whenever(chatRoomRepository.findById(room.id)).thenReturn(Optional.of(room))
        whenever(userChatSubscriptionRepository.findByUserIdAndChatRoomId(user.id, room.id)).thenReturn(null)

        assertThrows(AccessDeniedException::class.java) {
            service.sendMessage(user.email, room.id, "hello")
        }
    }

    @Test
    fun `send message throws when content is blank`() {
        val user = user()
        val room = room()
        whenever(userRepository.findByEmail(user.email)).thenReturn(Optional.of(user))
        whenever(chatRoomRepository.findById(room.id)).thenReturn(Optional.of(room))
        whenever(userChatSubscriptionRepository.findByUserIdAndChatRoomId(user.id, room.id))
            .thenReturn(subscription(user, room))

        assertThrows(IllegalArgumentException::class.java) {
            service.sendMessage(user.email, room.id, "   ")
        }
    }

    private fun user() = User(
        id = 1L,
        email = "user@test.com",
        displayName = "User",
        role = UserRole.OBSERVER
    )

    private fun room() = ChatRoom(
        id = 2L,
        name = "Global",
        type = ChatRoomType.GLOBAL,
        createdBy = 1L
    )

    private fun subscription(user: User, room: ChatRoom) = UserChatSubscription(
        id = UserChatSubscriptionId(userId = user.id, chatRoomId = room.id),
        user = user,
        chatRoom = room
    )
}
