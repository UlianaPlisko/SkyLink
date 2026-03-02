package com.skylink.backend.repository

import com.skylink.backend.model.entity.Favorite
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface FavoriteRepository : JpaRepository<Favorite, Long> {
    fun findByUserId(userId: Long): List<Favorite>
    fun findByUserIdAndSpaceObjectId(userId: Long, spaceObjectId: Long): Optional<Favorite>
    fun deleteByUserIdAndSpaceObjectId(userId: Long, spaceObjectId: Long)
}