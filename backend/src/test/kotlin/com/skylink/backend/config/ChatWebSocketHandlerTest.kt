package com.skylink.backend.config

import com.skylink.backend.dto.chat.ChatMessageResponse
import com.skylink.backend.dto.chat.RawSocketRequest
import com.skylink.backend.dto.chat.RawSocketResponse
import com.skylink.backend.model.UserChatSubscriptionId
import com.skylink.backend.model.entity.ChatRoom
import com.skylink.backend.model.entity.User
import com.skylink.backend.model.entity.UserChatSubscription
import com.skylink.backend.model.enums.ChatRoomType
import com.skylink.backend.model.enums.UserRole
import com.skylink.backend.repository.UserChatSubscriptionRepository
import com.skylink.backend.repository.UserRepository
import com.skylink.backend.service.ChatMessagingServiceInterface
import com.skylink.backend.service.JwtService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import tools.jackson.databind.ObjectMapper
import io.jsonwebtoken.JwtException
import java.time.Instant
import java.util.Optional

class ChatWebSocketHandlerTest {

    private val objectMapper: ObjectMapper = mock()
    private val jwtService: JwtService = mock()
    private val userRepository: UserRepository = mock()
    private val subscriptionRepository: UserChatSubscriptionRepository = mock()
    private val messagingService: ChatMessagingServiceInterface = mock()
    private val handler = TestableChatWebSocketHandler(
        objectMapper,
        jwtService,
        userRepository,
        subscriptionRepository,
        messagingService
    )
    private val session: WebSocketSession = mock {
        on { id }.thenReturn("session-1")
        on { isOpen }.thenReturn(true)
    }

    @Test
    fun `connection sends greeting and closed session is ignored`() {
        whenever(objectMapper.writeValueAsString(any<RawSocketResponse>())).thenReturn("json")

        handler.afterConnectionEstablished(session)

        verify(session).sendMessage(any<TextMessage>())
        assertEquals(
            RawSocketResponse(type = "CONNECTED", message = "WebSocket connected. Please authenticate."),
            capturedResponses().single()
        )

        val closedSession: WebSocketSession = mock {
            on { id }.thenReturn("closed")
            on { isOpen }.thenReturn(false)
        }
        handler.afterConnectionEstablished(closedSession)
        verify(closedSession, never()).sendMessage(any<TextMessage>())
    }

    @Test
    fun `auth join send leave and close room lifecycle`() {
        val user = user()
        val room = room()
        val subscription = UserChatSubscription(
            id = UserChatSubscriptionId(user.id, room.id),
            user = user,
            chatRoom = room
        )
        whenever(objectMapper.writeValueAsString(any<RawSocketResponse>())).thenReturn("json")
        whenever(jwtService.extractEmail("token")).thenReturn(user.email)
        whenever(jwtService.validateToken("token", user.email)).thenReturn(true)
        whenever(userRepository.findByEmail(user.email)).thenReturn(Optional.of(user))
        whenever(subscriptionRepository.findByUserIdAndChatRoomId(user.id, room.id)).thenReturn(subscription)
        whenever(messagingService.sendMessage(user.email, room.id, "hello")).thenReturn(
            ChatMessageResponse(1L, room.id, user.id, user.email, "hello", Instant.parse("2026-05-23T08:00:00Z"), null)
        )
        whenever(objectMapper.readValue("auth", RawSocketRequest::class.java))
            .thenReturn(RawSocketRequest(type = "AUTH", token = "token"))
        whenever(objectMapper.readValue("join", RawSocketRequest::class.java))
            .thenReturn(RawSocketRequest(type = "JOIN_ROOM", roomId = room.id))
        whenever(objectMapper.readValue("send", RawSocketRequest::class.java))
            .thenReturn(RawSocketRequest(type = "SEND_MESSAGE", roomId = room.id, content = "hello"))
        whenever(objectMapper.readValue("leave", RawSocketRequest::class.java))
            .thenReturn(RawSocketRequest(type = "LEAVE_ROOM", roomId = room.id))

        handler.receive(session, "auth")
        handler.receive(session, "join")
        handler.receive(session, "send")
        handler.receive(session, "leave")
        handler.afterConnectionClosed(session, CloseStatus.NORMAL)

        verify(messagingService).sendMessage(user.email, room.id, "hello")
        val responses = capturedResponses()
        assertEquals(listOf("AUTH_OK", "JOINED_ROOM", "NEW_MESSAGE", "LEFT_ROOM"), responses.map { it.type })
        assertEquals(user.email, responses[0].email)
        assertEquals(room.id, responses[1].roomId)
        assertEquals(room.id, responses[2].roomId)
        assertEquals(room.id, responses[3].roomId)
    }

