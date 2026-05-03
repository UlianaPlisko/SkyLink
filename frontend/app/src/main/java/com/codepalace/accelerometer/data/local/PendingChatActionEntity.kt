package com.codepalace.accelerometer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_chat_actions")
data class PendingChatActionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val roomId: Long,
    val action: String, // "SUBSCRIBE" or "UNSUBSCRIBE"
    val timestamp: Long = System.currentTimeMillis()
)