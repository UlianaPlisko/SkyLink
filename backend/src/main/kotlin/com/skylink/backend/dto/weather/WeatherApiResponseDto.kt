package com.skylink.backend.dto.weather

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class WeatherApiResponseDto(
    val location: WeatherApiLocationDto?,
    val current: WeatherApiCurrentDto?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class WeatherApiLocationDto(
    val name: String?,
    val localtime: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class WeatherApiCurrentDto(
    @JsonProperty("vis_km")
    val visKm: Double?,

    val cloud: Int?,
    val humidity: Int?,

    @JsonProperty("precip_mm")
    val precipMm: Double?,

    @JsonProperty("is_day")
    val isDay: Int?,

    val condition: WeatherApiConditionDto?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class WeatherApiConditionDto(
    val text: String?,
    val code: Int?
)