package com.skylink.backend.config

import tools.jackson.databind.ObjectMapper
import com.skylink.backend.dto.chat.RawSocketRequest
import com.skylink.backend.dto.chat.RawSocketResponse
import com.skylink.backend.repository.UserChatSubscriptionRepository
import com.skylink.backend.repository.UserRepository
import com.skylink.backend.service.ChatMessagingServiceInterface
import com.skylink.backend.service.JwtService
import io.jsonwebtoken.JwtException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.concurrent.ConcurrentHashMap

@Component
class ChatWebSocketHandler(
    private val objectMapper: ObjectMapper,
    private val jwtService: JwtService,
    private val userRepository: UserRepository,
    private val userChatSubscriptionRepository: UserChatSubscriptionRepository,
    private val chatMessagingService: ChatMessagingServiceInterface
) : TextWebSocketHandler() {

    private val authenticatedUsers = ConcurrentHashMap<String, String>()
    private val roomSessions = ConcurrentHashMap<Long, MutableSet<WebSocketSession>>()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        sendToSession(
            session,
            RawSocketResponse(
                type = "CONNECTED",
                message = "WebSocket connected. Please authenticate."
            )
        )
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        try {
            val request = objectMapper.readValue(message.payload, RawSocketRequest::class.java)

            when (request.type.uppercase()) {
                "AUTH" -> handleAuth(session, request)
                "JOIN_ROOM" -> handleJoinRoom(session, request)
                "LEAVE_ROOM" -> handleLeaveRoom(session, request)
                "SEND_MESSAGE" -> handleSendMessage(session, request)
                else -> sendError(session, "Unsupported message type: ${request.type}")
            }
        } catch (ex: Exception) {
            sendError(session, ex.message ?: "Invalid request")
        }
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        authenticatedUsers.remove(session.id)
        roomSessions.values.forEach { it.remove(session) }
    }

    private fun handleAuth(session: WebSocketSession, request: RawSocketRequest) {
        val token = request.token?.trim()
            ?: throw BadCredentialsException("Missing token")

        try {
            val email = jwtService.extractEmail(token)

            if (!jwtService.validateToken(token, email)) {
                throw BadCredentialsException("Invalid JWT token")
            }

            userRepository.findByEmail(email)
                .orElseThrow { BadCredentialsException("User not found") }

            authenticatedUsers[session.id] = email

            sendToSession(
                session,
                RawSocketResponse(
                    type = "AUTH_OK",
                    email = email,
                    message = "Authenticated successfully"
                )
            )
        } catch (ex: JwtException) {
            throw BadCredentialsException("Invalid or expired JWT token")
        }
    }

    private fun handleJoinRoom(session: WebSocketSession, request: RawSocketRequest) {
        val email = requireAuthenticated(session)
        val roomId = request.roomId ?: throw IllegalArgumentException("roomId is required")

        val user = userRepository.findByEmail(email)
            .orElseThrow { IllegalArgumentException("User not found") }

        val isSubscribed = userChatSubscriptionRepository
            .findByUserIdAndChatRoomId(user.id, roomId) != null

        if (!isSubscribed) {
            throw IllegalAccessException("User is not subscribed to room $roomId")
        }

        roomSet(roomId).add(session)

        sendToSession(
            session,
            RawSocketResponse(
                type = "JOINED_ROOM",
                roomId = roomId,
                message = "Joined room $roomId"
            )
        )
    }

    private fun handleLeaveRoom(session: WebSocketSession, request: RawSocketRequest) {
        val roomId = request.roomId ?: throw IllegalArgumentException("roomId is required")
        roomSessions[roomId]?.remove(session)

        sendToSession(
            session,
            RawSocketResponse(
                type = "LEFT_ROOM",
                roomId = roomId,
                message = "Left room $roomId"
            )
        )
    }

    private fun handleSendMessage(session: WebSocketSession, request: RawSocketRequest) {
        val email = requireAuthenticated(session)
        val roomId = request.roomId ?: throw IllegalArgumentException("roomId is required")
        val content = request.content ?: throw IllegalArgumentException("content is required")

        val joinedSessions = roomSessions[roomId]
        if (joinedSessions == null || !joinedSessions.contains(session)) {
            throw IllegalAccessException("You must join room $roomId before sending messages")
        }

        val savedMessage = chatMessagingService.sendMessage(
            currentUserEmail = email,
            roomId = roomId,
            content = content
        )

        broadcastToRoom(
            roomId,
            RawSocketResponse(
                type = "NEW_MESSAGE",
                roomId = roomId,
                payload = savedMessage
            )
        )
    }

    private fun requireAuthenticated(session: WebSocketSession): String {
        return authenticatedUsers[session.id]
            ?: throw BadCredentialsException("WebSocket session is not authenticated")
    }

    private fun roomSet(roomId: Long): MutableSet<WebSocketSession> {
        return roomSessions.computeIfAbsent(roomId) {
            ConcurrentHashMap.newKeySet<WebSocketSession>()
        }
    }

    private fun broadcastToRoom(roomId: Long, response: RawSocketResponse) {
        val json = objectMapper.writeValueAsString(response)
        roomSessions[roomId]?.forEach { session ->
            if (session.isOpen) {
                session.sendMessage(TextMessage(json))
            }
        }
    }

    private fun sendToSession(session: WebSocketSession, response: RawSocketResponse) {
        if (!session.isOpen) return
        val json = objectMapper.writeValueAsString(response)
        session.sendMessage(TextMessage(json))
    }

    private fun sendError(session: WebSocketSession, errorMessage: String) {
        sendToSession(
            session,
            RawSocketResponse(
                type = "ERROR",
                message = errorMessage
            )
        )
    }
}