package com.skylink.backend.dto.auth

import com.skylink.backend.model.enums.UserRole

data class AuthResponse(
    val token: String,
    val role: UserRole,
    val displayName: String,
    val userId: Long
)