package com.skylink.backend.dto.celestial

data class WikiResponse(
    val title: String,
    val summary: String?,
    val url: String?,
    val imageUrl: String?
)