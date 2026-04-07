package com.skylink.backend.dto.chat

data class RawSocketResponse(
    val type: String,
    val message: String? = null,
    val email: String? = null,
    val roomId: Long? = null,
    val payload: Any? = null
)