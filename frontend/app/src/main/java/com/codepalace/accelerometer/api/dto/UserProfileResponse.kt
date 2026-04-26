package com.codepalace.accelerometer.api.dto

import com.codepalace.accelerometer.data.model.enums.UserRole

data class UserProfileResponse(
    val id: Long,
    val email: String,
    val displayName: String,
    val role: UserRole,
    val provider: String? = null,
    val createdAt: String? = null,
    val lastUsedAt: String? = null,
    val pfpUrl: String? = null
)
