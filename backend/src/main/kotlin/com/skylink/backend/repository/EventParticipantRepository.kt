package com.skylink.backend.repository

import com.skylink.backend.model.EventParticipantId
import com.skylink.backend.model.entity.EventParticipant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface EventParticipantRepository : JpaRepository<EventParticipant, EventParticipantId> {

    fun existsByIdEventIdAndIdUserId(eventId: Long, userId: Long): Boolean

    fun findByIdEventIdAndIdUserId(eventId: Long, userId: Long): EventParticipant?
}