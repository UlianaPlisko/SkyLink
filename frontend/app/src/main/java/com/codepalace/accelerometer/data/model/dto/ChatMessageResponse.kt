package com.codepalace.accelerometer.data.model.dto

data class ChatMessageResponse(
    val id: Long,
    val roomId: Long,
    val userId: Long,
    val senderEmail: String,
    val content: String,
    val createdAt: String,   // ISO format from backend
    val editedAt: String? = null
)