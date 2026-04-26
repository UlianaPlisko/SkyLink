package com.codepalace.accelerometer.api.dto

data class FavoriteRequest(
    val spaceObjectId: Long,
    val note: String? = null,
    val visibility: Double? = 1.0
)
