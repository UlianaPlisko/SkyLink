package com.codepalace.accelerometer.api


import com.codepalace.accelerometer.data.model.dto.CurrentVisibilityResponse
import retrofit2.http.GET
import retrofit2.http.QueryMap

interface WeatherApi {
    @GET("/api/weather/current-visibility")
    suspend fun getCurrentVisibility(
        @QueryMap params: Map<String, String>
    ): CurrentVisibilityResponse
}