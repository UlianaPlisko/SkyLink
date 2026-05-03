package com.codepalace.accelerometer.ui.activity

import com.codepalace.accelerometer.R
import android.app.AlertDialog
import android.content.res.ColorStateList
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
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
import com.codepalace.accelerometer.util.DisplayDateFormatter
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
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

                if (message.startsWith("Loading", ignoreCase = true)) {
                    addView(
                        ImageView(this@FavoritesActivity).apply {
                            setImageResource(R.mipmap.ic_launcher)
                            contentDescription = getString(R.string.app_name)
                            val size = 76.dp()
                            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                                bottomMargin = 18.dp()
                            }
                        }
                    )
                }

                addView(
                    TextView(this@FavoritesActivity).apply {
                        text = message
                        setTextColor(getColor(R.color.color_accent))
                        textSize = 18f
                        typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
                        gravity = Gravity.CENTER
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    }
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
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }

        val objectTypeText = TextView(this).apply {
            text = spaceObject.objectType
            setTextColor(getColor(R.color.color_primary))
            textSize = 14f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            alpha = 0.85f
        }

        val magnitudeText = TextView(this).apply {
            text = "Magnitude: ${spaceObject.magnitude}"
            setTextColor(getColor(R.color.color_primary))
            textSize = 14f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            alpha = 0.75f
        }

        val coordinatesText = TextView(this).apply {
            text = "RA: ${spaceObject.raDeg}, Dec: ${spaceObject.decDeg}"
            setTextColor(getColor(R.color.color_primary))
            textSize = 13f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            alpha = 0.7f
        }

        val favoriteVisibilityText = TextView(this).apply {
            text = "Visibility: ${favorite.visibility}"
            setTextColor(getColor(R.color.color_primary))
            textSize = 13f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            alpha = 0.7f
        }

        val noteText = TextView(this).apply {
            text = formatFavoriteNote(favorite.note)
            setTextColor(getColor(R.color.color_primary))
            textSize = 13f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            alpha = if (favorite.note.isNullOrBlank()) 0.55f else 0.7f
            isClickable = true
            isFocusable = true
            paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
            contentDescription = "Edit note for ${spaceObject.displayName}"

            setOnClickListener {
                showEditNoteDialog(favorite)
            }
        }

        val addedAtText = TextView(this).apply {
            text = DisplayDateFormatter.formatAddedOn(favorite.addedAt).orEmpty()
            setTextColor(getColor(R.color.color_primary))
            textSize = 12f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
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

    private fun showEditNoteDialog(favorite: FavoriteResponse) {
        val input = EditText(this).apply {
            setText(favorite.note.orEmpty())
            hint = "Add a note"
            inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
                    InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 3
            maxLines = 5
            setSingleLine(false)
            setSelection(text.length)
            setPadding(32, 18, 32, 18)
            setTextColor(getColor(R.color.color_text_on_background))
            setHintTextColor(getColor(R.color.color_text_muted))
            backgroundTintList = ColorStateList.valueOf(getColor(R.color.color_accent))
        }

        val dialogContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24.dp(), 22.dp(), 24.dp(), 8.dp())

            addView(TextView(this@FavoritesActivity).apply {
                text = "Favorite note"
                setTextColor(getColor(R.color.color_accent))
                textSize = 20f
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            })

            addView(TextView(this@FavoritesActivity).apply {
                text = favorite.spaceObject.displayName
                setTextColor(getColor(R.color.color_text_on_background))
                textSize = 14f
                alpha = 0.82f
                typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 6.dp()
                    bottomMargin = 12.dp()
                }
            })

            addView(input)
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogContent)
            .setPositiveButton("Save", null)
            .setNeutralButton("Clear", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
        styleNoteDialog(dialog)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val note = input.text.toString()
            if (note.length > MAX_NOTE_LENGTH) {
                input.error = "Keep note under $MAX_NOTE_LENGTH characters"
                return@setOnClickListener
            }

            updateFavoriteNote(favorite, note)
            dialog.dismiss()
        }

        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
            updateFavoriteNote(favorite, null)
            dialog.dismiss()
        }
    }

    private fun updateFavoriteNote(favorite: FavoriteResponse, note: String?) {
        lifecycleScope.launch {
            try {
                favoriteRepository.updateFavoriteNoteOnline(favorite, note)
                showFavorites(favoriteRepository.getCachedFavorites())
                showAppMessage("Favorite note saved.", MessageKind.SUCCESS)
            } catch (e: HttpException) {
                showAppMessage(
                    ApiErrorMapper.fromHttpException(e, "Could not save favorite note."),
                    MessageKind.ERROR
                )
            } catch (_: IOException) {
                favoriteRepository.updateFavoriteNoteCached(favorite, note)
                showFavorites(favoriteRepository.getCachedFavorites())
                showAppMessage("Note saved on this device.", MessageKind.INFO)
            } catch (e: Exception) {
                showAppMessage(
                    ApiErrorMapper.fromThrowable(e, "Could not save favorite note."),
                    MessageKind.ERROR
                )
            }
        }
    }

    private fun Int.dp(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    private fun formatFavoriteNote(note: String?): String {
        return note
            ?.takeIf { it.isNotBlank() }
            ?.let { "Note: $it" }
            ?: "Add note"
    }

    private fun styleNoteDialog(dialog: AlertDialog) {
        dialog.window?.setBackgroundDrawable(
            GradientDrawable().apply {
                setColor(getColor(R.color.color_primary))
                cornerRadius = 16.dp().toFloat()
                setStroke(1.dp(), getColor(R.color.color_accent))
            }
        )
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(getColor(R.color.color_accent))
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.setTextColor(getColor(R.color.color_accent))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(getColor(R.color.color_text_muted))
    }

    companion object {
        private const val MAX_NOTE_LENGTH = 500
    }
}
