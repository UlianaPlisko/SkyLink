package com.skylink.backend.dto.favorite

import jakarta.validation.constraints.NotNull

data class FavoriteRequest(
    @field:NotNull val spaceObjectId: Long,
    val note: String?,
    val visibility: Double? = 1.0
)