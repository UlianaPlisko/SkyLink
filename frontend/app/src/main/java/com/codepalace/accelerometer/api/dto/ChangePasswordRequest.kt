package com.codepalace.accelerometer.api.dto

data class ChangePasswordRequest(
    val oldPassword: String,
    val newPassword: String
)
