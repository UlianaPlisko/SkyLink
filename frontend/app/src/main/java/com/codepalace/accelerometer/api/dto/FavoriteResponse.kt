package com.codepalace.accelerometer.api.dto

import com.codepalace.accelerometer.data.model.SpaceObjectSummary

data class FavoriteResponse(
    val id: Long,
    val spaceObject: SpaceObjectSummary,
    val note: String? = null,
    val visibility: Double,
    val addedAt: String? = null
)
