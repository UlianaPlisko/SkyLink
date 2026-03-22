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

    /**
     * Fetches ALL constellations that belong to the CURRENT active culture
     * (isCurrent = true) in ONE query with JOIN FETCH.
     */
    @Query("SELECT c FROM Constellation c LEFT JOIN FETCH c.culture WHERE c.culture.isCurrent = true")
    fun findAllWithCurrentCulture(): List<Constellation>

    /**
     * Fetches ONE constellation + its culture ONLY if that culture is currently active.
     * Used by the new "current culture" detail endpoint.
     */
    @Query("SELECT c FROM Constellation c LEFT JOIN FETCH c.culture WHERE c.id = :id AND c.culture.isCurrent = true")
    fun findByIdWithCurrentCulture(id: Long): Optional<Constellation>
}