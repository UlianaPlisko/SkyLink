package com.codepalace.accelerometer.api.dto

data class FavoriteUpdateRequest(
    val note: String?,
    val visibility: Double? = null
)
