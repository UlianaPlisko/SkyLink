package com.codepalace.accelerometer.data.model

data class SpaceObjectDetail(
    val id: Long,
    val displayName: String,
    val magnitude: Double? = null,
    val raDeg: Double? = null,
    val decDeg: Double? = null,
    val constellation: String? = null,
    val spectralType: String? = null,
    val distanceLy: Double? = null
)