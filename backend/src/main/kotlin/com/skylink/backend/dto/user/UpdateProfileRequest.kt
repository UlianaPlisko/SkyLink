package com.skylink.backend.dto.user

import jakarta.validation.constraints.Size

data class UpdateProfileRequest(
    @field:Size(min = 3, max = 50)
    val displayName: String? = null
)