package com.skylink.backend.dto.chat

data class RawSocketRequest(
    val type: String,
    val token: String? = null,
    val roomId: Long? = null,
    val content: String? = null
)