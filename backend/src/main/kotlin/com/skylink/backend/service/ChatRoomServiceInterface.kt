package com.skylink.backend.service

import com.skylink.backend.dto.chat.ChatMessageResponse
import com.skylink.backend.dto.chat.ChatRoomResponse
import com.skylink.backend.dto.chat.ChatSubscriptionResponse
import com.skylink.backend.dto.chat.CreateChatRoomRequest
import com.skylink.backend.dto.chat.UpdateChatRoomRequest
import com.skylink.backend.dto.chat.UpdateReadStateRequest
import com.skylink.backend.model.enums.ChatRoomType

interface ChatRoomServiceInterface {
    fun createRoom(currentUserEmail: String, request: CreateChatRoomRequest): ChatRoomResponse
    fun getAllRooms(type: ChatRoomType? = null): List<ChatRoomResponse>
    fun getRoomById(roomId: Long): ChatRoomResponse
    fun updateRoom(roomId: Long, request: UpdateChatRoomRequest): ChatRoomResponse
    fun deleteRoom(roomId: Long)

    fun subscribeCurrentUser(currentUserEmail: String, roomId: Long): ChatSubscriptionResponse
    fun unsubscribeCurrentUser(currentUserEmail: String, roomId: Long)
    fun getRoomSubscriptions(roomId: Long): List<ChatSubscriptionResponse>
    fun getUserSubscribedRooms(currentUserEmail: String): List<ChatRoomResponse>

    fun getRoomMessages(roomId: Long): List<ChatMessageResponse>
    fun getLatestMessage(roomId: Long): ChatMessageResponse?

    fun updateReadState(
        currentUserEmail: String,
        roomId: Long,
        request: UpdateReadStateRequest
    ): ChatSubscriptionResponse
}