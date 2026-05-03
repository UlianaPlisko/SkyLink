package com.codepalace.accelerometer.auth

import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.codepalace.accelerometer.R
import com.codepalace.accelerometer.api.ApiClient
import com.codepalace.accelerometer.api.ApiErrorMapper
import com.codepalace.accelerometer.api.dto.GoogleAuthRequest
import com.codepalace.accelerometer.ui.MessageKind
import com.codepalace.accelerometer.ui.activity.GoogleCompleteActivity
import com.codepalace.accelerometer.ui.activity.MainActivity
import com.codepalace.accelerometer.ui.showAppMessage
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class GoogleSignInCoordinator(
    private val activity: AppCompatActivity
) {
    private val googleAuthManager = GoogleAuthManager(
        activity = activity,
        webClientId = activity.getString(R.string.web_client_id)
    )

    private var isRunning = false

    fun start() {
        if (isRunning) return

        activity.lifecycleScope.launch {
            isRunning = true
            Log.d("GOOGLE_AUTH", "Starting Google sign-in")

            try {
                googleAuthManager.getGoogleIdToken()
                    .onSuccess { idToken ->
                        handleGoogleToken(idToken)
                    }
                    .onFailure { error ->
                        Log.e("GOOGLE_AUTH", "Google sign-in failed", error)
                        activity.showAppMessage(
                            ApiErrorMapper.fromThrowable(
                                error,
                                "Google sign-in was cancelled or failed."
                            ),
                            MessageKind.ERROR
                        )
                    }
            } finally {
                isRunning = false
            }
        }
    }

    private suspend fun handleGoogleToken(idToken: String) {
        Log.d("GOOGLE_AUTH", "Google ID token received")

        try {
            val response = ApiClient.authApi.googleAuth(
                GoogleAuthRequest(idToken = idToken)
            )

            Log.d("GOOGLE_AUTH", "Backend response: isPending=${response.isPending}")

            if (response.isPending) {
                ApiClient.getSessionStorage().savePendingGoogleToken(response.token)

                val intent = Intent(activity, GoogleCompleteActivity::class.java)
                intent.putExtra("prefill_display_name", response.displayName ?: "")
                activity.startActivity(intent)
            } else {
                ApiClient.getSessionStorage().saveAuth(
                    token = response.token,
                    role = response.role?.name ?: "OBSERVER",
                    displayName = response.displayName ?: "Google user",
                    userId = response.userId ?: -1L,
                    provider = "GOOGLE"
                )

                activity.showAppMessage("Google login successful.", MessageKind.SUCCESS)
                activity.startActivity(Intent(activity, MainActivity::class.java))
                activity.finishAffinity()
            }
        } catch (e: HttpException) {
            Log.e("GOOGLE_AUTH", "Backend Google sign-in failed", e)
            activity.showAppMessage(
                ApiErrorMapper.fromHttpException(
                    e,
                    "Google sign-in is unavailable right now."
                ),
                MessageKind.ERROR
            )
        } catch (e: IOException) {
            Log.e("GOOGLE_AUTH", "Google sign-in network error", e)
            activity.showAppMessage(ApiErrorMapper.fromIOException(e), MessageKind.ERROR)
        } catch (e: Exception) {
            Log.e("GOOGLE_AUTH", "Google sign-in failed", e)
            activity.showAppMessage(ApiErrorMapper.fromThrowable(e), MessageKind.ERROR)
        }
    }
}
