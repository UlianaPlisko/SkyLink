package com.skylink.backend.repository

import com.skylink.backend.model.entity.StarData
import org.springframework.data.jpa.repository.JpaRepository

interface StarDataRepository : JpaRepository<StarData, Long> {
    fun findBySpaceObjectId(spaceObjectId: Long): StarData?
}