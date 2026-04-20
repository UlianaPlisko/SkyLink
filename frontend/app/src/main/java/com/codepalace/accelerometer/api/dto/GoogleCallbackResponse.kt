package com.codepalace.accelerometer.api.dto

import com.codepalace.accelerometer.data.model.enums.UserRole

data class GoogleCallbackResponse(
    val token: String,
    val isPending: Boolean,
    val displayName: String? = null,
    val role: UserRole? = null,
    val userId: Long? = null
)