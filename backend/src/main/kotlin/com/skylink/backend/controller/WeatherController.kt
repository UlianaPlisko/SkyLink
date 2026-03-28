package com.skylink.backend.controller

import com.skylink.backend.dto.weather.CurrentVisibilityResponseDto
import com.skylink.backend.service.WeatherService
import com.skylink.backend.service.WeatherServiceInterface
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/weather")
class WeatherController(
    private val weatherService: WeatherServiceInterface
) {

    @GetMapping("/current-visibility")
    fun getCurrentVisibility(
        @RequestParam location: String
    ): CurrentVisibilityResponseDto {
        return weatherService.getCurrentVisibility(location)
    }
}