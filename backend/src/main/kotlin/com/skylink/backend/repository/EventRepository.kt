package com.skylink.backend.repository

import com.skylink.backend.model.entity.Event
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface EventRepository : JpaRepository<Event, Long> {

    fun findByStartAtAfter(after: Instant): List<Event>

    fun findByStartAtBetween(start: Instant, end: Instant): List<Event>

    @Query(
        value = """
            SELECT e.* 
            FROM "Events" e 
            INNER JOIN "EventParticipant" ep ON e.id = ep.event_id 
            WHERE ep.user_id = :userId 
              AND e.start_at >= :todayStart 
            ORDER BY e.start_at ASC
        """,
        nativeQuery = true
    )
    fun findUserEventsFromToday(@Param("userId") userId: Long, @Param("todayStart") todayStart: Instant): List<Event>
}