package com.skylink.backend.dto.auth

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.*
import jakarta.validation.constraints.Size
import com.skylink.backend.model.enums.UserRole

data class RegisterRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String,

    @field:NotBlank(message = "Display name is required")
    @field:Size(min = 3, max = 50)
    val displayName: String,

    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, message = "Password must be at least 8 characters")
    val password: String,

    @field:NotNull(message = "Please, choose your role")
    val role: UserRole
)