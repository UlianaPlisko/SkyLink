package com.codepalace.accelerometer

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.codepalace.accelerometer.api.ApiClient
import com.codepalace.accelerometer.data.model.enums.UserRole
import com.codepalace.accelerometer.repository.AuthRepository
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import android.widget.ImageButton

class SignupActivity : AppCompatActivity() {

    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiClient.init(this)
        setContentView(R.layout.activity_signup)

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
                    Toast.makeText(this, "Select role", Toast.LENGTH_SHORT).show()

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
                    userId = response.userId
                )

                Toast.makeText(this@SignupActivity, "Registration successful", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@SignupActivity, MainActivity::class.java))
                finishAffinity()

            } catch (e: HttpException) {
                Toast.makeText(this@SignupActivity, "Registration failed: ${e.code()}", Toast.LENGTH_LONG).show()
            } catch (_: IOException) {
                Toast.makeText(this@SignupActivity, "Network error", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this@SignupActivity, e.message ?: "Unexpected error", Toast.LENGTH_LONG).show()
            }
        }
    }
}