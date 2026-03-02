package com.skylink.backend.dto.celestial

data class SpaceObjectSummary(
    val id: Long,
    val displayName: String,
    val magnitude: Double?,
    val objectType: String
)