    @Test
    fun `message handling sends errors for invalid states`() {
        whenever(objectMapper.writeValueAsString(any<RawSocketResponse>())).thenReturn("json")
        whenever(objectMapper.readValue("bad", RawSocketRequest::class.java)).thenThrow(IllegalArgumentException("bad json"))
        whenever(objectMapper.readValue("null-message", RawSocketRequest::class.java)).thenThrow(RuntimeException())
        whenever(objectMapper.readValue("unknown", RawSocketRequest::class.java)).thenReturn(RawSocketRequest(type = "NOPE"))
        whenever(objectMapper.readValue("join", RawSocketRequest::class.java)).thenReturn(RawSocketRequest(type = "JOIN_ROOM", roomId = 2L))
        whenever(objectMapper.readValue("auth-missing", RawSocketRequest::class.java)).thenReturn(RawSocketRequest(type = "AUTH"))
        whenever(objectMapper.readValue("auth-blank", RawSocketRequest::class.java)).thenReturn(RawSocketRequest(type = "AUTH", token = "   "))
        whenever(objectMapper.readValue("auth-invalid", RawSocketRequest::class.java)).thenReturn(RawSocketRequest(type = "AUTH", token = "bad"))
        whenever(objectMapper.readValue("auth-jwt-exception", RawSocketRequest::class.java)).thenReturn(RawSocketRequest(type = "AUTH", token = "jwt-exception"))
        whenever(jwtService.extractEmail("")).thenThrow(JwtException("blank"))
        whenever(jwtService.extractEmail("bad")).thenReturn("user@test.com")
        whenever(jwtService.extractEmail("jwt-exception")).thenThrow(JwtException("expired"))
        whenever(jwtService.validateToken("bad", "user@test.com")).thenReturn(false)

        handler.receive(session, "bad")
        handler.receive(session, "null-message")
        handler.receive(session, "unknown")
        handler.receive(session, "join")
        handler.receive(session, "auth-missing")
        handler.receive(session, "auth-blank")
        handler.receive(session, "auth-invalid")
        handler.receive(session, "auth-jwt-exception")

        val responses = capturedResponses()
        assertTrue(responses.all { it.type == "ERROR" })
        assertEquals(
            listOf(
                "bad json",
                "Invalid request",
                "Unsupported message type: NOPE",
                "WebSocket session is not authenticated",
                "Missing token",
                "Invalid or expired JWT token",
                "Invalid JWT token",
                "Invalid or expired JWT token"
            ),
            responses.map { it.message }
        )
    }

