package com.codepalace.accelerometer.data.model.dto

data class ConstellationCultureResponse(
    val id: Long,
    val name: String,
    val region: String,
    val description: String?,
    val current: Boolean
)
