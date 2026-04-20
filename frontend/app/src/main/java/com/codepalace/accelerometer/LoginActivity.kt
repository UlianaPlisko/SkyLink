package com.codepalace.accelerometer

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.codepalace.accelerometer.api.ApiClient
import com.codepalace.accelerometer.repository.AuthRepository
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class LoginActivity : AppCompatActivity() {

    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiClient.init(this)
        setContentView(R.layout.activity_login)

        val tvBackLogin = findViewById<TextView>(R.id.tvBackLogin)
        val etLoginUsername = findViewById<EditText>(R.id.etLoginUsername)
        val etLoginPassword = findViewById<EditText>(R.id.etLoginPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvGoSignupFromLogin = findViewById<TextView>(R.id.tvGoSignupFromLogin)

        tvBackLogin.setOnClickListener {
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
    }

    private fun doLogin(email: String, password: String) {
        lifecycleScope.launch {
            try {
                val response = authRepository.login(email, password)

                ApiClient.getSessionStorage().saveAuth(
                    token = response.token,
                    role = response.role.name,
                    displayName = response.displayName,
                    userId = response.userId
                )

                Toast.makeText(this@LoginActivity, "Login successful", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finishAffinity()

            } catch (e: HttpException) {
                Toast.makeText(this@LoginActivity, "Login failed: ${e.code()}", Toast.LENGTH_LONG).show()
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this@LoginActivity, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, e.message ?: "Unexpected error", Toast.LENGTH_LONG).show()
            }
        }
    }
}