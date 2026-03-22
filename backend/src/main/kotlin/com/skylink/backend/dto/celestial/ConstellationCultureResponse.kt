package com.skylink.backend.dto.celestial

data class ConstellationCultureResponse(
    val id: Long,
    val name: String,
    val region: String,
    val description: String?,
    val isCurrent: Boolean
)