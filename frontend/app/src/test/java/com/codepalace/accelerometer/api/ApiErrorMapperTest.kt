package com.codepalace.accelerometer.api

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class ApiErrorMapperTest {

    @Test
    fun `http errors prefer backend messages and map known messages`() {
        assertEquals(
            "This email is already registered. Try logging in.",
            ApiErrorMapper.fromHttpException(httpException(400, """{"message":"Email already in use"}"""), "fallback")
        )
        assertEquals(
            "Wrong email or password.",
            ApiErrorMapper.fromHttpException(httpException(401, """{"message":"Invalid password"}"""), "fallback")
        )
        assertEquals(
            "Only JPG, PNG, and WEBP images are allowed.",
            ApiErrorMapper.fromHttpException(httpException(400, """{"message":"Only JPG, PNG, and WEBP"}"""), "fallback")
        )
    }

    @Test
    fun `http errors fall back by status when backend message is absent`() {
        assertEquals(
            "Check the entered data and try again.",
            ApiErrorMapper.fromHttpException(httpException(400, ""), "fallback")
        )
        assertEquals("fallback", ApiErrorMapper.fromHttpException(httpException(401, ""), "fallback"))
        assertEquals("fallback", ApiErrorMapper.fromHttpException(httpException(403, ""), "fallback"))
        assertEquals(
            "This account is already registered.",
            ApiErrorMapper.fromHttpException(httpException(409, ""), "fallback")
        )
        assertEquals("fallback", ApiErrorMapper.fromHttpException(httpException(500, ""), "fallback"))
    }

    @Test
    fun `throwable and io exception mapping is stable`() {
        assertEquals(
            "This email uses Google sign-in.",
            ApiErrorMapper.fromThrowable(IllegalArgumentException("Please sign in with Google"))
        )
        assertEquals("fallback", ApiErrorMapper.fromThrowable(IllegalArgumentException(""), "fallback"))
        assertEquals(
            "Network error. Check your connection and try again.",
            ApiErrorMapper.fromIOException(IOException("socket closed"))
        )
        assertEquals(
            "Network error. Check your connection and try again.",
            ApiErrorMapper.fromIOException(IOException())
        )
    }

    private fun httpException(code: Int, body: String): HttpException {
        val responseBody = body.toResponseBody("application/json".toMediaType())
        return HttpException(Response.error<Any>(code, responseBody))
    }
}
