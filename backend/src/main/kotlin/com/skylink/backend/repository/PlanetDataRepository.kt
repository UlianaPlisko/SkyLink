package com.skylink.backend.repository

import com.skylink.backend.model.entity.PlanetData
import org.springframework.data.jpa.repository.JpaRepository

interface PlanetDataRepository : JpaRepository<PlanetData, Long> {

    fun findBySpaceObjectId(spaceObjectId: Long): PlanetData?
}