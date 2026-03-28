package com.skylink.backend.service

import com.skylink.backend.dto.weather.CurrentVisibilityResponseDto

interface WeatherServiceInterface {
    fun getCurrentVisibility(location: String): CurrentVisibilityResponseDto
}