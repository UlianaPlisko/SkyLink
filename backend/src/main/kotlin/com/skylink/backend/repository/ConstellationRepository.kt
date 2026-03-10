package com.skylink.backend.repository

import com.skylink.backend.model.entity.Constellation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional

interface ConstellationRepository : JpaRepository<Constellation, Long> {

    /**
     * Fetches all constellations with their culture in ONE query (JOIN FETCH).
     * Prevents N+1 lazy loading problems.
     */
    @Query("SELECT c FROM Constellation c LEFT JOIN FETCH c.culture")
    fun findAllWithCulture(): List<Constellation>

    /**
     * Fetches a single constellation with its culture in ONE query.
     * Used in constellation detail screen.
     */
    @Query("SELECT c FROM Constellation c LEFT JOIN FETCH c.culture WHERE c.id = :id")
    fun findByIdWithCulture(id: Long): Optional<Constellation>
}