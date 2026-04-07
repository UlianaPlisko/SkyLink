package com.skylink.backend.service

import com.skylink.backend.dto.chat.ChatMessageResponse

interface ChatMessagingServiceInterface {
    fun sendMessage(currentUserEmail: String, roomId: Long, content: String): ChatMessageResponse
}