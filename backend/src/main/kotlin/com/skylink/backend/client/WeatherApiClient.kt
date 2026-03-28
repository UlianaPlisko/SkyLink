package com.skylink.backend.client

import com.skylink.backend.config.WeatherApiProperties
import com.skylink.backend.dto.weather.WeatherApiResponseDto
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException

@Component
class WeatherApiClient(
    private val weatherApiWebClient: WebClient,
    private val properties: WeatherApiProperties
) {

    fun getCurrentWeather(location: String): WeatherApiResponseDto {
        try {
            return weatherApiWebClient.get()
                .uri { uriBuilder ->
                    uriBuilder
                        .path("/current.json")
                        .queryParam("key", properties.key)
                        .queryParam("q", location)
                        .queryParam("aqi", "no")
                        .build()
                }
                .retrieve()
                .bodyToMono(WeatherApiResponseDto::class.java)
                .block()
                ?: throw RuntimeException("Empty response from WeatherAPI.")
        } catch (ex: WebClientResponseException) {
            throw RuntimeException(
                "WeatherAPI request failed with status ${ex.statusCode.value()}: ${ex.responseBodyAsString}",
                ex
            )
        } catch (ex: Exception) {
            throw RuntimeException("Failed to fetch weather data.", ex)
        }
    }
}