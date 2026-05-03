package com.codepalace.accelerometer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_rooms")
data class ChatRoomEntity(
    @PrimaryKey val id: Long,
    val userId: Long,                    // ← added for per-user isolation
    val name: String,
    val type: String,
    val regionGeom: String? = null,
    val eventId: Long? = null,
    val createdBy: Long? = null,
    val createdAt: String? = null,
    val isSubscribed: Boolean = false,
    val unreadCount: Int = 0
)