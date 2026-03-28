package com.skylink.backend.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient


@Configuration
@EnableConfigurationProperties(WeatherApiProperties::class)
class WeatherApiConfig {

    @Bean
    fun webClientBuilder(): WebClient.Builder {
        return WebClient.builder()
    }

    @Bean
    fun weatherApiWebClient(
        builder: WebClient.Builder,
        properties: WeatherApiProperties
    ): WebClient {
        println("Weather API base URL = '${properties.baseUrl}'")
        println("Weather API key present = ${properties.key.isNotBlank()}")
        return builder
            .baseUrl(properties.baseUrl)
            .build()
    }

}