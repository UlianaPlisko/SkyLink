package com.skylink.backend.dto

data class ErrorResponse(
    val timestamp: String = java.time.Instant.now().toString(),
    val status: Int,
    val error: String,
    val message: String
)