package com.skylink.backend.dto.weather

data class CurrentVisibilityResponseDto(
    val location: String,
    val localTime: String?,
    val visibilityKm: Double?,
    val cloudPercent: Int?,
    val humidity: Int?,
    val precipitationMm: Double?,
    val isDay: Boolean,
    val conditionText: String?,
    val conditionCode: Int?
)