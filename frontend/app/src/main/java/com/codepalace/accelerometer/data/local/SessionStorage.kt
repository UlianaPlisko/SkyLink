package com.codepalace.accelerometer.data.local

import android.content.Context
import android.util.Base64
import org.json.JSONObject
import java.nio.charset.StandardCharsets

class SessionStorage(context: Context) {

    private val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    fun saveAuth(
        token: String,
        role: String,
        displayName: String,
        userId: Long,
        provider: String = AUTH_PROVIDER_LOCAL
    ) {
        if (!isValidAuthToken(token)) {
            clearAuth()
            return
        }

        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_ROLE, role)
            .putString(KEY_DISPLAY_NAME, displayName)
            .putLong(KEY_USER_ID, userId)
            .putString(KEY_AUTH_PROVIDER, provider)
            .remove(KEY_PENDING_GOOGLE_TOKEN)
            .apply()
    }

    fun updateUserProfile(
        role: String,
        displayName: String,
        userId: Long,
        provider: String = getAuthProvider() ?: AUTH_PROVIDER_LOCAL
    ) {
        prefs.edit()
            .putString(KEY_ROLE, role)
            .putString(KEY_DISPLAY_NAME, displayName)
            .putLong(KEY_USER_ID, userId)
            .putString(KEY_AUTH_PROVIDER, provider)
            .apply()
    }

    fun getToken(): String? {
        val token = prefs.getString(KEY_TOKEN, null)
        return if (token != null && !isValidAuthToken(token)) {
            clearAuth()
            null
        } else {
            token
        }
    }

    fun peekToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun getRole(): String? = prefs.getString(KEY_ROLE, null)

    fun getDisplayName(): String? = prefs.getString(KEY_DISPLAY_NAME, null)

    fun getAuthProvider(): String? = prefs.getString(KEY_AUTH_PROVIDER, null)

    fun isGoogleAccount(): Boolean = getAuthProvider() == AUTH_PROVIDER_GOOGLE

    fun getUserId(): Long = prefs.getLong(KEY_USER_ID, -1L)

    fun isLoggedIn(): Boolean = !getToken().isNullOrBlank()

    fun clearAuth() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_ROLE)
            .remove(KEY_DISPLAY_NAME)
            .remove(KEY_USER_ID)
            .remove(KEY_AUTH_PROVIDER)
            .remove(KEY_PENDING_GOOGLE_TOKEN)
            .apply()
    }

    fun savePendingGoogleToken(token: String) {
        if (isPendingGoogleToken(token)) {
            prefs.edit().putString(KEY_PENDING_GOOGLE_TOKEN, token).apply()
        } else {
            clearPendingGoogleToken()
        }
    }

    fun getPendingGoogleToken(): String? {
        val token = prefs.getString(KEY_PENDING_GOOGLE_TOKEN, null)
        return if (token != null && !isPendingGoogleToken(token)) {
            clearPendingGoogleToken()
            null
        } else {
            token
        }
    }

    fun clearPendingGoogleToken() {
        prefs.edit().remove(KEY_PENDING_GOOGLE_TOKEN).apply()
    }

    private fun isValidAuthToken(token: String): Boolean {
        val claims = decodeJwtPayload(token) ?: return false
        val tokenType = claims.optString("tokenType")
        val expiresAtSeconds = claims.optLong("exp", 0L)
        val nowSeconds = System.currentTimeMillis() / 1000L

        return tokenType == TOKEN_TYPE_AUTH && expiresAtSeconds > nowSeconds
    }

    private fun isPendingGoogleToken(token: String): Boolean {
        val claims = decodeJwtPayload(token) ?: return false
        val tokenType = claims.optString("tokenType")
        val expiresAtSeconds = claims.optLong("exp", 0L)
        val nowSeconds = System.currentTimeMillis() / 1000L

        return tokenType == TOKEN_TYPE_PENDING_GOOGLE && expiresAtSeconds > nowSeconds
    }

    private fun decodeJwtPayload(token: String): JSONObject? {
        return runCatching {
            val payload = token.split(".").getOrNull(1) ?: return null
            val decoded = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
            JSONObject(String(decoded, StandardCharsets.UTF_8))
        }.getOrNull()
    }

    companion object {
        private const val KEY_TOKEN = "token"
        private const val KEY_ROLE = "role"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_AUTH_PROVIDER = "auth_provider"
        private const val KEY_PENDING_GOOGLE_TOKEN = "pending_google_token"
        private const val TOKEN_TYPE_AUTH = "AUTH"
        private const val TOKEN_TYPE_PENDING_GOOGLE = "PENDING_GOOGLE"
        const val AUTH_PROVIDER_LOCAL = "LOCAL"
        const val AUTH_PROVIDER_GOOGLE = "GOOGLE"
    }
}
