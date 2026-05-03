package com.skylink.backend.dto.favorite

data class FavoriteUpdateRequest(
    val note: String? = null,
    val visibility: Double? = null
)
