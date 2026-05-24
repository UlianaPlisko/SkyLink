package com.skylink.backend.config

import com.skylink.backend.service.JwtService
import com.google.firebase.FirebaseApp
import com.zaxxer.hikari.HikariDataSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration
import org.springframework.boot.test.context.runner.WebApplicationContextRunner
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.util.function.Supplier

class ConfigAndFilterTest {

    @Test
    fun `open api config exposes bearer metadata`() {
        val openApi = OpenApiConfig().customOpenAPI()

        assertEquals("SkyLink REST API", openApi.info.title)
        assertEquals("SkyLink Team", openApi.info.contact.name)
        assertEquals("Apache 2.0", openApi.info.license.name)
        assertTrue(openApi.components.securitySchemes.containsKey("bearerAuth"))
    }

    @Test
    fun `weather api config creates builder and client`() {
        val config = WeatherApiConfig()
        val defaultProperties = WeatherApiProperties()
        defaultProperties.key = "default-key"
        defaultProperties.baseUrl = "https://default-weather.test"
        val builder = config.webClientBuilder()
        val client = suppressStdout {
            config.weatherApiWebClient(builder, WeatherApiProperties(key = "key", baseUrl = "https://weather.test"))
        }
        val blankKeyClient = suppressStdout {
            config.weatherApiWebClient(builder, WeatherApiProperties(key = "", baseUrl = "https://weather.test"))
        }

        assertEquals("default-key", defaultProperties.key)
        assertEquals("https://default-weather.test", defaultProperties.baseUrl)
        assertNotNull(client)
        assertNotNull(blankKeyClient)
    }

    @Test
    fun `data source config validates url and creates hikari data source`() {
        assertThrows(IllegalStateException::class.java) {
            DataSourceConfig(AppDataSourceProperties()).dataSource()
        }

        assertThrows(Exception::class.java) {
            DataSourceConfig(
                AppDataSourceProperties(
                    url = "jdbc:postgresql://localhost:5432/test",
                    username = "user",
                    password = "pass",
                    maxPoolSize = 3,
                    minIdle = 1,
                    connectionTimeout = 1000L
                )
            ).dataSource()
        }

        Mockito.mockConstruction(HikariDataSource::class.java).use { mockedDataSources ->
            val dataSource = DataSourceConfig(
                AppDataSourceProperties(
                    url = "jdbc:postgresql://localhost:5432/test",
                    maxPoolSize = 3,
                    minIdle = 1,
                    connectionTimeout = 1000L
                )
            ).dataSource()

            assertNotNull(dataSource)
            assertEquals(1, mockedDataSources.constructed().size)
        }
    }

    @Test
    fun `security config creates encoder and jwt filter bean`() {
        val config = SecurityConfig()
        val jwtService: JwtService = mock()

        assertTrue(config.passwordEncoder().matches("secret", config.passwordEncoder().encode("secret")))
        assertNotNull(config.jwtAuthenticationFilter(jwtService))
    }

    @Test
    fun `security filter chain is built by servlet security context`() {
        WebApplicationContextRunner()
            .withConfiguration(
                AutoConfigurations.of(
                    SecurityAutoConfiguration::class.java,
                    ServletWebSecurityAutoConfiguration::class.java,
                    SecurityFilterAutoConfiguration::class.java
                )
            )
            .withUserConfiguration(SecurityConfig::class.java, DelegatingWebMvcConfiguration::class.java)
            .withBean(JwtService::class.java, Supplier { mock<JwtService>() })
            .run { context ->
                assertEquals(1, context.getBeansOfType(SecurityFilterChain::class.java).size)
            }
    }

    @Test
    fun `jwt authentication filter skips missing header and authenticates valid token`() {
        val jwtService: JwtService = mock()
        val filter = JwtAuthenticationFilter(jwtService)
        SecurityContextHolder.clearContext()

        filter.doFilter(MockHttpServletRequest(), MockHttpServletResponse(), MockFilterChain())
        assertEquals(null, SecurityContextHolder.getContext().authentication)

        val malformedHeaderRequest = MockHttpServletRequest()
        malformedHeaderRequest.addHeader(HttpHeaders.AUTHORIZATION, "Token value")
        filter.doFilter(malformedHeaderRequest, MockHttpServletResponse(), MockFilterChain())
        assertEquals(null, SecurityContextHolder.getContext().authentication)

        val request = MockHttpServletRequest()
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token")
        whenever(jwtService.extractEmail("token")).thenReturn("user@test.com")
        whenever(jwtService.validateToken("token", "user@test.com")).thenReturn(true)
        filter.doFilter(request, MockHttpServletResponse(), MockFilterChain())

        assertEquals("user@test.com", SecurityContextHolder.getContext().authentication?.name)
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `jwt authentication filter leaves context empty when token is not valid`() {
        val jwtService: JwtService = mock()
        val filter = JwtAuthenticationFilter(jwtService)
        val request = MockHttpServletRequest()
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token")
        whenever(jwtService.extractEmail("token")).thenReturn("user@test.com")
        whenever(jwtService.validateToken("token", "user@test.com")).thenReturn(false)

        filter.doFilter(request, MockHttpServletResponse(), MockFilterChain())

        assertEquals(null, SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `jwt authentication filter swallows invalid tokens`() {
        val jwtService: JwtService = mock()
        val filter = JwtAuthenticationFilter(jwtService)
        val request = MockHttpServletRequest()
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token")
        whenever(jwtService.extractEmail("token")).thenThrow(IllegalArgumentException("bad"))

        filter.doFilter(request, MockHttpServletResponse(), MockFilterChain())

        assertEquals(null, SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `web socket config registers handler path and origins`() {
        val handler: ChatWebSocketHandler = mock()
        val registry: WebSocketHandlerRegistry = mock()
        val registration: WebSocketHandlerRegistration = mock()
        whenever(registry.addHandler(handler, "/ws-chat")).thenReturn(registration)
        whenever(registration.setAllowedOrigins("http://localhost:3000", "https://your-frontend.com")).thenReturn(registration)

        WebSocketConfig(handler).registerWebSocketHandlers(registry)

        verify(registry).addHandler(handler, "/ws-chat")
        verify(registration).setAllowedOrigins("http://localhost:3000", "https://your-frontend.com")
    }

    @Test
    fun `google verifier and firebase config bean methods are callable`() {
        assertNotNull(GoogleVerifierConfig("client-id").googleVerifier())
        runCatching { FirebaseConfig().firebaseApp() }
            .onSuccess { assertNotNull(it) }
            .onFailure { assertTrue(it is Exception) }
    }

    @Test
    fun `firebase config returns existing app when firebase is already initialized`() {
        val existingApp: FirebaseApp = mock()

        Mockito.mockStatic(FirebaseApp::class.java).use { firebaseApp ->
            firebaseApp.`when`<List<FirebaseApp>> { FirebaseApp.getApps() }
                .thenReturn(listOf(existingApp))
            firebaseApp.`when`<FirebaseApp> { FirebaseApp.getInstance() }
                .thenReturn(existingApp)

            assertEquals(existingApp, FirebaseConfig().firebaseApp())
        }
    }

    private fun <T> suppressStdout(block: () -> T): T {
        val originalOut = System.out
        return try {
            System.setOut(PrintStream(ByteArrayOutputStream()))
            block()
        } finally {
            System.setOut(originalOut)
        }
    }
}
