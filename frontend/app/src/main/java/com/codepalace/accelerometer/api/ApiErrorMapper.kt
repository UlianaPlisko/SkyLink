package com.codepalace.accelerometer.api

import com.google.gson.Gson
import retrofit2.HttpException
import java.io.IOException

object ApiErrorMapper {

    private val gson = Gson()

    fun fromHttpException(exception: HttpException, fallback: String): String {
        val serverMessage = readServerMessage(exception)
        if (!serverMessage.isNullOrBlank()) {
            return mapKnownMessage(serverMessage)
        }

        return when (exception.code()) {
            400 -> "Check the entered data and try again."
            401, 403 -> "Wrong email or password."
            409 -> "This account is already registered."
            else -> fallback
        }
    }

    fun fromIOException(exception: IOException): String {
        return if (exception.message.isNullOrBlank()) {
            "Network error. Check your connection and try again."
        } else {
            "Network error. Check your connection and try again."
        }
    }

    fun fromThrowable(exception: Throwable, fallback: String = "Something went wrong. Try again.") =
        exception.message?.takeIf { it.isNotBlank() }?.let(::mapKnownMessage) ?: fallback

    private fun readServerMessage(exception: HttpException): String? {
        val rawBody = runCatching {
            exception.response()?.errorBody()?.string()
        }.getOrNull()

        if (rawBody.isNullOrBlank()) return null

        return runCatching {
            gson.fromJson(rawBody, BackendError::class.java)?.message
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    private fun mapKnownMessage(message: String): String {
        val normalized = message.lowercase()

        return when {
            "email already in use" in normalized ->
                "This email is already registered. Try logging in."

            "user already registered" in normalized ->
                "This account is already registered. Try logging in."

            "user not found" in normalized || "invalid password" in normalized ->
                "Wrong email or password."

            "please sign in with google" in normalized ->
                "This email uses Google sign-in."

            "email registered with password login" in normalized ->
                "This email already has a password account."

            "old password is incorrect" in normalized ->
                "Current password is incorrect."

            "new password cannot be the same" in normalized ->
                "New password must be different from the current one."

            "google account cannot have password" in normalized ->
                "Google accounts do not use a local password."

            "file is empty" in normalized ->
                "Choose an image before uploading."

            "only jpg, png, and webp" in normalized ->
                "Only JPG, PNG, and WEBP images are allowed."

            "file size must not exceed" in normalized ->
                "Profile picture must be smaller than 5 MB."

            else -> message
        }
    }

    private data class BackendError(
        val message: String? = null
    )
}
