package com.skylink.backend.service

import com.skylink.backend.client.WeatherApiClient
import com.skylink.backend.dto.weather.CurrentVisibilityResponseDto
import org.springframework.stereotype.Service

@Service
class WeatherService(
    private val weatherApiClient: WeatherApiClient
) : WeatherServiceInterface {

    override fun getCurrentVisibility(location: String): CurrentVisibilityResponseDto {
        val response = weatherApiClient.getCurrentWeather(location)

        val locationData = response.location
            ?: throw RuntimeException("Missing location data in WeatherAPI response.")

        val currentData = response.current
            ?: throw RuntimeException("Missing current weather data in WeatherAPI response.")

        return CurrentVisibilityResponseDto(
            location = locationData.name ?: location,
            localTime = locationData.localtime,
            visibilityKm = currentData.visKm,
            cloudPercent = currentData.cloud,
            humidity = currentData.humidity,
            precipitationMm = currentData.precipMm,
            isDay = currentData.isDay == 1,
            conditionText = currentData.condition?.text,
            conditionCode = currentData.condition?.code
        )
    }
}