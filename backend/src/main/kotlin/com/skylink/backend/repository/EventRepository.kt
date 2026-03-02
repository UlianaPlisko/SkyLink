package com.skylink.backend.repository

import com.skylink.backend.model.entity.Event
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface EventRepository : JpaRepository<Event, Long> {
    fun findByStartAtAfter(after: Instant): List<Event>
}