package com.codepalace.accelerometer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.codepalace.accelerometer.api.ApiClient
import android.widget.ImageButton
import com.codepalace.accelerometer.ui.MessageKind
import com.codepalace.accelerometer.ui.showAppMessage
class MenuActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var itemProfile: LinearLayout
    private lateinit var itemSkyCultures: LinearLayout
    private lateinit var itemSettings: LinearLayout
    private lateinit var itemAuthentication: LinearLayout
    private lateinit var itemLogout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiClient.init(this)
        setContentView(R.layout.activity_menu)

        btnBack = findViewById(R.id.btnBack)
        itemProfile = findViewById(R.id.itemProfile)
        itemSkyCultures = findViewById(R.id.itemSkyCultures)
        itemSettings = findViewById(R.id.itemSettings)
        itemAuthentication = findViewById(R.id.itemAuthentication)
        itemLogout = findViewById(R.id.itemLogout)

        updateMenuUi()

        btnBack.setOnClickListener {
            finish()
        }

        itemProfile.setOnClickListener {
            if (ApiClient.getSessionStorage().isLoggedIn()) {
                startActivity(Intent(this, ProfileActivity::class.java))
            } else {
                showAppMessage("Log in to open your profile.", MessageKind.INFO)
                startActivity(Intent(this, AuthActivity::class.java))
            }
        }

        itemSkyCultures.setOnClickListener {
            showAppMessage("Sky cultures will be available soon.", MessageKind.INFO)
        }

        itemSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        itemAuthentication.setOnClickListener {
            startActivity(Intent(this, AuthActivity::class.java))
        }

        itemLogout.setOnClickListener {
            ApiClient.getSessionStorage().clearAuth()
            showAppMessage("Logged out.", MessageKind.SUCCESS)
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        updateMenuUi()
    }

    private fun updateMenuUi() {
        val loggedIn = ApiClient.getSessionStorage().isLoggedIn()

        itemAuthentication.visibility = if (loggedIn) View.GONE else View.VISIBLE
        itemLogout.visibility = if (loggedIn) View.VISIBLE else View.GONE
    }
}
