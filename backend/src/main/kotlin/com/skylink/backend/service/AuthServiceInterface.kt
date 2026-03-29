package com.skylink.backend.service

import com.skylink.backend.dto.auth.*
import org.springframework.stereotype.Service

@Service
interface AuthServiceInterface {
    fun register(request: RegisterRequest): AuthResponse
    fun login(request: LoginRequest): AuthResponse
    fun googleAuth(request: GoogleAuthRequest): GoogleCallbackResponse
    fun completeGoogleRegistration(request: CompleteGoogleRequest): AuthResponse
}
