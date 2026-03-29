package com.skylink.backend.dto.auth

data class GoogleCallbackResponse(
    val token: String,
    val isPending: Boolean,
    val displayName: String? = null
)
