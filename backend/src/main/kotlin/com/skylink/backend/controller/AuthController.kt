package com.skylink.backend.controller

import com.skylink.backend.dto.auth.*
import com.skylink.backend.service.AuthServiceInterface
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthServiceInterface
) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): AuthResponse {
        return authService.register(request)
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): AuthResponse {
        return authService.login(request)
    }

    @PostMapping("/google")
    fun googleAuth(@RequestBody request: GoogleAuthRequest): GoogleCallbackResponse {
        return authService.googleAuth(request)
    }

    @PostMapping("/google/complete")
    fun completeGoogle(
        @RequestBody request: CompleteGoogleRequest
    ): AuthResponse {
        return authService.completeGoogleRegistration(request)
    }
}
