package com.skylink.backend.service

import com.skylink.backend.dto.chat.ChatMessageResponse
import com.skylink.backend.model.entity.ChatMessage
import com.skylink.backend.model.entity.ChatRoom
import com.skylink.backend.model.entity.User
import com.skylink.backend.repository.ChatMessageRepository
import com.skylink.backend.repository.ChatRoomRepository
import com.skylink.backend.repository.UserChatSubscriptionRepository
import com.skylink.backend.repository.UserRepository
import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service

@Service
class ChatMessagingService(
    private val chatMessageRepository: ChatMessageRepository,
    private val chatRoomRepository: ChatRoomRepository,
    private val userRepository: UserRepository,
    private val userChatSubscriptionRepository: UserChatSubscriptionRepository
) : ChatMessagingServiceInterface {

    @Transactional
    override fun sendMessage(
        currentUserEmail: String,
        roomId: Long,
        content: String
    ): ChatMessageResponse {
        val user = findUserByEmailOrThrow(currentUserEmail)
        val room = findRoomOrThrow(roomId)

        val isSubscribed = userChatSubscriptionRepository
            .findByUserIdAndChatRoomId(user.id, roomId) != null

        if (!isSubscribed) {
            throw AccessDeniedException("User is not subscribed to room $roomId")
        }

        val trimmed = content.trim()
        require(trimmed.isNotBlank()) { "Message content cannot be blank" }

        val saved = chatMessageRepository.save(
            ChatMessage(
                room = room,
                user = user,
                content = trimmed
            )
        )

        return saved.toBroadcastDto(user.email)
    }

    private fun findUserByEmailOrThrow(email: String): User {
        return userRepository.findByEmail(email)
            .orElseThrow { EntityNotFoundException("User not found with email=$email") }
    }

    private fun findRoomOrThrow(roomId: Long): ChatRoom {
        return chatRoomRepository.findById(roomId)
            .orElseThrow { EntityNotFoundException("ChatRoom not found with id=$roomId") }
    }

    private fun ChatMessage.toBroadcastDto(senderEmail: String): ChatMessageResponse {
        return ChatMessageResponse(
            id = id,
            roomId = room.id,
            userId = user.id,
            senderEmail = senderEmail,
            content = content,
            createdAt = createdAt,
            editedAt = editedAt
        )
    }
}