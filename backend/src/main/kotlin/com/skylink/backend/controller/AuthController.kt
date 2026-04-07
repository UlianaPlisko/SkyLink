package com.skylink.backend.controller

import com.skylink.backend.dto.auth.*
import com.skylink.backend.service.AuthServiceInterface
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@Tag(
    name = "Authentication",
    description = "Operations related to user authentication"
)
@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthServiceInterface
) {

    @Operation(summary = "Register a new account")
    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): AuthResponse {
        return authService.register(request)
    }

    @Operation(summary = "Log in to an existing account")
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): AuthResponse {
        return authService.login(request)
    }

    @Operation(summary = "Authenticate with Google")
    @PostMapping("/google")
    fun googleAuth(@RequestBody request: GoogleAuthRequest): GoogleCallbackResponse {
        return authService.googleAuth(request)
    }

    @Operation(summary = "Complete Google authentication")
    @PostMapping("/google/complete")
    fun completeGoogle(
        @RequestBody request: CompleteGoogleRequest
    ): AuthResponse {
        return authService.completeGoogleRegistration(request)
    }
}