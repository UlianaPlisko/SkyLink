package com.codepalace.accelerometer.api.dto

import com.codepalace.accelerometer.data.model.enums.UserRole

data class CompleteGoogleRequest(
    val pendingToken: String,
    val displayName: String,
    val role: UserRole
)