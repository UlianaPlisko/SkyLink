package com.codepalace.accelerometer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.codepalace.accelerometer.api.ApiClient
import com.codepalace.accelerometer.api.ApiErrorMapper
import com.codepalace.accelerometer.api.dto.FavoriteResponse
import com.codepalace.accelerometer.ui.MessageKind
import com.codepalace.accelerometer.ui.showAppMessage
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class FavoritesActivity : AppCompatActivity() {

    private lateinit var titleText: TextView
    private lateinit var container: LinearLayout
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ApiClient.init(this)

        if (!ApiClient.getSessionStorage().isLoggedIn()) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_favorites)

        titleText = findViewById(R.id.tvFavoritesTitle)
        container = findViewById(R.id.favoritesContainer)
        statusText = findViewById(R.id.tvFavoritesStatus)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        syncHeaderWithProfile()
        loadFavorites()
    }

    private fun syncHeaderWithProfile() {
        titleText.text = "Favorites"
    }

    private fun loadFavorites() {
        lifecycleScope.launch {
            try {
                showLoading()

                val favorites = ApiClient.favoriteApi.getFavorites()

                if (favorites.isEmpty()) {
                    showEmpty()
                } else {
                    showFavorites(favorites)
                }

            } catch (e: HttpException) {
                showError(
                    ApiErrorMapper.fromHttpException(
                        e,
                        "Could not load favorites."
                    )
                )
            } catch (e: IOException) {
                showError(ApiErrorMapper.fromIOException(e))
            }
        }
    }

    private fun showLoading() {
        container.removeAllViews()

        statusText.visibility = View.VISIBLE
        statusText.text = "Loading favorites..."

        container.addView(statusText)
    }

    private fun showEmpty() {
        container.removeAllViews()

        statusText.visibility = View.VISIBLE
        statusText.text = "You have no favorite objects yet."

        container.addView(statusText)
    }

    private fun showError(message: String) {
        container.removeAllViews()

        statusText.visibility = View.VISIBLE
        statusText.text = message

        container.addView(statusText)

        showAppMessage(message, MessageKind.ERROR)
    }

    private fun showFavorites(favorites: List<FavoriteResponse>) {
        container.removeAllViews()

        favorites.forEach { favorite ->
            container.addView(createFavoriteCard(favorite))
        }
    }

    private fun createFavoriteCard(favorite: FavoriteResponse): View {
        val spaceObject = favorite.spaceObject

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_profile_section)
            setPadding(28, 22, 28, 22)

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
        }

        val objectNameText = TextView(this).apply {
            text = spaceObject.displayName
            setTextColor(getColor(R.color.dark_blue))
            textSize = 18f
        }

        val objectTypeText = TextView(this).apply {
            text = spaceObject.objectType
            setTextColor(getColor(R.color.dark_blue))
            textSize = 14f
            alpha = 0.85f
        }

        val magnitudeText = TextView(this).apply {
            text = "Magnitude: ${spaceObject.magnitude}"
            setTextColor(getColor(R.color.dark_blue))
            textSize = 14f
            alpha = 0.75f
        }

        val coordinatesText = TextView(this).apply {
            text = "RA: ${spaceObject.raDeg}, Dec: ${spaceObject.decDeg}"
            setTextColor(getColor(R.color.dark_blue))
            textSize = 13f
            alpha = 0.7f
        }

        val favoriteVisibilityText = TextView(this).apply {
            text = "Visibility: ${favorite.visibility}"
            setTextColor(getColor(R.color.dark_blue))
            textSize = 13f
            alpha = 0.7f
        }

        val noteText = TextView(this).apply {
            text = favorite.note
                ?.takeIf { it.isNotBlank() }
                ?.let { "Note: $it" }
                ?: "No note"

            setTextColor(getColor(R.color.dark_blue))
            textSize = 13f
            alpha = 0.7f
        }

        val addedAtText = TextView(this).apply {
            text = favorite.addedAt?.let { "Added: $it" } ?: ""
            setTextColor(getColor(R.color.dark_blue))
            textSize = 12f
            alpha = 0.6f

            visibility = if (favorite.addedAt.isNullOrBlank()) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }

        card.addView(objectNameText)
        card.addView(objectTypeText)
        card.addView(magnitudeText)
        card.addView(coordinatesText)
        card.addView(favoriteVisibilityText)
        card.addView(noteText)
        card.addView(addedAtText)

        return card
    }
}