package com.codepalace.accelerometer.ui.activity

import com.codepalace.accelerometer.R
import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.codepalace.accelerometer.api.ApiClient
import com.codepalace.accelerometer.api.ApiErrorMapper
import com.codepalace.accelerometer.api.dto.ChangePasswordRequest
import com.codepalace.accelerometer.ui.MessageKind
import com.codepalace.accelerometer.ui.showAppMessage
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class AccountSettingsActivity : AppCompatActivity() {

    private lateinit var tvEmail: TextView
    private lateinit var itemChangePassword: LinearLayout

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiClient.init(this)

        if (!ApiClient.getSessionStorage().isLoggedIn()) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_account_settings)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        tvEmail = findViewById(R.id.tvAccountEmail)
        itemChangePassword = findViewById(R.id.itemChangePassword)
        updatePasswordRow(ApiClient.getSessionStorage().getAuthProvider())

        itemChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        findViewById<LinearLayout>(R.id.itemUpgradeStatus).setOnClickListener {
            showAppMessage("Status upgrades will be available soon.", MessageKind.INFO)
        }

        loadAccount()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadAccount() {
        lifecycleScope.launch {
            try {
                val profile = ApiClient.profileApi.getProfile()
                tvEmail.text = profile.email
                updatePasswordRow(profile.provider)

                val session = ApiClient.getSessionStorage()
                session.updateUserProfile(
                    role = profile.role.name,
                    displayName = profile.displayName,
                    userId = profile.id,
                    provider = profile.provider ?: session.getAuthProvider() ?: "LOCAL"
                )
            } catch (e: HttpException) {
                showAppMessage(
                    ApiErrorMapper.fromHttpException(e, "Could not load account settings."),
                    MessageKind.ERROR
                )
            } catch (e: IOException) {
                showAppMessage(ApiErrorMapper.fromIOException(e), MessageKind.ERROR)
            }
        }
    }

    private fun updatePasswordRow(provider: String?) {
        itemChangePassword.visibility = if (provider == "GOOGLE") View.GONE else View.VISIBLE
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showChangePasswordDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 12, 32, 0)
        }

        val oldPassword = passwordField("Current password")
        val newPassword = passwordField("New password")
        val repeatPassword = passwordField("Repeat new password")

        container.addView(oldPassword)
        container.addView(newPassword)
        container.addView(repeatPassword)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Change password")
            .setView(container)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val oldValue = oldPassword.text.toString()
                val newValue = newPassword.text.toString()
                val repeatValue = repeatPassword.text.toString()

                when {
                    oldValue.isBlank() -> oldPassword.error = "Current password is required"
                    newValue.length < 8 -> newPassword.error = "Minimum 8 characters"
                    newValue != repeatValue -> repeatPassword.error = "Passwords do not match"
                    else -> {
                        dialog.dismiss()
                        changePassword(oldValue, newValue)
                    }
                }
            }
        }

        dialog.show()
    }

    private fun passwordField(hint: String): EditText {
        return EditText(this).apply {
            this.hint = hint
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setSingleLine(true)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun changePassword(oldPassword: String, newPassword: String) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.profileApi.changePassword(
                    ChangePasswordRequest(
                        oldPassword = oldPassword,
                        newPassword = newPassword
                    )
                )

                if (!response.isSuccessful) {
                    throw HttpException(response)
                }

                showAppMessage("Password changed.", MessageKind.SUCCESS)
            } catch (e: HttpException) {
                showAppMessage(
                    ApiErrorMapper.fromHttpException(e, "Could not change password."),
                    MessageKind.ERROR
                )
            } catch (e: IOException) {
                showAppMessage(ApiErrorMapper.fromIOException(e), MessageKind.ERROR)
            }
        }
    }
}
