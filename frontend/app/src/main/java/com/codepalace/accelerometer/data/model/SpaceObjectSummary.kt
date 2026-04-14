package com.codepalace.accelerometer.data.model


data class SpaceObjectSummary(
    val id: Long,
    val displayName: String,
    val magnitude: Double,
    val objectType: String,
    val raDeg: Double,
    val decDeg: Double,
    val description: String? = null
)