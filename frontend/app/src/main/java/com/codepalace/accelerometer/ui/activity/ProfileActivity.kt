package com.codepalace.accelerometer.ui.activity

import com.codepalace.accelerometer.R
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.request.CachePolicy
import coil.load
import coil.transform.CircleCropTransformation
import com.codepalace.accelerometer.api.ApiClient
import com.codepalace.accelerometer.api.ApiErrorMapper
import com.codepalace.accelerometer.api.dto.UpdateProfileRequest
import com.codepalace.accelerometer.api.dto.UserProfileResponse
import com.codepalace.accelerometer.config.ApiConfig
import com.codepalace.accelerometer.data.local.ProfileImageCache
import com.codepalace.accelerometer.ui.MessageKind
import com.codepalace.accelerometer.ui.showAppMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.IOException
import java.util.Locale

class ProfileActivity : AppCompatActivity() {

    private lateinit var tvTitle: TextView
    private lateinit var avatarImage: ImageView
    private lateinit var avatarInitial: TextView
    private lateinit var tvName: TextView
    private lateinit var tvRole: TextView
    private lateinit var profileImageCache: ProfileImageCache

    private var currentProfile: UserProfileResponse? = null

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            uploadProfilePicture(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiClient.init(this)

        if (!ApiClient.getSessionStorage().isLoggedIn()) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_profile)
        profileImageCache = ProfileImageCache(this)

        applyTopBarInsets(findViewById(R.id.headerBar))

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        tvTitle = findViewById(R.id.tvProfileTitle)
        avatarImage = findViewById(R.id.ivProfileAvatar)
        avatarInitial = findViewById(R.id.tvProfileInitial)
        tvName = findViewById(R.id.tvProfileName)
        tvRole = findViewById(R.id.tvProfileRole)

        val editPicture = findViewById<TextView>(R.id.tvEditProfilePicture)
        val avatarFrame = findViewById<FrameLayout>(R.id.avatarFrame)
        val nameRow = findViewById<LinearLayout>(R.id.nameRow)

        editPicture.setOnClickListener { imagePicker.launch("image/*") }
        avatarFrame.setOnClickListener { imagePicker.launch("image/*") }
        nameRow.setOnClickListener { showEditNameDialog() }

        findViewById<LinearLayout>(R.id.favoritesSection).setOnClickListener {
            startActivity(Intent(this, FavoritesActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.eventsSection).setOnClickListener {
            showAppMessage("Events will appear here soon.", MessageKind.INFO)
        }

        applySessionProfile()
        loadProfile()
    }

    private fun applySessionProfile() {
        val session = ApiClient.getSessionStorage()
        val name = session.getDisplayName().orEmpty().ifBlank { "User" }
        tvTitle.text = name
        tvName.text = name
        tvRole.text = session.getRole()?.toDisplayRole().orEmpty()
        avatarInitial.text = name.first().uppercaseChar().toString()
        showCachedProfilePicture()
    }

    private fun loadProfile() {
        lifecycleScope.launch {
            try {
                val profile = ApiClient.profileApi.getProfile()
                currentProfile = profile
                saveProfileInSession(profile)
                applyProfile(profile)
                cacheRemoteProfilePicture(profile)
            } catch (e: HttpException) {
                showAppMessage(
                    ApiErrorMapper.fromHttpException(e, "Could not load profile."),
                    MessageKind.ERROR
                )
            } catch (e: IOException) {
                showCachedProfilePicture()
                showAppMessage("Offline mode: showing saved profile.", MessageKind.INFO)
            }
        }
    }

    private fun applyProfile(profile: UserProfileResponse) {
        tvTitle.text = profile.displayName
        tvName.text = profile.displayName
        tvRole.text = profile.role.name.toDisplayRole()
        avatarInitial.text = profile.displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "U"

        if (profile.pfpUrl.isNullOrBlank()) {
            avatarImage.setImageDrawable(null)
            avatarInitial.visibility = View.VISIBLE
        } else {
            val token = ApiClient.getSessionStorage().getToken()
            avatarImage.load(absoluteUrl(profile.pfpUrl)) {
                token?.let { addHeader("Authorization", "Bearer $it") }
                transformations(CircleCropTransformation())
                diskCachePolicy(CachePolicy.ENABLED)
                memoryCachePolicy(CachePolicy.ENABLED)
                listener(
                    onSuccess = { _, _ -> avatarInitial.visibility = View.GONE },
                    onError = { _, _ -> showCachedProfilePicture() }
                )
            }
        }
    }

    private fun showEditNameDialog() {
        val profileName = currentProfile?.displayName
            ?: ApiClient.getSessionStorage().getDisplayName().orEmpty()

        val input = EditText(this).apply {
            setText(profileName)
            selectAll()
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            setSingleLine(true)
            setPadding(32, 18, 32, 18)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Edit profile name")
            .setView(input)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val newName = input.text.toString().trim()
                if (newName.length < 3) {
                    input.error = "Minimum 3 characters"
                    return@setOnClickListener
                }

                dialog.dismiss()
                updateDisplayName(newName)
            }
        }

        dialog.show()
    }

