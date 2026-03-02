package com.skylink.backend.dto.favorite

import com.skylink.backend.dto.celestial.SpaceObjectSummary
import java.time.Instant

data class FavoriteResponse(
    val id: Long,
    val spaceObject: SpaceObjectSummary,
    val note: String?,
    val visibility: Double,
    val addedAt: Instant
)