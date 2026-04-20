package com.codepalace.accelerometer.data.model

data class Star(
    val spaceObjectId: Long,
    val name: String,
    val raDegrees: Double,
    val decDegrees: Double,
    val magnitude: Double,
    val azimuth: Double,
    val altitude: Double,
    val east: Double,
    val north: Double,
    val up: Double
)