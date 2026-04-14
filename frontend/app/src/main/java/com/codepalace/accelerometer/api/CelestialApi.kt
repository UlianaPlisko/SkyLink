package com.codepalace.accelerometer.api

import com.codepalace.accelerometer.data.model.SpaceObjectSummary
import retrofit2.http.GET
import retrofit2.http.Query

interface CelestialApi {

    @GET("/api/celestial/space-objects")
    suspend fun getAllSpaceObjects(): List<SpaceObjectSummary>

    @GET("/api/celestial/bright-stars")
    suspend fun getBrightStars(@Query("maxMagnitude") maxMagnitude: Double = 3.0): List<SpaceObjectSummary>
}