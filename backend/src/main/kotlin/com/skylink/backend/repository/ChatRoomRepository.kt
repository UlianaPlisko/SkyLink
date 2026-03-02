package com.skylink.backend.repository

import com.skylink.backend.model.entity.ChatRoom
import com.skylink.backend.model.enums.ChatRoomType
import org.springframework.data.jpa.repository.JpaRepository

interface ChatRoomRepository : JpaRepository<ChatRoom, Long> {
    fun findByType(type: ChatRoomType): List<ChatRoom>
}