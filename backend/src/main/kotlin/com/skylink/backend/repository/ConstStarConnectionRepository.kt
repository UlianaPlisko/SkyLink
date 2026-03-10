package com.skylink.backend.repository

import com.skylink.backend.model.entity.ConstStarConnection
import com.skylink.backend.model.ConstStarConnectionId
import org.springframework.data.jpa.repository.JpaRepository

interface ConstStarConnectionRepository : JpaRepository<ConstStarConnection, ConstStarConnectionId> {

    fun findByConstellationId(constellationId: Long): List<ConstStarConnection>
}