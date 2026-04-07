package com.skylink.backend.dto.chat


import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ChatMessageSendRequest(
    @field:NotBlank
    @field:Size(max = 5000)
    val content: String
)