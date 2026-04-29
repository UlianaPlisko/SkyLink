package com.codepalace.accelerometer.data.repository

import com.codepalace.accelerometer.api.ApiClient
import com.codepalace.accelerometer.api.dto.AuthResponse
import com.codepalace.accelerometer.api.dto.LoginRequest
import com.codepalace.accelerometer.api.dto.RegisterRequest
import com.codepalace.accelerometer.data.model.enums.UserRole

class AuthRepository {

    suspend fun login(email: String, password: String): AuthResponse {
        return ApiClient.authApi.login(
            LoginRequest(
                email = email,
                password = password
            )
        )
    }

    suspend fun register(
        email: String,
        displayName: String,
        password: String,
        role: UserRole
    ): AuthResponse {
        return ApiClient.authApi.register(
            RegisterRequest(
                email = email,
                displayName = displayName,
                password = password,
                role = role
            )
        )
    }
}
