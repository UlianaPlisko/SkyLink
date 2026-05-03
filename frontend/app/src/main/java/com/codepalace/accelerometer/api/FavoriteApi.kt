package com.codepalace.accelerometer.api

import com.codepalace.accelerometer.api.dto.FavoriteRequest
import com.codepalace.accelerometer.api.dto.FavoriteResponse
import com.codepalace.accelerometer.api.dto.FavoriteUpdateRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface FavoriteApi {

    @GET("api/favorites")
    suspend fun getFavorites(): List<FavoriteResponse>

    @POST("api/favorites")
    suspend fun addFavorite(@Body request: FavoriteRequest): FavoriteResponse

    @PATCH("api/favorites/{spaceObjectId}")
    suspend fun updateFavorite(
        @Path("spaceObjectId") spaceObjectId: Long,
        @Body request: FavoriteUpdateRequest
    ): FavoriteResponse

    @DELETE("api/favorites/{spaceObjectId}")
    suspend fun removeFavorite(@Path("spaceObjectId") spaceObjectId: Long): Response<Unit>
}