    @Test
    fun `message handling validates missing room content and missing users`() {
        val user = user()
        whenever(objectMapper.writeValueAsString(any<RawSocketResponse>())).thenReturn("json")
        whenever(jwtService.extractEmail("token")).thenReturn(user.email)
        whenever(jwtService.validateToken("token", user.email)).thenReturn(true)
        whenever(userRepository.findByEmail(user.email)).thenReturn(Optional.empty(), Optional.of(user), Optional.empty())
        whenever(objectMapper.readValue("auth-missing-user", RawSocketRequest::class.java))
            .thenReturn(RawSocketRequest(type = "AUTH", token = "token"))
        whenever(objectMapper.readValue("auth", RawSocketRequest::class.java))
            .thenReturn(RawSocketRequest(type = "AUTH", token = "token"))
        whenever(objectMapper.readValue("join-missing-room", RawSocketRequest::class.java))
            .thenReturn(RawSocketRequest(type = "JOIN_ROOM"))
        whenever(objectMapper.readValue("join-missing-user", RawSocketRequest::class.java))
            .thenReturn(RawSocketRequest(type = "JOIN_ROOM", roomId = 2L))
        whenever(objectMapper.readValue("leave-missing-room", RawSocketRequest::class.java))
            .thenReturn(RawSocketRequest(type = "LEAVE_ROOM"))
        whenever(objectMapper.readValue("leave-empty-room", RawSocketRequest::class.java))
            .thenReturn(RawSocketRequest(type = "LEAVE_ROOM", roomId = 999L))
        whenever(objectMapper.readValue("send-missing-room", RawSocketRequest::class.java))
            .thenReturn(RawSocketRequest(type = "SEND_MESSAGE", content = "hello"))
        whenever(objectMapper.readValue("send-missing-content", RawSocketRequest::class.java))
            .thenReturn(RawSocketRequest(type = "SEND_MESSAGE", roomId = 2L))

        handler.receive(session, "auth-missing-user")
        handler.receive(session, "auth")
        handler.receive(session, "join-missing-room")
        handler.receive(session, "join-missing-user")
        handler.receive(session, "leave-missing-room")
        handler.receive(session, "leave-empty-room")
        handler.receive(session, "send-missing-room")
        handler.receive(session, "send-missing-content")

        val responses = capturedResponses()
        assertEquals(
            listOf("ERROR", "AUTH_OK", "ERROR", "ERROR", "ERROR", "LEFT_ROOM", "ERROR", "ERROR"),
            responses.map { it.type }
        )
        assertEquals(
            listOf(
                "User not found",
                "Authenticated successfully",
                "roomId is required",
                "User not found",
                "roomId is required",
                "Left room 999",
                "roomId is required",
                "content is required"
            ),
            responses.map { it.message }
        )
    }

    @Test
    fun `send rejects authenticated user who has not joined the room`() {
        val user = user()
        val room = room()
        val otherSession: WebSocketSession = mock {
            on { id }.thenReturn("other")
            on { isOpen }.thenReturn(true)
        }
        val subscription = UserChatSubscription(
            id = UserChatSubscriptionId(user.id, room.id),
            user = user,
            chatRoom = room
        )
        whenever(objectMapper.writeValueAsString(any<RawSocketResponse>())).thenReturn("json")
        whenever(jwtService.extractEmail("token")).thenReturn(user.email)
        whenever(jwtService.validateToken("token", user.email)).thenReturn(true)
        whenever(userRepository.findByEmail(user.email)).thenReturn(Optional.of(user))
        whenever(subscriptionRepository.findByUserIdAndChatRoomId(user.id, room.id)).thenReturn(subscription)
        whenever(objectMapper.readValue("auth", RawSocketRequest::class.java))
            .thenReturn(RawSocketRequest(type = "AUTH", token = "token"))
        whenever(objectMapper.readValue("join", RawSocketRequest::class.java))
            .thenReturn(RawSocketRequest(type = "JOIN_ROOM", roomId = room.id))
        whenever(objectMapper.readValue("send", RawSocketRequest::class.java))
            .thenReturn(RawSocketRequest(type = "SEND_MESSAGE", roomId = room.id, content = "hello"))

        handler.receive(otherSession, "auth")
        handler.receive(otherSession, "join")
        handler.receive(session, "auth")
        handler.receive(session, "send")

        verify(messagingService, never()).sendMessage(any(), any(), any())
        assertEquals("You must join room ${room.id} before sending messages", capturedResponses().last().message)
    }

    @Test
    fun `join and send validate subscription and joined room`() {
        val user = user()
        val room = room()
        whenever(objectMapper.writeValueAsString(any<RawSocketResponse>())).thenReturn("json")
        whenever(jwtService.extractEmail("token")).thenReturn(user.email)
        whenever(jwtService.validateToken("token", user.email)).thenReturn(true)
        whenever(userRepository.findByEmail(user.email)).thenReturn(Optional.of(user))
        whenever(subscriptionRepository.findByUserIdAndChatRoomId(user.id, room.id)).thenReturn(null)
        whenever(objectMapper.readValue("auth", RawSocketRequest::class.java))
            .thenReturn(RawSocketRequest(type = "AUTH", token = "token"))
        whenever(objectMapper.readValue("join", RawSocketRequest::class.java))
            .thenReturn(RawSocketRequest(type = "JOIN_ROOM", roomId = room.id))
        whenever(objectMapper.readValue("send", RawSocketRequest::class.java))
            .thenReturn(RawSocketRequest(type = "SEND_MESSAGE", roomId = room.id, content = "hello"))

        handler.receive(session, "auth")
        handler.receive(session, "join")
        handler.receive(session, "send")

        verify(messagingService, never()).sendMessage(any(), any(), any())
        val responses = capturedResponses()
        assertEquals(listOf("AUTH_OK", "ERROR", "ERROR"), responses.map { it.type })
        assertEquals("User is not subscribed to room ${room.id}", responses[1].message)
        assertEquals("You must join room ${room.id} before sending messages", responses[2].message)
    }

