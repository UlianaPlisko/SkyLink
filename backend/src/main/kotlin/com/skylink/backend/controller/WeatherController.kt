package com.skylink.backend.controller

import com.skylink.backend.dto.weather.CurrentVisibilityResponseDto
import com.skylink.backend.service.WeatherService
import com.skylink.backend.service.WeatherServiceInterface
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(
    name = "Weather",
    description = "Operations related to weather and visibility conditions"
)
@RestController
@RequestMapping("/api/weather")
class WeatherController(
    private val weatherService: WeatherServiceInterface
) {

    @Operation(summary = "Get current visibility conditions for a location")
    @GetMapping("/current-visibility")
    fun getCurrentVisibility(
        @RequestParam location: String
    ): CurrentVisibilityResponseDto {
        return weatherService.getCurrentVisibility(location)
    }
}