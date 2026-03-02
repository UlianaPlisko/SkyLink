package com.skylink.backend.repository

import com.skylink.backend.model.entity.SpaceObject
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface SpaceObjectRepository : JpaRepository<SpaceObject, Long> {
    @Query("SELECT s FROM SpaceObject s WHERE s.magnitude <= :maxMagnitude ORDER BY s.magnitude")
    fun findBrightStars(maxMagnitude: Double): List<SpaceObject>
}