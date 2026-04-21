package com.skylink.backend.dto.celestial

data class DeepSkyObjectResponse(
    override val id: Long,
    override val displayName: String,
    override val magnitude: Double?,
    override val raDeg: Double,
    override val decDeg: Double,
    override val description: String?,
    override val wikiSummary: String? = null,
    override val wikiUrl: String? = null,
    val catalogId: String?,
    val objectClass: String?,
    val angularSize: Double?
) : SpaceObjectDetailResponse