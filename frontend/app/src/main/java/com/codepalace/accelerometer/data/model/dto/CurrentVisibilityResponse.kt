package com.codepalace.accelerometer.data.model.dto

import com.google.gson.annotations.SerializedName

data class CurrentVisibilityResponse(
    @SerializedName("location")
    val location: String,

    @SerializedName("localTime")
    val localTime: String,

    @SerializedName("visibilityKm")
    val visibilityKm: Double,

    @SerializedName("cloudPercent")
    val cloudPercent: Int,

    @SerializedName("humidity")
    val humidity: Int,

    @SerializedName("precipitationMm")
    val precipitationMm: Double,

    @SerializedName("conditionText")
    val conditionText: String,  // "Sunny", "Cloudy", "Rainy", etc.

    @SerializedName("conditionCode")
    val conditionCode: Int,  // 1000 = Sunny, etc.

    @SerializedName("day")
    val isDay: Boolean  // Changed from isDay to match "day" in response
)