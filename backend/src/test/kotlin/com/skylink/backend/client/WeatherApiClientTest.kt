package com.skylink.backend.client

import com.skylink.backend.config.WeatherApiProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

class WeatherApiClientTest {

    @Test
    fun `get current weather sends expected query and decodes response`() {
        var requestedUrl = ""
        val client = WeatherApiClient(
            webClient { request ->
                requestedUrl = request.url().toString()
                ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body("""{"location":{"name":"Bratislava","localtime":"now"},"current":{"vis_km":10.5,"cloud":20,"humidity":40,"precip_mm":0.2,"is_day":1,"condition":{"text":"Clear","code":1000}}}""")
                    .build()
            },
            WeatherApiProperties(key = "secret", baseUrl = "https://weather.test")
        )

        val response = client.getCurrentWeather("Bratislava")

        assertEquals("Bratislava", response.location?.name)
        assertEquals(10.5, response.current?.visKm)
        assertEquals("https://weather.test/current.json?key=secret&q=Bratislava&aqi=no", requestedUrl)
    }

    @Test
    fun `get current weather wraps empty response and http errors`() {
        val emptyClient = WeatherApiClient(
            webClient {
                ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .build()
            },
            WeatherApiProperties(key = "secret", baseUrl = "https://weather.test")
        )
        val errorClient = WeatherApiClient(
            webClient {
                ClientResponse.create(HttpStatus.BAD_REQUEST)
                    .body("bad request")
                    .build()
            },
            WeatherApiProperties(key = "secret", baseUrl = "https://weather.test")
        )

        assertThrows(RuntimeException::class.java) { emptyClient.getCurrentWeather("X") }
        assertThrows(RuntimeException::class.java) { errorClient.getCurrentWeather("X") }
    }

    private fun webClient(response: (org.springframework.web.reactive.function.client.ClientRequest) -> ClientResponse): WebClient {
        val exchange = ExchangeFunction { request -> Mono.just(response(request)) }
        return WebClient.builder()
            .baseUrl("https://weather.test")
            .exchangeFunction(exchange)
            .build()
    }
}
