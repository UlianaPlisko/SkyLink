package com.skylink.backend.dto.celestial

data class ConstellationDetailResponse(
    val id: Long,
    val name: String,
    val cultureName: String,
    val region: String,
    val description: String?,
    val stars: List<SpaceObjectSummary>
)