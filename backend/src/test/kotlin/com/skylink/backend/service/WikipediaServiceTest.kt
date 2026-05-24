package com.skylink.backend.service

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class WikipediaServiceTest {

    @Test
    fun `fetch summary ignores blank title and maps response`() {
        assertNull(WikipediaService().fetchSummary("   "))
        val service = serviceWithClients(
            wikiClient = webClient {
                ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body("""{"title":"Mars","extract":"Planet","thumbnail":{"source":"image"},"contentUrls":{"desktop":{"page":"url"}}}""")
                    .build()
            },
            imageClient = webClient { ClientResponse.create(HttpStatus.OK).build() }
        )

        val response = service.fetchSummary("Mars")

        assertEquals("Mars", response?.title)
        assertEquals("Planet", response?.summary)
        assertEquals("url", response?.url)
        assertEquals("image", response?.imageUrl)
    }

    @Test
    fun `fetch summary falls back when optional fields are absent`() {
        val service = serviceWithClients(
            wikiClient = webClient {
                ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body("""{"extract":"Fallback summary"}""")
                    .build()
            },
            imageClient = webClient { ClientResponse.create(HttpStatus.OK).build() }
        )

        val response = service.fetchSummary("Fallback")

        assertEquals("Fallback", response?.title)
        assertEquals("Fallback summary", response?.summary)
        assertNull(response?.url)
        assertNull(response?.imageUrl)
    }

    @Test
    fun `fetch summary returns null when response body is empty`() {
        val service = serviceWithClients(
            wikiClient = webClient { ClientResponse.create(HttpStatus.OK).build() },
            imageClient = webClient { ClientResponse.create(HttpStatus.OK).build() }
        )

        assertNull(service.fetchSummary("No body"))
    }

    @Test
    fun `fetch summary returns null for not found other http errors and unexpected errors`() {
        assertNull(serviceWithWikiStatus(HttpStatus.NOT_FOUND).fetchSummary("Missing"))
        assertNull(serviceWithWikiStatus(HttpStatus.INTERNAL_SERVER_ERROR).fetchSummary("Broken"))

        val throwingClient = WebClient.builder()
            .exchangeFunction { Mono.error(IllegalStateException("boom")) }
            .build()

        assertNull(serviceWithClients(throwingClient, throwingClient).fetchSummary("Boom"))
    }

    @Test
    fun `download image maps bytes and handles failures`() {
        val okService = serviceWithClients(
            wikiClient = webClient { ClientResponse.create(HttpStatus.OK).build() },
            imageClient = webClient {
                ClientResponse.create(HttpStatus.OK)
                    .body(Flux.just(DefaultDataBufferFactory().wrap(byteArrayOf(1, 2, 3))))
                    .build()
            }
        )

        assertArrayEquals(byteArrayOf(1, 2, 3), okService.downloadImage("https://image.test/a.png"))
        assertNull(serviceWithImageStatus(HttpStatus.NOT_FOUND).downloadImage("https://image.test/missing.png"))
        assertNull(serviceWithImageStatus(HttpStatus.BAD_GATEWAY).downloadImage("https://image.test/bad.png"))

        val throwingClient = WebClient.builder()
            .exchangeFunction { Mono.error(IllegalStateException("boom")) }
            .build()
        assertNull(serviceWithClients(throwingClient, throwingClient).downloadImage("https://image.test/boom.png"))
    }

    @Test
    fun `guess content type maps known extensions and defaults to jpeg`() {
        val service = WikipediaService()

        assertEquals("image/png", service.guessContentType("A.PNG"))
        assertEquals("image/jpeg", service.guessContentType("a.jpg"))
        assertEquals("image/jpeg", service.guessContentType("a.jpeg"))
        assertEquals("image/webp", service.guessContentType("a.webp"))
        assertEquals("image/gif", service.guessContentType("a.gif"))
        assertEquals("image/jpeg", service.guessContentType("a.bmp"))
    }

    private fun serviceWithWikiStatus(status: HttpStatus): WikipediaService =
        serviceWithClients(
            wikiClient = webClient { ClientResponse.create(status).body("error").build() },
            imageClient = webClient { ClientResponse.create(HttpStatus.OK).build() }
        )

    private fun serviceWithImageStatus(status: HttpStatus): WikipediaService =
        serviceWithClients(
            wikiClient = webClient { ClientResponse.create(HttpStatus.OK).build() },
            imageClient = webClient { ClientResponse.create(status).body("error").build() }
        )

    private fun serviceWithClients(wikiClient: WebClient, imageClient: WebClient): WikipediaService {
        val service = WikipediaService()
        setPrivateField(service, "wikiClient", wikiClient)
        setPrivateField(service, "imageClient", imageClient)
        return service
    }

    private fun setPrivateField(target: Any, fieldName: String, value: Any) {
        val field = target.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(target, value)
    }

    private fun webClient(response: (org.springframework.web.reactive.function.client.ClientRequest) -> ClientResponse): WebClient {
        val exchange = ExchangeFunction { request -> Mono.just(response(request)) }
        return WebClient.builder()
            .baseUrl("https://wiki.test")
            .exchangeFunction(exchange)
            .build()
    }
}
