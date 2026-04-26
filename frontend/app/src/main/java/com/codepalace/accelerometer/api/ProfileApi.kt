package com.codepalace.accelerometer.api

import com.codepalace.accelerometer.api.dto.ChangePasswordRequest
import com.codepalace.accelerometer.api.dto.UpdateProfileRequest
import com.codepalace.accelerometer.api.dto.UserProfileResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part

interface ProfileApi {

    @GET("api/profile")
    suspend fun getProfile(): UserProfileResponse

    @PUT("api/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): UserProfileResponse

    @PUT("api/profile/password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<Unit>

    @Multipart
    @POST("api/profile/pfp")
    suspend fun uploadProfilePicture(@Part file: MultipartBody.Part): Response<Unit>
}
