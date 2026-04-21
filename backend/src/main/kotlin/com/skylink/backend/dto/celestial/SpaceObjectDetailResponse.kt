package com.skylink.backend.dto.celestial

interface SpaceObjectDetailResponse {
    val id: Long
    val displayName: String
    val magnitude: Double?
    val raDeg: Double
    val decDeg: Double
    val description: String?
    val wikiSummary: String?
    val wikiUrl: String?
}