package com.skylink.backend.service

import com.skylink.backend.dto.chat.ChatMessageResponse
import com.skylink.backend.dto.chat.ChatRoomResponse
import com.skylink.backend.dto.chat.ChatSubscriptionResponse
import com.skylink.backend.dto.chat.CreateChatRoomRequest
import com.skylink.backend.dto.chat.UpdateChatRoomRequest
import com.skylink.backend.dto.chat.UpdateReadStateRequest
import com.skylink.backend.model.UserChatSubscriptionId
import com.skylink.backend.model.entity.ChatMessage
import com.skylink.backend.model.entity.ChatRoom
import com.skylink.backend.model.entity.User
import com.skylink.backend.model.entity.UserChatSubscription
import com.skylink.backend.model.enums.ChatRoomType
import com.skylink.backend.repository.ChatMessageRepository
import com.skylink.backend.repository.ChatRoomRepository
import com.skylink.backend.repository.UserChatSubscriptionRepository
import com.skylink.backend.repository.UserRepository
import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class ChatRoomService(
    private val chatRoomRepository: ChatRoomRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val userChatSubscriptionRepository: UserChatSubscriptionRepository,
    private val userRepository: UserRepository
) : ChatRoomServiceInterface {

    @Transactional
    override fun createRoom(currentUserEmail: String, request: CreateChatRoomRequest): ChatRoomResponse {
        validateRoomRequest(
            type = request.type,
            eventId = request.eventId,
            regionGeom = request.regionGeom
        )

        if (request.type == ChatRoomType.EVENT && request.eventId != null) {
            if (chatRoomRepository.existsByEventId(request.eventId)) {
                throw IllegalArgumentException("Event chat room already exists for eventId=${request.eventId}")
            }
        }

        val currentUser = findUserByEmailOrThrow(currentUserEmail)

        val chatRoom = ChatRoom(
            name = request.name.trim(),
            type = request.type,
            regionGeom = request.regionGeom,
            eventId = request.eventId,
            createdBy = currentUser.id
        )

        val savedRoom = chatRoomRepository.save(chatRoom)

        val creatorSubscription = UserChatSubscription(
            id = UserChatSubscriptionId(
                userId = currentUser.id,
                chatRoomId = savedRoom.id
            ),
            user = currentUser,
            chatRoom = savedRoom,
            lastReadMessage = null
        )

        userChatSubscriptionRepository.save(creatorSubscription)

        return savedRoom.toResponseDto()
    }

    @Transactional
    override fun getAllRooms(type: ChatRoomType?): List<ChatRoomResponse> {
        val rooms = if (type != null) {
            chatRoomRepository.findByType(type)
        } else {
            chatRoomRepository.findAll()
        }

        return rooms.map { it.toResponseDto() }
    }

    @Transactional
    override fun getRoomById(roomId: Long): ChatRoomResponse {
        val room = findRoomOrThrow(roomId)
        return room.toResponseDto()
    }

    @Transactional
    override fun updateRoom(roomId: Long, request: UpdateChatRoomRequest): ChatRoomResponse {
        val room = findRoomOrThrow(roomId)

        validateRoomRequest(
            type = request.type,
            eventId = request.eventId,
            regionGeom = request.regionGeom
        )

        if (request.type == ChatRoomType.EVENT && request.eventId != null) {
            val existing = chatRoomRepository.findByEventId(request.eventId)
            if (existing != null && existing.id != roomId) {
                throw IllegalArgumentException("Another event room already exists for eventId=${request.eventId}")
            }
        }

        room.name = request.name.trim()
        room.type = request.type
        room.regionGeom = request.regionGeom
        room.eventId = request.eventId

        return chatRoomRepository.save(room).toResponseDto()
    }

    @Transactional
    override fun deleteRoom(roomId: Long) {
        val room = findRoomOrThrow(roomId)
        chatRoomRepository.delete(room)
    }

    @Transactional
    override fun subscribeCurrentUser(currentUserEmail: String, roomId: Long): ChatSubscriptionResponse {
        val room = findRoomOrThrow(roomId)
        val user = findUserByEmailOrThrow(currentUserEmail)

        val existing = userChatSubscriptionRepository
            .findByUserIdAndChatRoomId(user.id, roomId)

        if (existing != null) {
            return existing.toResponseDto()
        }

        val subscription = UserChatSubscription(
            id = UserChatSubscriptionId(
                userId = user.id,
                chatRoomId = room.id
            ),
            user = user,
            chatRoom = room,
            lastReadMessage = null
        )

        val saved = userChatSubscriptionRepository.save(subscription)
        return saved.toResponseDto()
    }

    @Transactional
    override fun unsubscribeCurrentUser(currentUserEmail: String, roomId: Long) {
        val user = findUserByEmailOrThrow(currentUserEmail)
        findRoomOrThrow(roomId)

        val existing = userChatSubscriptionRepository
            .findByUserIdAndChatRoomId(user.id, roomId)
            ?: throw EntityNotFoundException("Subscription not found for current user and roomId=$roomId")

        userChatSubscriptionRepository.delete(existing)
    }

    @Transactional
    override fun getRoomSubscriptions(roomId: Long): List<ChatSubscriptionResponse> {
        findRoomOrThrow(roomId)
        return userChatSubscriptionRepository.findByChatRoomId(roomId)
            .map { it.toResponseDto() }
    }

    @Transactional
    override fun getUserSubscribedRooms(currentUserEmail: String): List<ChatRoomResponse> {
        val user = findUserByEmailOrThrow(currentUserEmail)
        val subscriptions = userChatSubscriptionRepository.findByUserId(user.id)

        if (subscriptions.isEmpty()) return emptyList()

        return subscriptions.map { it.chatRoom.toResponseDto() }
    }

    @Transactional
    override fun getRoomMessages(roomId: Long): List<ChatMessageResponse> {
        findRoomOrThrow(roomId)
        return chatMessageRepository.findByRoomIdOrderByCreatedAtAsc(roomId)
            .map { it.toResponseDto() }
    }

    @Transactional
    override fun getLatestMessage(roomId: Long): ChatMessageResponse? {
        findRoomOrThrow(roomId)
        return chatMessageRepository.findTop1ByRoomIdOrderByCreatedAtDesc(roomId)
            ?.toResponseDto()
    }

    @Transactional
    override fun updateReadState(
        currentUserEmail: String,
        roomId: Long,
        request: UpdateReadStateRequest
    ): ChatSubscriptionResponse {
        findRoomOrThrow(roomId)
        val user = findUserByEmailOrThrow(currentUserEmail)

        val subscription = userChatSubscriptionRepository
            .findByUserIdAndChatRoomId(user.id, roomId)
            ?: throw EntityNotFoundException("Subscription not found for current user and roomId=$roomId")

        val msg = chatMessageRepository.findById(request.lastReadMsgId)
            .orElseThrow {
                EntityNotFoundException("ChatMessage not found with id=${request.lastReadMsgId}")
            }

        if (msg.room.id != roomId) {
            throw IllegalArgumentException("Message ${request.lastReadMsgId} does not belong to room $roomId")
        }

        subscription.lastReadMessage = msg

        val saved = userChatSubscriptionRepository.save(subscription)
        return saved.toResponseDto()
    }

    private fun findRoomOrThrow(roomId: Long): ChatRoom {
        return chatRoomRepository.findById(roomId)
            .orElseThrow { EntityNotFoundException("ChatRoom not found with id=$roomId") }
    }

    private fun findUserByEmailOrThrow(email: String): User {
        return userRepository.findByEmail(email)
            .orElseThrow { EntityNotFoundException("User not found with email=$email") }
    }


    private fun ChatRoom.toResponseDto(): ChatRoomResponse {
        return ChatRoomResponse(
            id = this.id,
            name = this.name,
            type = this.type,
            regionGeom = this.regionGeom,
            eventId = this.eventId,
            createdBy = this.createdBy,
            createdAt = this.createdAt
        )
    }

    private fun validateRoomRequest(
        type: ChatRoomType,
        eventId: Long?,
        regionGeom: String?
    ) {
        when (type) {
            ChatRoomType.EVENT -> {
                require(eventId != null) { "eventId is required for EVENT room" }
            }
            ChatRoomType.REGIONAL -> {
                // require(!regionGeom.isNullOrBlank()) { "regionGeom is required for REGIONAL room" }
            }
            ChatRoomType.GLOBAL -> {
                // no special requirement
            }
        }
    }

    private fun ChatMessage.toResponseDto(): ChatMessageResponse {
        return ChatMessageResponse(
            id = this.id,
            roomId = this.room.id,
            userId = this.user.id,
            senderEmail = this.user.email,
            content = this.content,
            createdAt = this.createdAt,
            editedAt = this.editedAt
        )
    }

    private fun UserChatSubscription.toResponseDto(): ChatSubscriptionResponse {
        return ChatSubscriptionResponse(
            userId = this.user.id,
            chatRoomId = this.chatRoom.id,
            addedAt = this.addedAt,
            lastReadMsgId = this.lastReadMessage?.id
        )
    }
}