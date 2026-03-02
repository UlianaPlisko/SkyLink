package com.skylink.backend.model

import jakarta.persistence.Embeddable
import java.io.Serializable

@Embeddable
data class UserChatSubscriptionId(
    val userId: Long = 0,
    val chatRoomId: Long = 0
) : Serializable