package com.codepalace.accelerometer.api

import com.codepalace.accelerometer.api.dto.AuthResponse
import com.codepalace.accelerometer.api.dto.CompleteGoogleRequest
import com.codepalace.accelerometer.api.dto.GoogleAuthRequest
import com.codepalace.accelerometer.api.dto.GoogleCallbackResponse
import com.codepalace.accelerometer.api.dto.LoginRequest
import com.codepalace.accelerometer.api.dto.RegisterRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("api/auth/google")
    suspend fun googleAuth(@Body request: GoogleAuthRequest): GoogleCallbackResponse

    @POST("api/auth/google/complete")
    suspend fun completeGoogle(@Body request: CompleteGoogleRequest): AuthResponse
}