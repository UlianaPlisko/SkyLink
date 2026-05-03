package com.codepalace.accelerometer.ui.activity

import com.codepalace.accelerometer.R
import com.codepalace.accelerometer.auth.GoogleSignInCoordinator
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.codepalace.accelerometer.api.ApiClient

class AuthActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var googleSignInCoordinator: GoogleSignInCoordinator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiClient.init(this)
        setContentView(R.layout.activity_auth)

        btnBack = findViewById(R.id.btnBack)

        btnBack.setOnClickListener {
            finish()
        }

        googleSignInCoordinator = GoogleSignInCoordinator(this)

        findViewById<Button>(R.id.btnGoLogin).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        findViewById<Button>(R.id.btnGoSignup).setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        findViewById<Button>(R.id.btnGoogle).setOnClickListener {
            googleSignInCoordinator.start()
        }
    }
}
