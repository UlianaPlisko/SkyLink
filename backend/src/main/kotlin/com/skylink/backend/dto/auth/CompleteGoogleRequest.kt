package com.skylink.backend.dto.auth

import com.skylink.backend.model.enums.UserRole
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class CompleteGoogleRequest(
    val pendingToken: String,

    @field:NotBlank(message = "Display name is required")
    @field:Size(min = 3, max = 50, message = "Display name must be 3-50 characters")
    val displayName: String,

    @field:NotNull(message = "Please choose your role")
    val role: UserRole
)