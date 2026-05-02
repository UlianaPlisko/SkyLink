package com.skylink.backend.repository

import com.skylink.backend.model.EventParticipantId
import com.skylink.backend.model.entity.EventParticipant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface EventParticipantRepository : JpaRepository<EventParticipant, EventParticipantId> {

    fun existsByIdEventIdAndIdUserId(eventId: Long, userId: Long): Boolean
    fun findByIdEventIdAndIdUserId(eventId: Long, userId: Long): EventParticipant?

    // Add this
    fun countByIdEventId(eventId: Long): Long

    @Query("""
    SELECT ep.id.eventId
    FROM EventParticipant ep
    WHERE ep.id.userId = :userId AND ep.id.eventId IN :eventIds
""")
    fun findParticipatedEventIds(
        @Param("userId") userId: Long,
        @Param("eventIds") eventIds: List<Long>
    ): List<Long>

    // Optional but recommended for better performance (avoids N+1)
    @Query("""
        SELECT ep.id.eventId as eventId, COUNT(ep) as participantCount 
        FROM EventParticipant ep 
        WHERE ep.id.eventId IN :eventIds 
        GROUP BY ep.id.eventId
    """)
    fun countParticipantsByEventIds(@Param("eventIds") eventIds: List<Long>): List<ParticipantCountProjection>
}

// Add this projection interface (create a new file or put it in the repository file)
interface ParticipantCountProjection {
    fun getEventId(): Long
    fun getParticipantCount(): Long
}