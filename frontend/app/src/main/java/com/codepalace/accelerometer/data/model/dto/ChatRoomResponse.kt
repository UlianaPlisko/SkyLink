package com.codepalace.accelerometer.data.model.dto

data class ChatRoomResponse(
    val id: Long,
    val name: String,
    val type: String,
    val regionGeom: String? = null,
    val eventId: Long? = null,
    val createdBy: Long? = null,
    val createdAt: String? = null
)

data class ChatRoomUi(
    val id: Long,
    val name: String,
    val isSubscribed: Boolean,
    val unreadCount: Int = 0
)