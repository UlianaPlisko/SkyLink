package com.codepalace.accelerometer.ui.activity

import com.codepalace.accelerometer.R
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.codepalace.accelerometer.api.ApiClient
import com.codepalace.accelerometer.api.ApiErrorMapper
import com.codepalace.accelerometer.api.dto.CompleteGoogleRequest
import com.codepalace.accelerometer.data.model.enums.UserRole
import com.codepalace.accelerometer.ui.MessageKind
import com.codepalace.accelerometer.ui.showAppMessage
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import android.widget.ImageButton

class GoogleCompleteActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiClient.init(this)
        setContentView(R.layout.activity_google_complete)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val etDisplayName = findViewById<EditText>(R.id.etGoogleDisplayName)
        val rgRole = findViewById<RadioGroup>(R.id.rgRole)
        val btnComplete = findViewById<Button>(R.id.btnCompleteGoogle)

        val prefillName = intent.getStringExtra("prefill_display_name").orEmpty()
        etDisplayName.setText(prefillName)

        btnBack.setOnClickListener {
            finish()
        }

        btnComplete.setOnClickListener {
            val displayName = etDisplayName.text.toString().trim()

            val role = when (rgRole.checkedRadioButtonId) {
                R.id.rbObserver -> UserRole.OBSERVER
                R.id.rbContributor -> UserRole.CONTRIBUTOR
                else -> null
            }

            when {
                displayName.length < 3 -> {
                    etDisplayName.error = "Minimum 3 characters"
                }
                role == null -> {
                    showAppMessage("Select your role.", MessageKind.ERROR)
                }
                else -> {
                    completeGoogle(displayName, role)
                }
            }
        }
    }

    private fun completeGoogle(displayName: String, role: UserRole) {
        val pendingToken = ApiClient.getSessionStorage().getPendingGoogleToken()

        if (pendingToken.isNullOrBlank()) {
            showAppMessage("Google session expired. Try signing in again.", MessageKind.ERROR)
            return
        }

        lifecycleScope.launch {
            try {
                val response = ApiClient.authApi.completeGoogle(
                    CompleteGoogleRequest(
                        pendingToken = pendingToken,
                        displayName = displayName,
                        role = role
                    )
                )

                ApiClient.getSessionStorage().saveAuth(
                    token = response.token,
                    role = response.role.name,
                    displayName = response.displayName,
                    userId = response.userId,
                    provider = "GOOGLE"
                )
                ApiClient.getSessionStorage().clearPendingGoogleToken()

                showAppMessage("Google registration completed.", MessageKind.SUCCESS)
                startActivity(Intent(this@GoogleCompleteActivity, MainActivity::class.java))
                finishAffinity()

            } catch (e: HttpException) {
                showAppMessage(
                    ApiErrorMapper.fromHttpException(e, "Could not complete Google registration."),
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