    @Test
    fun `broadcast sends saved messages only to open joined sessions`() {
        val user = user()
        val room = room()
        val closedSession: WebSocketSession = mock {
            on { id }.thenReturn("closed-broadcast")
            on { isOpen }.thenReturn(false)
        }
        val subscription = UserChatSubscription(
            id = UserChatSubscriptionId(user.id, room.id),
            user = user,
            chatRoom = room
        )
        val savedMessage = ChatMessageResponse(
            9L,
            room.id,
            user.id,
            user.email,
            "hello",
            Instant.parse("2026-05-23T08:00:00Z"),
            null
        )
        whenever(objectMapper.writeValueAsString(any<RawSocketResponse>())).thenReturn("json")
        whenever(jwtService.extractEmail("token")).thenReturn(user.email)
        whenever(jwtService.validateToken("token", user.email)).thenReturn(true)
        whenever(userRepository.findByEmail(user.email)).thenReturn(Optional.of(user))
        whenever(subscriptionRepository.findByUserIdAndChatRoomId(user.id, room.id)).thenReturn(subscription)
        whenever(messagingService.sendMessage(user.email, room.id, "hello")).thenReturn(savedMessage)
        whenever(objectMapper.readValue("auth", RawSocketRequest::class.java))
            .thenReturn(RawSocketRequest(type = "AUTH", token = "token"))
        whenever(objectMapper.readValue("join", RawSocketRequest::class.java))
            .thenReturn(RawSocketRequest(type = "JOIN_ROOM", roomId = room.id))
        whenever(objectMapper.readValue("send", RawSocketRequest::class.java))
            .thenReturn(RawSocketRequest(type = "SEND_MESSAGE", roomId = room.id, content = "hello"))

        handler.receive(session, "auth")
        handler.receive(session, "join")
        handler.receive(closedSession, "auth")
        handler.receive(closedSession, "join")
        handler.receive(session, "send")

        verify(closedSession, never()).sendMessage(any<TextMessage>())
        val responses = capturedResponses()
        assertEquals("NEW_MESSAGE", responses.last().type)
        assertEquals(savedMessage, responses.last().payload)
    }

    @Test
    fun `broadcast to room without joined sessions is a no-op`() {
        whenever(objectMapper.writeValueAsString(any<RawSocketResponse>())).thenReturn("json")
        val method = ChatWebSocketHandler::class.java.getDeclaredMethod(
            "broadcastToRoom",
            Long::class.javaPrimitiveType,
            RawSocketResponse::class.java
        )

        method.isAccessible = true
        method.invoke(handler, 999L, RawSocketResponse(type = "NEW_MESSAGE", roomId = 999L))

        verify(session, never()).sendMessage(any<TextMessage>())
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
        type = ChatRoomType.GLOBAL
    )

    private fun capturedResponses(): List<RawSocketResponse> {
        val responseCaptor = argumentCaptor<RawSocketResponse>()
        verify(objectMapper, atLeastOnce()).writeValueAsString(responseCaptor.capture())
        return responseCaptor.allValues
    }

    private class TestableChatWebSocketHandler(
        objectMapper: ObjectMapper,
        jwtService: JwtService,
        userRepository: UserRepository,
        userChatSubscriptionRepository: UserChatSubscriptionRepository,
        chatMessagingService: ChatMessagingServiceInterface
    ) : ChatWebSocketHandler(
        objectMapper,
        jwtService,
        userRepository,
        userChatSubscriptionRepository,
        chatMessagingService
    ) {
        fun receive(session: WebSocketSession, payload: String) {
            super.handleTextMessage(session, TextMessage(payload))
        }
    }
}
