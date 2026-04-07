package com.skylink.backend.dto.chat

import com.skylink.backend.model.enums.ChatRoomType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UpdateChatRoomRequest(
    @field:NotBlank
    @field:Size(max = 255)
    val name: String,

    val type: ChatRoomType,
    val regionGeom: String? = null,
    val eventId: Long? = null
)