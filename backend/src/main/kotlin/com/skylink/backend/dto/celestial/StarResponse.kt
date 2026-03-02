package com.skylink.backend.dto.celestial

data class StarResponse(
    val id: Long,
    val displayName: String,
    val magnitude: Double?,
    val raDeg: Double,
    val decDeg: Double,
    val constellation: String?,
    val spectralType: String?,
    val distanceLy: Double?
)