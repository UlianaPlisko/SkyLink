package com.skylink.backend.dto.chat

import java.time.Instant

data class ChatMessageResponse(
    val id: Long?,
    val roomId: Long,
    val userId: Long,
    val senderEmail: String,
    val content: String,
    val createdAt: Instant,
    val editedAt: Instant?
)