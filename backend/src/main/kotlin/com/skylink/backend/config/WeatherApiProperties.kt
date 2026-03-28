package com.skylink.backend.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.weather.api")
data class WeatherApiProperties(
    var key: String = "",
    var baseUrl: String = ""
)