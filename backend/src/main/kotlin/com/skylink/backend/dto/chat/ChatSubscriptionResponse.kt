package com.skylink.backend.dto.chat

import java.time.Instant

data class ChatSubscriptionResponse(
    val userId: Long,
    val chatRoomId: Long,
    val addedAt: Instant,
    val lastReadMsgId: Long?
)