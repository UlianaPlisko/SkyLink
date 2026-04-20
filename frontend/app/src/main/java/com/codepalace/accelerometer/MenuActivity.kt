package com.codepalace.accelerometer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.codepalace.accelerometer.api.ApiClient

class MenuActivity : AppCompatActivity() {

    private lateinit var btnBackMenu: TextView
    private lateinit var itemProfile: LinearLayout
    private lateinit var itemSkyCultures: LinearLayout
    private lateinit var itemSettings: LinearLayout
    private lateinit var itemAuthentication: LinearLayout
    private lateinit var itemLogout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiClient.init(this)
        setContentView(R.layout.activity_menu)

        btnBackMenu = findViewById(R.id.btnBackMenu)
        itemProfile = findViewById(R.id.itemProfile)
        itemSkyCultures = findViewById(R.id.itemSkyCultures)
        itemSettings = findViewById(R.id.itemSettings)
        itemAuthentication = findViewById(R.id.itemAuthentication)
        itemLogout = findViewById(R.id.itemLogout)

        updateMenuUi()

        btnBackMenu.setOnClickListener {
            finish()
        }

        itemProfile.setOnClickListener {
            if (ApiClient.getSessionStorage().isLoggedIn()) {
                //ProfileActivity
            } else {
                startActivity(Intent(this, AuthActivity::class.java))
            }
        }

        itemSkyCultures.setOnClickListener {
            Toast.makeText(this, "Sky Cultures later", Toast.LENGTH_SHORT).show()
        }

        itemSettings.setOnClickListener {
            Toast.makeText(this, "Settings later", Toast.LENGTH_SHORT).show()
        }

        itemAuthentication.setOnClickListener {
            startActivity(Intent(this, AuthActivity::class.java))
        }

        itemLogout.setOnClickListener {
            ApiClient.getSessionStorage().clearAuth()
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
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