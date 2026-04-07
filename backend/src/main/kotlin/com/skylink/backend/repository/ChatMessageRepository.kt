package com.skylink.backend.repository

import com.skylink.backend.model.entity.ChatMessage
import org.springframework.data.jpa.repository.JpaRepository

interface ChatMessageRepository : JpaRepository<ChatMessage, Long> {
    fun findByRoomIdOrderByCreatedAtAsc(roomId: Long): List<ChatMessage>
    fun findTop50ByRoomIdOrderByCreatedAtDesc(roomId: Long): List<ChatMessage>
    fun findTop1ByRoomIdOrderByCreatedAtDesc(roomId: Long): ChatMessage?
}