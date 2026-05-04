package com.codepalace.accelerometer.ui.activity

import com.codepalace.accelerometer.R
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.codepalace.accelerometer.api.ApiClient
import android.widget.ImageButton
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import com.codepalace.accelerometer.data.local.AppDatabase
import com.codepalace.accelerometer.data.repository.EventRepository
import com.codepalace.accelerometer.ui.MessageKind
import com.codepalace.accelerometer.ui.showAppMessage
import kotlinx.coroutines.launch

class MenuActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var itemProfile: LinearLayout
    private lateinit var itemSkyCultures: LinearLayout
    private lateinit var itemSettings: LinearLayout
    private lateinit var itemAuthentication: LinearLayout
    private lateinit var itemLogout: LinearLayout

    private lateinit var eventRepository: EventRepository

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiClient.init(this)

        val database = AppDatabase.getDatabase(this)
        eventRepository = EventRepository(
            api = ApiClient.eventApi,
            database = database
        )
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
            startActivity(Intent(this, SkyCulturesActivity::class.java))
        }

        itemSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        itemAuthentication.setOnClickListener {
            startActivity(Intent(this, AuthActivity::class.java))
        }

        itemLogout.setOnClickListener {
            handleLogout()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleLogout() {
        lifecycleScope.launch {                    // ← This is the fix
            try {
                eventRepository.clearEventsCache()   // Now safe to call

                ApiClient.getSessionStorage().clearAuth()

                showAppMessage("Logged out successfully.", MessageKind.SUCCESS)

                // Go back to login screen and clear back stack
                startActivity(Intent(this@MenuActivity, LoginActivity::class.java))
                finishAffinity()   // Clears all previous activities

            } catch (e: Exception) {
                Log.e("Logout", "Error during logout", e)
                showAppMessage("Logged out (with minor issue)", MessageKind.INFO)
                startActivity(Intent(this@MenuActivity, LoginActivity::class.java))
                finishAffinity()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        updateMenuUi()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateMenuUi() {
        val loggedIn = ApiClient.getSessionStorage().isLoggedIn()

        itemAuthentication.visibility = if (loggedIn) View.GONE else View.VISIBLE
        itemLogout.visibility = if (loggedIn) View.VISIBLE else View.GONE
    }
}
