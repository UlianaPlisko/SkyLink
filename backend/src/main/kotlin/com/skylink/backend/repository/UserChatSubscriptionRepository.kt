package com.skylink.backend.repository

import com.skylink.backend.model.UserChatSubscriptionId
import com.skylink.backend.model.entity.UserChatSubscription
import org.springframework.data.jpa.repository.JpaRepository

interface UserChatSubscriptionRepository :
    JpaRepository<UserChatSubscription, UserChatSubscriptionId> {

    fun findByChatRoomId(chatRoomId: Long): List<UserChatSubscription>
    fun findByUserId(userId: Long): List<UserChatSubscription>
    fun existsByUserIdAndChatRoomId(userId: Long, chatRoomId: Long): Boolean
    fun findByUserIdAndChatRoomId(userId: Long, chatRoomId: Long): UserChatSubscription?
    fun deleteByUserIdAndChatRoomId(userId: Long, chatRoomId: Long)
}