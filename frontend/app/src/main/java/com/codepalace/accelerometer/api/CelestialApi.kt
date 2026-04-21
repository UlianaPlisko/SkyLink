package com.codepalace.accelerometer.api

import com.codepalace.accelerometer.data.model.SpaceObjectDetail
import com.codepalace.accelerometer.data.model.SpaceObjectSummary
import com.codepalace.accelerometer.data.model.dto.WikiResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface CelestialApi {

    @GET("/api/celestial/space-objects")
    suspend fun getAllSpaceObjects(): List<SpaceObjectSummary>

    @GET("/api/celestial/bright-stars")
    suspend fun getBrightStars(@Query("maxMagnitude") maxMagnitude: Double = 3.0): List<SpaceObjectSummary>

    @GET("/api/celestial/space-objects/{id}")
    suspend fun getSpaceObjectDetail(@Path("id") id: Long): SpaceObjectDetail

    @GET("/api/celestial/space-objects/{id}/wiki")
    suspend fun getSpaceObjectWiki(@Path("id") id: Long): WikiResponse
}