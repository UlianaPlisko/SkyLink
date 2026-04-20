package com.codepalace.accelerometer.api.dto
import com.codepalace.accelerometer.data.model.enums.UserRole

data class RegisterRequest(
    val email: String,
    val displayName: String,
    val password: String,
    val role: UserRole
)