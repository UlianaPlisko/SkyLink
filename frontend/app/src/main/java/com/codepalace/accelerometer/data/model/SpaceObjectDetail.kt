package com.codepalace.accelerometer.data.model

data class SpaceObjectDetail(
    val id: Long,
    val displayName: String,
    val magnitude: Double?,
    val raDeg: Double,
    val decDeg: Double,
    val description: String?,
    val wikiSummary: String?,
    val wikiUrl: String?,
    val imageUrl: String? = null,

    val constellation: String? = null,
    val spectralType: String? = null,
    val distanceLy: Double? = null,

    val orbitalModel: String? = null,
    val lastComputed: String? = null,

    val catalogId: String? = null,
    val objectClass: String? = null,
    val angularSize: Double? = null
)