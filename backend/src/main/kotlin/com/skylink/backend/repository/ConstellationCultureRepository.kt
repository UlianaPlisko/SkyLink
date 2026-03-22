package com.skylink.backend.repository

import com.skylink.backend.model.entity.ConstellationCulture
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ConstellationCultureRepository : JpaRepository<ConstellationCulture, Long> {

    /**
     * Sets isCurrent = false for ALL cultures in one query.
     * Used before activating a new culture.
     */
    @Modifying
    @Query("UPDATE ConstellationCulture c SET c.isCurrent = false")
    fun setAllToNotCurrent()

    /**
     * Sets isCurrent = true for the given culture.
     * Only one culture should ever be current.
     */
    @Modifying
    @Query("UPDATE ConstellationCulture c SET c.isCurrent = true WHERE c.id = :id")
    fun setCurrent(@Param("id") id: Long)
}