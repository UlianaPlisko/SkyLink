package com.skylink.backend.model.entity

import com.skylink.backend.model.UserChatSubscriptionId
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "\"UserChatSubscription\"")
data class UserChatSubscription(
    @EmbeddedId
    val id: UserChatSubscriptionId = UserChatSubscriptionId(),

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("chatRoomId")
    @JoinColumn(name = "chat_room_id")
    val chatRoom: ChatRoom,

    @Column(name = "added_at", nullable = false)
    val addedAt: Instant = Instant.now(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_read_msg_id")
    var lastReadMessage: ChatMessage? = null
)