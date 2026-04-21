package com.skylink.backend.dto.celestial

import java.time.Instant

data class PlanetResponse(
    override val id: Long,
    override val displayName: String,
    override val magnitude: Double?,
    override val raDeg: Double,
    override val decDeg: Double,
    override val description: String?,
    override val wikiSummary: String? = null,
    override val wikiUrl: String? = null,
    val orbitalModel: String?,
    val lastComputed: Instant
) : SpaceObjectDetailResponse