package com.skylink.backend.dto.user

import com.skylink.backend.model.enums.UserRole
import java.time.Instant

data class UserProfileResponse(
    val id: Long,
    val email: String,
    val displayName: String,
    val role: UserRole,
    val createdAt: Instant,
    val lastUsedAt: Instant?
)