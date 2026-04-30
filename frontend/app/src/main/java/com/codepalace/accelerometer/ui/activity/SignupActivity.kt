package com.codepalace.accelerometer.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.codepalace.accelerometer.R
import com.codepalace.accelerometer.api.ApiClient
import com.codepalace.accelerometer.api.ApiErrorMapper
import com.codepalace.accelerometer.data.model.enums.UserRole
import com.codepalace.accelerometer.data.repository.AuthRepository
import com.codepalace.accelerometer.ui.MessageKind
import com.codepalace.accelerometer.ui.showAppMessage
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class SignupActivity : AppCompatActivity() {

    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiClient.init(this)
        setContentView(R.layout.activity_signup)
        applyTopBarInsets(findViewById(R.id.headerBar), extraTopDp = 0)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val etSignupUsername = findViewById<EditText>(R.id.etSignupUsername)
        val etSignupEmail = findViewById<EditText>(R.id.etSignupEmail)
        val etSignupPassword = findViewById<EditText>(R.id.etSignupPassword)
        val etSignupRepeatPassword = findViewById<EditText>(R.id.etSignupRepeatPassword)
        val rgRole = findViewById<RadioGroup>(R.id.rgRole)
        val btnSignup = findViewById<Button>(R.id.btnSignup)
        val tvGoLoginFromSignup = findViewById<TextView>(R.id.tvGoLoginFromSignup)

        btnBack.setOnClickListener {
            finish()
        }

        tvGoLoginFromSignup.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        btnSignup.setOnClickListener {
            val displayName = etSignupUsername.text.toString().trim()
            val email = etSignupEmail.text.toString().trim()
            val password = etSignupPassword.text.toString()
            val repeatPassword = etSignupRepeatPassword.text.toString()

            val role = when (rgRole.checkedRadioButtonId) {
                R.id.rbObserver -> UserRole.OBSERVER
                R.id.rbContributor -> UserRole.CONTRIBUTOR
                else -> null
            }

            when {
                displayName.length < 3 ->
                    etSignupUsername.error = "Minimum 3 characters"

                email.isBlank() ->
                    etSignupEmail.error = "Email is required"

                password.length < 8 ->
                    etSignupPassword.error = "Minimum 8 characters"

                password != repeatPassword ->
                    etSignupRepeatPassword.error = "Passwords do not match"

                role == null ->
                    showAppMessage("Select your role.", MessageKind.ERROR)

                else -> doSignup(
                    email = email,
                    displayName = displayName,
                    password = password,
                    role = role
                )
            }
        }
    }

    private fun doSignup(
        email: String,
        displayName: String,
        password: String,
        role: UserRole
    ) {
        lifecycleScope.launch {
            try {
                val response = authRepository.register(
                    email = email,
                    displayName = displayName,
                    password = password,
                    role = role
                )

                ApiClient.getSessionStorage().saveAuth(
                    token = response.token,
                    role = response.role.name,
                    displayName = response.displayName,
                    userId = response.userId,
                    provider = "LOCAL"
                )

                showAppMessage("Account created successfully.", MessageKind.SUCCESS)
                startActivity(Intent(this@SignupActivity, MainActivity::class.java))
                finishAffinity()

            } catch (e: HttpException) {
                showAppMessage(
                    ApiErrorMapper.fromHttpException(
                        e,
                        "Could not create account. This email may already be registered."
                    ),
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
