package com.codepalace.accelerometer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.codepalace.accelerometer.api.ApiClient
import com.codepalace.accelerometer.api.dto.GoogleAuthRequest
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class AuthActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var googleAuthManager: GoogleAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiClient.init(this)
        setContentView(R.layout.activity_auth)

        btnBack = findViewById(R.id.btnBack)

        btnBack.setOnClickListener {
            finish()
        }

        googleAuthManager = GoogleAuthManager(
            activity = this,
            webClientId = getString(R.string.web_client_id)
        )

        findViewById<Button>(R.id.btnGoLogin).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        findViewById<Button>(R.id.btnGoSignup).setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        findViewById<Button>(R.id.btnGoogle).setOnClickListener {
            doGoogleLogin()
        }
    }

    private fun doGoogleLogin() {
        lifecycleScope.launch {
            Log.d("GOOGLE_AUTH", "Starting Google sign-in")

            googleAuthManager.getGoogleIdToken()
                .onSuccess { idToken ->
                    Log.d("GOOGLE_AUTH", "Google ID token received")

                    try {
                        val response = ApiClient.authApi.googleAuth(
                            GoogleAuthRequest(idToken = idToken)
                        )

                        Log.d("GOOGLE_AUTH", "Backend response: isPending=${response.isPending}")

                        if (response.isPending) {
                            ApiClient.getSessionStorage().savePendingGoogleToken(response.token)

                            val intent = Intent(this@AuthActivity, GoogleCompleteActivity::class.java)
                            intent.putExtra("prefill_display_name", response.displayName ?: "")
                            startActivity(intent)
                        } else {
                            ApiClient.getSessionStorage().saveAuth(
                                token = response.token,
                                role = response.role?.name ?: "OBSERVER",
                                displayName = response.displayName ?: "Google user",
                                userId = response.userId ?: -1L
                            )

                            Toast.makeText(this@AuthActivity, "Google login successful", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@AuthActivity, MainActivity::class.java))
                            finishAffinity()
                        }

                    } catch (e: HttpException) {
                        e.printStackTrace()
                        Toast.makeText(this@AuthActivity, "HTTP ${e.code()}", Toast.LENGTH_LONG).show()
                    } catch (e: IOException) {
                        e.printStackTrace()
                        Toast.makeText(this@AuthActivity, "IO: ${e.javaClass.simpleName}: ${e.message}", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(this@AuthActivity, "EX: ${e.javaClass.simpleName}: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
                .onFailure { e ->
                    e.printStackTrace()
                    Log.e("GOOGLE_AUTH", "Google sign-in failed", e)

                    Toast.makeText(
                        this@AuthActivity,
                        "${e.javaClass.simpleName}: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }
}