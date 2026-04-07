package com.skylink.backend.dto.chat

import com.skylink.backend.model.enums.ChatRoomType
import java.time.Instant

data class ChatRoomResponse(
    val id: Long,
    val name: String,
    val type: ChatRoomType,
    val regionGeom: String?,
    val eventId: Long?,
    val createdBy: Long?,
    val createdAt: Instant
)
