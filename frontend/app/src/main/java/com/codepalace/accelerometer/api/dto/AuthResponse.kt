package com.codepalace.accelerometer.api.dto

import com.codepalace.accelerometer.data.model.enums.UserRole

data class AuthResponse(
    val token: String,
    val role: UserRole,
    val displayName: String,
    val userId: Long
)