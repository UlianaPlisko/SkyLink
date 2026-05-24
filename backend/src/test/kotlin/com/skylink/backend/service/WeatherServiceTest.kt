package com.skylink.backend.service

import com.skylink.backend.client.WeatherApiClient
import com.skylink.backend.dto.weather.WeatherApiConditionDto
import com.skylink.backend.dto.weather.WeatherApiCurrentDto
import com.skylink.backend.dto.weather.WeatherApiLocationDto
import com.skylink.backend.dto.weather.WeatherApiResponseDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class WeatherServiceTest {

    private val weatherApiClient: WeatherApiClient = mock()
    private val service = WeatherService(weatherApiClient)

    @Test
    fun `get current visibility maps weather response`() {
        whenever(weatherApiClient.getCurrentWeather("Bratislava")).thenReturn(
            WeatherApiResponseDto(
                location = WeatherApiLocationDto(name = "Bratislava", localtime = "2026-05-23 10:00"),
                current = WeatherApiCurrentDto(
                    visKm = 12.0,
                    cloud = 20,
                    humidity = 45,
                    precipMm = 0.0,
                    isDay = 0,
                    condition = WeatherApiConditionDto(text = "Clear", code = 1000)
                )
            )
        )

        val response = service.getCurrentVisibility("Bratislava")

        assertEquals("Bratislava", response.location)
        assertEquals("2026-05-23 10:00", response.localTime)
        assertEquals(12.0, response.visibilityKm)
        assertEquals(20, response.cloudPercent)
        assertEquals(45, response.humidity)
        assertEquals(0.0, response.precipitationMm)
        assertFalse(response.isDay)
        assertEquals("Clear", response.conditionText)
        assertEquals(1000, response.conditionCode)
    }

    @Test
    fun `get current visibility falls back to requested location name`() {
        whenever(weatherApiClient.getCurrentWeather("Unknown")).thenReturn(
            WeatherApiResponseDto(
                location = WeatherApiLocationDto(name = null, localtime = null),
                current = WeatherApiCurrentDto(
                    visKm = null,
                    cloud = null,
                    humidity = null,
                    precipMm = null,
                    isDay = 1,
                    condition = null
                )
            )
        )

        val response = service.getCurrentVisibility("Unknown")

        assertEquals("Unknown", response.location)
        assertEquals(true, response.isDay)
    }

    @Test
    fun `get current visibility maps null condition and night state`() {
        whenever(weatherApiClient.getCurrentWeather("Night")).thenReturn(
            WeatherApiResponseDto(
                location = WeatherApiLocationDto(name = "Night", localtime = "2026-05-23 22:00"),
                current = WeatherApiCurrentDto(
                    visKm = 5.0,
                    cloud = 80,
                    humidity = 70,
                    precipMm = 0.1,
                    isDay = 0,
                    condition = null
                )
            )
        )

        val response = service.getCurrentVisibility("Night")

        assertFalse(response.isDay)
        assertEquals(null, response.conditionText)
        assertEquals(null, response.conditionCode)
    }

    @Test
    fun `get current visibility treats null day flag as night`() {
        whenever(weatherApiClient.getCurrentWeather("Null day")).thenReturn(
            WeatherApiResponseDto(
                location = WeatherApiLocationDto(name = "Null day", localtime = "2026-05-23 22:00"),
                current = WeatherApiCurrentDto(
                    visKm = 5.0,
                    cloud = 80,
                    humidity = 70,
                    precipMm = 0.1,
                    isDay = null,
                    condition = WeatherApiConditionDto(text = null, code = null)
                )
            )
        )

        assertFalse(service.getCurrentVisibility("Null day").isDay)
    }

    @Test
    fun `get current visibility throws when location data is missing`() {
        whenever(weatherApiClient.getCurrentWeather("Nowhere")).thenReturn(
            WeatherApiResponseDto(location = null, current = null)
        )

        assertThrows(RuntimeException::class.java) {
            service.getCurrentVisibility("Nowhere")
        }
    }

    @Test
    fun `get current visibility throws when current data is missing`() {
        whenever(weatherApiClient.getCurrentWeather("Nowhere")).thenReturn(
            WeatherApiResponseDto(location = WeatherApiLocationDto("Nowhere", null), current = null)
        )

        assertThrows(RuntimeException::class.java) {
            service.getCurrentVisibility("Nowhere")
        }
    }
}
