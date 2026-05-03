package com.codepalace.accelerometer.api

import com.codepalace.accelerometer.data.local.SessionStorage
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val sessionStorage: SessionStorage
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = sessionStorage.getToken()

        val request = if (!token.isNullOrBlank()) {
            chain.request()
                .newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }

        val response = chain.proceed(request)
        if (!token.isNullOrBlank() && (response.code == 401)) {
            sessionStorage.clearAuth()
        }

        return response
    }
}
