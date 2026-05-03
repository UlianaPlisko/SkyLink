package com.codepalace.accelerometer.data.model.dto

data class MessageUi(
    val id: Long,
    val content: String,
    val time: String,                    // HH:mm for display
    val isFromCurrentUser: Boolean,
    val senderDisplayName: String? = null,
    val createdAt: String                // ← NEW: full ISO timestamp from backend
)