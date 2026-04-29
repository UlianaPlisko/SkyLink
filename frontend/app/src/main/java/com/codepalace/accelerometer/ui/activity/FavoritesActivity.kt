package com.codepalace.accelerometer.ui.activity

import com.codepalace.accelerometer.R
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.codepalace.accelerometer.api.ApiClient
import com.codepalace.accelerometer.api.ApiErrorMapper
import com.codepalace.accelerometer.api.dto.FavoriteResponse
import com.codepalace.accelerometer.data.local.AppDatabase
import com.codepalace.accelerometer.data.repository.FavoriteRepository
import com.codepalace.accelerometer.ui.MessageKind
import com.codepalace.accelerometer.ui.showAppMessage
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

@Suppress("DEPRECATION")
class FavoritesActivity : AppCompatActivity() {

    private lateinit var titleText: TextView
    private lateinit var container: LinearLayout
    private lateinit var favoriteRepository: FavoriteRepository
    private var didLoadFavorites = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ApiClient.init(this)

        if (!ApiClient.getSessionStorage().isLoggedIn()) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_favorites)
        favoriteRepository = FavoriteRepository(
            favoriteApi = ApiClient.favoriteApi,
            celestialApi = ApiClient.celestialApi,
            database = AppDatabase.getDatabase(this)
        )

        applyTopBarInsets(findViewById(R.id.headerBar))

        titleText = findViewById(R.id.tvFavoritesTitle)
        container = findViewById(R.id.favoritesContainer)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        syncHeaderWithProfile()
        loadFavorites()
    }

    override fun onResume() {
        super.onResume()
        if (didLoadFavorites) {
            loadFavorites()
        }
    }

    private fun syncHeaderWithProfile() {
        titleText.text = "Favorites"
    }

    private fun loadFavorites() {
        lifecycleScope.launch {
            didLoadFavorites = true
            val cachedFavorites = favoriteRepository.getCachedFavorites()
            if (cachedFavorites.isNotEmpty()) {
                showFavorites(cachedFavorites)
            } else {
                showLoading()
            }

            try {
                val favorites = favoriteRepository.refreshFavorites()

                if (favorites.isEmpty()) {
                    showEmpty()
                } else {
                    showFavorites(favorites)
                }

            } catch (e: HttpException) {
                showOfflineOrError(
                    cachedFavorites,
                    ApiErrorMapper.fromHttpException(
                        e, "Could not load favorites."
                    )
                )
            } catch (e: IOException) {
                showOfflineOrError(cachedFavorites, "Offline mode: showing saved favorites.")
            }
        }
    }

    private fun showLoading() {
        showStatus("Loading favorites...")
    }

    private fun showEmpty() {
        showStatus("You have no favorite objects yet.")
    }

    private fun showError(message: String) {
        showStatus(message)
        showAppMessage(message, MessageKind.ERROR)
    }

    private fun showOfflineOrError(cachedFavorites: List<FavoriteResponse>, message: String) {
        if (cachedFavorites.isEmpty()) {
            showError(message)
        } else {
            showFavorites(cachedFavorites)
            showAppMessage(message, MessageKind.INFO)
        }
    }

    private fun showFavorites(favorites: List<FavoriteResponse>) {
        container.removeAllViews()

        favorites.forEach { favorite ->
            container.addView(createFavoriteCard(favorite))
        }
    }

    private fun showStatus(message: String) {
        container.removeAllViews()
        container.addView(
            TextView(this).apply {
                text = message
                setTextColor(getColor(R.color.color_accent))
                textSize = 18f
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
        )
    }

    private fun createFavoriteCard(favorite: FavoriteResponse): View {
        val spaceObject = favorite.spaceObject

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card_menu)
            setPadding(28, 22, 28, 22)
            isClickable = true
            isFocusable = true
            contentDescription = "Open ${spaceObject.displayName} details"

            val selectable = TypedValue()
            theme.resolveAttribute(android.R.attr.selectableItemBackground, selectable, true)
            foreground = ContextCompat.getDrawable(this@FavoritesActivity, selectable.resourceId)

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }

            setOnClickListener {
                val intent = Intent(this@FavoritesActivity, StarDetailActivity::class.java).apply {
                    putExtra("star_id", spaceObject.id)
                    putExtra("star_name", spaceObject.displayName)
                }

                startActivity(intent)
                overridePendingTransition(R.anim.slide_up_in, R.anim.stay)
            }
        }

        val objectNameText = TextView(this).apply {
            text = spaceObject.displayName
            setTextColor(getColor(R.color.color_primary))
            textSize = 18f
        }

        val objectTypeText = TextView(this).apply {
            text = spaceObject.objectType
            setTextColor(getColor(R.color.color_primary))
            textSize = 14f
            alpha = 0.85f
        }

        val magnitudeText = TextView(this).apply {
            text = "Magnitude: ${spaceObject.magnitude}"
            setTextColor(getColor(R.color.color_primary))
            textSize = 14f
            alpha = 0.75f
        }

        val coordinatesText = TextView(this).apply {
            text = "RA: ${spaceObject.raDeg}, Dec: ${spaceObject.decDeg}"
            setTextColor(getColor(R.color.color_primary))
            textSize = 13f
            alpha = 0.7f
        }

        val favoriteVisibilityText = TextView(this).apply {
            text = "Visibility: ${favorite.visibility}"
            setTextColor(getColor(R.color.color_primary))
            textSize = 13f
            alpha = 0.7f
        }

        val noteText = TextView(this).apply {
            text = favorite.note
                ?.takeIf { it.isNotBlank() }
                ?.let { "Note: $it" }
                ?: "No note"

            setTextColor(getColor(R.color.color_primary))
            textSize = 13f
            alpha = 0.7f
        }

        val addedAtText = TextView(this).apply {
            text = favorite.addedAt?.let { "Added: $it" } ?: ""
            setTextColor(getColor(R.color.color_primary))
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
