package com.skylink.backend.dto.celestial

import java.time.Instant

data class PlanetResponse(
    val id: Long,
    val displayName: String,
    val magnitude: Double?,
    val raDeg: Double,
    val decDeg: Double,
    val orbitalModel: String,
    val lastComputed: Instant
)