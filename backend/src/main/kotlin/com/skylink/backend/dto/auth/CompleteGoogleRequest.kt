package com.skylink.backend.dto.auth

import com.skylink.backend.model.enums.UserRole
import jakarta.validation.constraints.NotNull

data class CompleteGoogleRequest(
    val pendingToken: String,
    @field:NotNull(message = "Please choose your role")
    val role: UserRole
)