package com.skylink.backend.service

import com.skylink.backend.dto.celestial.WikiResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Service
class WikipediaService {

    private val log = LoggerFactory.getLogger(WikipediaService::class.java)

    private val wikiClient = WebClient.builder()
        .baseUrl("https://en.wikipedia.org/api/rest_v1")
        .build()

    private val imageClient = WebClient.builder()
        .defaultHeader(HttpHeaders.USER_AGENT, "SkyLinkBackend/1.0")
        .build()

    @Service
    class WikipediaService {

        private val log = LoggerFactory.getLogger(WikipediaService::class.java)

        private val wikiClient = WebClient.builder()
            .baseUrl("https://en.wikipedia.org/api/rest_v1")
            .build()

        fun fetchSummary(title: String): WikiResponse? {
            if (title.isBlank()) return null

            return try {
                val encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8)

                val response = wikiClient.get()
                    .uri("/page/summary/$encodedTitle")
                    .retrieve()
                    .bodyToMono(WikipediaSummaryApiResponse::class.java)
                    .block()

                response?.let {
                    WikiResponse(
                        title = it.title ?: title,
                        summary = it.extract,
                        url = it.contentUrls?.desktop?.page,
                        imageUrl = it.thumbnail?.source
                    )
                }
            } catch (e: WebClientResponseException.NotFound) {
                log.info("Wikipedia page not found for title={}", title)
                null
            } catch (e: WebClientResponseException) {
                log.warn(
                    "Wikipedia request failed for title={} with status {}",
                    title,
                    e.statusCode
                )
                null
            } catch (e: Exception) {
                log.error("Unexpected Wikipedia fetch error for title={}", title, e)
                null
            }
        }
    }

    fun downloadImage(imageUrl: String): ByteArray? {
        return try {
            imageClient.get()
                .uri(imageUrl)
                .retrieve()
                .bodyToMono(ByteArray::class.java)
                .block()
        } catch (e: WebClientResponseException.NotFound) {
            log.info("Wikipedia image not found: {}", imageUrl)
            null
        } catch (e: WebClientResponseException) {
            log.warn(
                "Wikipedia image request failed for URL={} with status {}",
                imageUrl,
                e.statusCode
            )
            null
        } catch (e: Exception) {
            log.error("Unexpected image download error for URL={}", imageUrl, e)
            null
        }
    }

    fun guessContentType(imageUrl: String): String {
        val lower = imageUrl.lowercase()
        return when {
            lower.endsWith(".png") -> "image/png"
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
            lower.endsWith(".webp") -> "image/webp"
            lower.endsWith(".gif") -> "image/gif"
            else -> "image/jpeg"
        }
    }
}

data class WikipediaSummaryApiResponse(
    val title: String?,
    val extract: String?,
    val thumbnail: WikipediaThumbnail?,
    val contentUrls: WikipediaContentUrls?
)

data class WikipediaThumbnail(
    val source: String?
)

data class WikipediaContentUrls(
    val desktop: WikipediaDesktopUrl?
)

data class WikipediaDesktopUrl(
    val page: String?
)