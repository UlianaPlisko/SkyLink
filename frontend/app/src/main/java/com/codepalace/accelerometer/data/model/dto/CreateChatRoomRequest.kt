package com.codepalace.accelerometer.data.model.dto

import com.codepalace.accelerometer.data.model.enums.ChatRoomType

data class CreateChatRoomRequest(
    val name: String,
    val type: ChatRoomType,
    val regionGeom: String? = null,
    val eventId: Long? = null
)