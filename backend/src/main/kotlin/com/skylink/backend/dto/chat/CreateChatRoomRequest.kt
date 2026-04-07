package com.skylink.backend.dto.chat

import com.skylink.backend.model.enums.ChatRoomType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size


data class CreateChatRoomRequest(
    @field:NotBlank(message = "Room name must not be blank.")
    @field:Size(max = 255, message = "Room name must be at most 255 characters.")
    val name: String,

    @field:NotNull(message = "Room type is required.")
    val type: ChatRoomType,

    val regionGeom: String? = null,
    val eventId: Long? = null
)