package com.codepalace.accelerometer.data.local

import android.content.Context

class SessionStorage(context: Context) {

    private val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    fun saveAuth(
        token: String,
        role: String,
        displayName: String,
        userId: Long,
        provider: String = AUTH_PROVIDER_LOCAL
    ) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_ROLE, role)
            .putString(KEY_DISPLAY_NAME, displayName)
            .putLong(KEY_USER_ID, userId)
            .putString(KEY_AUTH_PROVIDER, provider)
            .apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

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
        prefs.edit().putString(KEY_PENDING_GOOGLE_TOKEN, token).apply()
    }

    fun getPendingGoogleToken(): String? {
        return prefs.getString(KEY_PENDING_GOOGLE_TOKEN, null)
    }

    fun clearPendingGoogleToken() {
        prefs.edit().remove(KEY_PENDING_GOOGLE_TOKEN).apply()
    }

    companion object {
        private const val KEY_TOKEN = "token"
        private const val KEY_ROLE = "role"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_AUTH_PROVIDER = "auth_provider"
        private const val KEY_PENDING_GOOGLE_TOKEN = "pending_google_token"
        const val AUTH_PROVIDER_LOCAL = "LOCAL"
        const val AUTH_PROVIDER_GOOGLE = "GOOGLE"
    }
}
