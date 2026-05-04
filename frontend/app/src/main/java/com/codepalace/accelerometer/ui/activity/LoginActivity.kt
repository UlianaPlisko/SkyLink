package com.codepalace.accelerometer.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.codepalace.accelerometer.R
import com.codepalace.accelerometer.api.ApiClient
import com.codepalace.accelerometer.api.ApiErrorMapper
import com.codepalace.accelerometer.auth.GoogleSignInCoordinator
import com.codepalace.accelerometer.data.local.AppDatabase
import com.codepalace.accelerometer.data.repository.AuthRepository
import com.codepalace.accelerometer.data.repository.EventRepository
import com.codepalace.accelerometer.notification.MyFirebaseMessagingService
import com.codepalace.accelerometer.notification.MyFirebaseMessagingService.Companion.sendSavedTokenIfExists
import com.codepalace.accelerometer.ui.MessageKind
import com.codepalace.accelerometer.ui.showAppMessage
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class LoginActivity : AppCompatActivity() {

    private val authRepository = AuthRepository()

    private lateinit var eventRepository: EventRepository
    private lateinit var googleSignInCoordinator: GoogleSignInCoordinator

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiClient.init(this)
        val database = AppDatabase.getDatabase(this)
        eventRepository = EventRepository(
            api = ApiClient.eventApi,
            database = database
        )
        setContentView(R.layout.activity_login)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val etLoginUsername = findViewById<EditText>(R.id.etLoginUsername)
        val etLoginPassword = findViewById<EditText>(R.id.etLoginPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnGoogle = findViewById<Button>(R.id.btnGoogle)
        val tvGoSignupFromLogin = findViewById<TextView>(R.id.tvGoSignupFromLogin)
        googleSignInCoordinator = GoogleSignInCoordinator(this)

        btnBack.setOnClickListener {
            finish()
        }

        tvGoSignupFromLogin.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        btnLogin.setOnClickListener {
            val email = etLoginUsername.text.toString().trim()
            val password = etLoginPassword.text.toString()

            when {
                email.isBlank() -> etLoginUsername.error = "Email is required"
                password.isBlank() -> etLoginPassword.error = "Password is required"
                else -> doLogin(email, password)
            }
        }

        btnGoogle.setOnClickListener {
            googleSignInCoordinator.start()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun doLogin(email: String, password: String) {
        lifecycleScope.launch {
            try {
                val response = authRepository.login(email, password)

                ApiClient.getSessionStorage().saveAuth(
                    token = response.token,
                    role = response.role.name,
                    displayName = response.displayName,
                    userId = response.userId,
                    provider = "LOCAL"
                )

                sendSavedTokenIfExists(this@LoginActivity)

                eventRepository.clearEventsCache()

                showAppMessage("Welcome back, ${response.displayName}.", MessageKind.SUCCESS)

                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finishAffinity()

            } catch (e: HttpException) {
                showAppMessage(
                    ApiErrorMapper.fromHttpException(e, "Unable to log in right now. Try again."),
                    MessageKind.ERROR
                )
            } catch (e: IOException) {
                showAppMessage(ApiErrorMapper.fromIOException(e), MessageKind.ERROR)
            } catch (e: Exception) {
                showAppMessage(ApiErrorMapper.fromThrowable(e), MessageKind.ERROR)
            }
        }
    }
}
