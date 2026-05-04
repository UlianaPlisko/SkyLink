package com.codepalace.accelerometer.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_messages",
    indices = [Index(value = ["roomId"])] // fast queries per room
)
data class ChatMessageEntity(
    @PrimaryKey val id: Long,
    val roomId: Long,
    val content: String,
    val userId: Long,
    val senderEmail: String,
    val createdAt: String   // ISO string from backend
)