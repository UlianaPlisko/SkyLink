package com.skylink.backend.service

import com.skylink.backend.dto.auth.LoginRequest
import com.skylink.backend.dto.auth.RegisterRequest
import com.skylink.backend.dto.auth.AuthResponse
import org.springframework.stereotype.Service

@Service
interface AuthServiceInterface {
    fun register(request: RegisterRequest): AuthResponse
    fun login(request: LoginRequest): AuthResponse
}
