package com.skylink.backend.dto.celestial

data class ConstellationResponse(
    val id: Long,
    val name: String,
    val cultureName: String,
    val region: String,
    val description: String?
)