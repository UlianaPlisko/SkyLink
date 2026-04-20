package com.codepalace.accelerometer.data.local

import android.content.Context

class SessionStorage(context: Context) {

    private val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    fun saveAuth(
        token: String,
        role: String,
        displayName: String,
        userId: Long
    ) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_ROLE, role)
            .putString(KEY_DISPLAY_NAME, displayName)
            .putLong(KEY_USER_ID, userId)
            .apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun getRole(): String? = prefs.getString(KEY_ROLE, null)

    fun getDisplayName(): String? = prefs.getString(KEY_DISPLAY_NAME, null)

    fun getUserId(): Long = prefs.getLong(KEY_USER_ID, -1L)

    fun isLoggedIn(): Boolean = !getToken().isNullOrBlank()

    fun clearAuth() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_ROLE)
            .remove(KEY_DISPLAY_NAME)
            .remove(KEY_USER_ID)
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
        private const val KEY_PENDING_GOOGLE_TOKEN = "pending_google_token"
    }
}