    private fun updateDisplayName(newName: String) {
        lifecycleScope.launch {
            try {
                val profile = ApiClient.profileApi.updateProfile(UpdateProfileRequest(newName))
                currentProfile = profile

                saveProfileInSession(profile)

                applyProfile(profile)
                showAppMessage("Profile name updated.", MessageKind.SUCCESS)
            } catch (e: HttpException) {
                showAppMessage(
                    ApiErrorMapper.fromHttpException(e, "Could not update profile name."),
                    MessageKind.ERROR
                )
            } catch (e: IOException) {
                showAppMessage(
                    "You are offline. Profile name can be changed when internet is back.",
                    MessageKind.INFO
                )
            }
        }
    }

    private fun uploadProfilePicture(uri: Uri) {
        lifecycleScope.launch {
            try {
                val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
                val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }

                if (bytes == null || bytes.isEmpty()) {
                    showAppMessage("Choose an image before uploading.", MessageKind.ERROR)
                    return@launch
                }

                val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData(
                    "file",
                    "profile-picture",
                    requestBody
                )

                val response = ApiClient.profileApi.uploadProfilePicture(part)
                if (!response.isSuccessful) {
                    throw HttpException(response)
                }

                savePickedProfilePicture(uri)
                showAppMessage("Profile picture updated.", MessageKind.SUCCESS)
                loadProfile()
            } catch (e: HttpException) {
                showAppMessage(
                    ApiErrorMapper.fromHttpException(e, "Could not upload profile picture."),
                    MessageKind.ERROR
                )
            } catch (e: IOException) {
                showAppMessage(
                    "You are offline. Profile picture can be changed when internet is back.",
                    MessageKind.INFO
                )
            }
        }
    }

    private fun saveProfileInSession(profile: UserProfileResponse) {
        val session = ApiClient.getSessionStorage()
        session.updateUserProfile(
            role = profile.role.name,
            displayName = profile.displayName,
            userId = profile.id,
            provider = profile.provider ?: session.getAuthProvider() ?: "LOCAL"
        )
    }

    private fun showCachedProfilePicture(): Boolean {
        val userId = ApiClient.getSessionStorage().getUserId()
        val cachedFile = profileImageCache.getProfilePicture(userId) ?: return false

        avatarImage.load(cachedFile) {
            transformations(CircleCropTransformation())
            listener(
                onSuccess = { _, _ -> avatarInitial.visibility = View.GONE },
                onError = { _, _ -> avatarInitial.visibility = View.VISIBLE }
            )
        }

        return true
    }

    private fun savePickedProfilePicture(uri: Uri) {
        val userId = ApiClient.getSessionStorage().getUserId()
        profileImageCache.saveProfilePicture(userId, uri)?.let { file ->
            avatarImage.load(file) {
                transformations(CircleCropTransformation())
                listener(
                    onSuccess = { _, _ -> avatarInitial.visibility = View.GONE },
                    onError = { _, _ -> avatarInitial.visibility = View.VISIBLE }
                )
            }
        }
    }

    private fun cacheRemoteProfilePicture(profile: UserProfileResponse) {
        val pfpUrl = profile.pfpUrl?.takeIf { it.isNotBlank() } ?: return
        val userId = profile.id
        val token = ApiClient.getSessionStorage().getToken()

        lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder()
                    .url(absoluteUrl(pfpUrl))
                    .apply {
                        token?.let { header("Authorization", "Bearer $it") }
                    }
                    .build()

                OkHttpClient().newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        response.body?.bytes()?.takeIf { it.isNotEmpty() }?.let { bytes ->
                            profileImageCache.saveProfilePicture(userId, bytes)
                        }
                    }
                }
            }
        }
    }

    private fun absoluteUrl(path: String): String {
        return if (path.startsWith("http")) {
            path
        } else {
            ApiConfig.BASE_URL.trimEnd('/') + "/" + path.trimStart('/')
        }
    }

    private fun String.toDisplayRole(): String {
        return lowercase(Locale.US)
            .replaceFirstChar { it.titlecase(Locale.US) }
    }
}
