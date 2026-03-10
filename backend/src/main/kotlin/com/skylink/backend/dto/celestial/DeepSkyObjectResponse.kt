package com.skylink.backend.dto.celestial

data class DeepSkyObjectResponse(
    val id: Long,
    val displayName: String,
    val magnitude: Double?,
    val raDeg: Double,
    val decDeg: Double,
    val catalogId: String?,
    val objectClass: String,
    val angularSize: Double?
)