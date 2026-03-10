package com.skylink.backend.repository

import com.skylink.backend.model.entity.DeepSkyObjectData
import org.springframework.data.jpa.repository.JpaRepository

interface DeepSkyObjectDataRepository : JpaRepository<DeepSkyObjectData, Long> {

    fun findBySpaceObjectId(spaceObjectId: Long): DeepSkyObjectData?
}