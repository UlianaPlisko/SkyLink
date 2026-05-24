package com.skylink.backend.integration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.skylink.backend.controller.AuthController
import com.skylink.backend.dto.auth.AuthResponse
import com.skylink.backend.dto.auth.LoginRequest
import com.skylink.backend.dto.auth.RegisterRequest
import com.skylink.backend.model.enums.UserRole
import com.skylink.backend.service.AuthServiceInterface
import org.junit.jupiter.api.Test
import org.mockito.Mockito.never
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [AuthController::class])
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerWebMvcIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc
) {

    private val objectMapper = jacksonObjectMapper()

    @MockitoBean
    lateinit var authService: AuthServiceInterface

    @Test
    fun `login endpoint deserializes request delegates service and serializes response`() {
        val request = LoginRequest("user@example.com", "password123")
        whenever(authService.login(request)).thenReturn(
            AuthResponse(
                token = "jwt-token",
                role = UserRole.OBSERVER,
                displayName = "Sky User",
                userId = 12L
            )
        )

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.token").value("jwt-token"))
            .andExpect(jsonPath("$.role").value("OBSERVER"))
            .andExpect(jsonPath("$.displayName").value("Sky User"))
            .andExpect(jsonPath("$.userId").value(12))

        verify(authService).login(request)
    }

    @Test
    fun `register endpoint rejects invalid json before service call`() {
        val invalidRequest = RegisterRequest(
            email = "not-an-email",
            displayName = "ab",
            password = "short",
            role = UserRole.OBSERVER
        )

        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
        )
            .andExpect(status().isBadRequest)

        verify(authService, never()).register(any())
    }
}
