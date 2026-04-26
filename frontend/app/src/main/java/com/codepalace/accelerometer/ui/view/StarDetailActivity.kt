package com.codepalace.accelerometer.ui.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.codepalace.accelerometer.AuthActivity
import com.codepalace.accelerometer.R
import com.codepalace.accelerometer.api.ApiClient
import com.codepalace.accelerometer.api.ApiErrorMapper
import com.codepalace.accelerometer.api.dto.FavoriteRequest
import com.codepalace.accelerometer.config.ApiConfig
import com.codepalace.accelerometer.ui.MessageKind
import com.codepalace.accelerometer.ui.showAppMessage
import com.codepalace.accelerometer.ui.viewmodel.StarDetailViewModel
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class StarDetailActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var btnFavorite: ImageButton
    private lateinit var ivStarImage: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var tvConstellation: TextView
    private lateinit var tvMagnitude: TextView
    private lateinit var tvDistance: TextView
    private lateinit var tvRaDec: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvAlsoKnown: TextView
    private var starId: Long = -1L
    private var isFavorite = false
    private var favoriteBusy = false

    private val viewModel: StarDetailViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiClient.init(this)
        enableEdgeToEdge()
        setContentView(R.layout.activity_star_detail)

        val root = findViewById<View>(R.id.starDetailRoot)

        btnBack = findViewById(R.id.btnBack)
        btnFavorite = findViewById(R.id.btnFavorite)
        ivStarImage = findViewById(R.id.ivStarImage)
        tvTitle = findViewById(R.id.tvTitle)
        tvSubtitle = findViewById(R.id.tvSubtitle)
        tvConstellation = findViewById(R.id.tvConstellation)
        tvMagnitude = findViewById(R.id.tvMagnitude)
        tvDistance = findViewById(R.id.tvDistance)
        tvRaDec = findViewById(R.id.tvRaDec)
        tvDescription = findViewById(R.id.tvDescription)
        tvAlsoKnown = findViewById(R.id.tvAlsoKnown)

        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                view.paddingLeft,
                systemBars.top,
                view.paddingRight,
                systemBars.bottom
            )
            insets
        }

        btnBack.setOnClickListener {
            finish()
        }

        btnFavorite.setOnClickListener {
            toggleFavorite()
        }

        starId = intent.getLongExtra("star_id", -1L)
        val starName = intent.getStringExtra("star_name").orEmpty()

        tvTitle.text = starName
        tvSubtitle.text = "Loading info..."
        tvConstellation.text = ""
        tvMagnitude.text = ""
        tvDistance.text = ""
        tvRaDec.text = ""
        tvDescription.text = "Loading star description..."
        tvAlsoKnown.text = ""

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.detail.collect { detail ->
                        if (detail != null) {
                            tvTitle.text = detail.displayName

                            tvSubtitle.text = detail.spectralType
                                ?: detail.objectClass
                                        ?: "Space object"

                            tvConstellation.text = detail.constellation?.let { "Constellation: $it" } ?: ""
                            tvMagnitude.text = "Magnitude: ${detail.magnitude ?: "-"}"
                            tvDistance.text = detail.distanceLy?.let { "Distance: $it ly" } ?: ""
                            tvRaDec.text = "RA: ${detail.raDeg}   Dec: ${detail.decDeg}"

                            val baseDescription = detail.description?.takeIf { it.isNotBlank() }
                            val wikiSummary = detail.wikiSummary?.takeIf { it.isNotBlank() }

                            tvDescription.text = when {
                                baseDescription != null && wikiSummary != null ->
                                    "$baseDescription\n\nWikipedia:\n$wikiSummary"
                                baseDescription != null ->
                                    baseDescription
                                wikiSummary != null ->
                                    wikiSummary
                                else ->
                                    "No description available."
                            }

                            val extraInfo = buildString {
                                detail.spectralType?.let { append("Spectral type: $it\n") }
                                detail.orbitalModel?.let { append("Orbital model: $it\n") }
                                detail.catalogId?.let { append("Catalog ID: $it\n") }
                                detail.objectClass?.let { append("Object class: $it\n") }
                                detail.angularSize?.let { append("Angular size: $it\n") }
                                detail.wikiUrl?.let { append("Wikipedia: $it\n") }
                            }.trim()

                            tvAlsoKnown.text = when {
                                extraInfo.isNotBlank() -> extraInfo
                                detail.wikiSummary.isNullOrBlank() && detail.wikiUrl.isNullOrBlank() ->
                                    "No Wikipedia info available."
                                else -> ""
                            }

                            android.util.Log.d("StarDetailActivity", "detail.imageUrl = ${detail.imageUrl}")

                            val imageUrl = resolveUrl(detail.imageUrl)

                            if (!imageUrl.isNullOrBlank()) {
                                ivStarImage.load(imageUrl) {
                                    crossfade(true)
                                    placeholder(R.drawable.space_object_placeholder)
                                    error(R.drawable.space_object_placeholder)
                                }
                            } else {
                                ivStarImage.setImageResource(R.drawable.space_object_placeholder)
                            }
                        }
                    }
                }

                launch {
                    viewModel.error.collect { error ->
                        if (error != null) {
                            tvSubtitle.text = error
                            tvDescription.text = "Could not load data from backend."
                        }
                    }
                }
            }
        }

        if (starId != -1L) {
            viewModel.loadDetail(starId)
            loadFavoriteState()
        } else {
            tvSubtitle.text = "Invalid star ID"
            tvDescription.text = "No star detail could be loaded."
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.stay, R.anim.slide_down_out)
    }

    fun resolveUrl(path: String?): String? {
        if (path == null) return null
        return if (path.startsWith("http")) path
        else "${ApiConfig.BASE_URL}$path"
    }

    private fun loadFavoriteState() {
        if (!ApiClient.getSessionStorage().isLoggedIn()) {
            setFavoriteState(false)
            return
        }

        lifecycleScope.launch {
            try {
                val favorites = ApiClient.favoriteApi.getFavorites()
                setFavoriteState(favorites.any { it.spaceObject.id == starId })
            } catch (_: Exception) {
                setFavoriteState(false)
            }
        }
    }

    private fun toggleFavorite() {
        if (starId == -1L || favoriteBusy) return

        if (!ApiClient.getSessionStorage().isLoggedIn()) {
            showAppMessage("Log in to save favorites.", MessageKind.INFO)
            startActivity(Intent(this, AuthActivity::class.java))
            return
        }

        lifecycleScope.launch {
            favoriteBusy = true
            btnFavorite.isEnabled = false

            try {
                if (isFavorite) {
                    val response = ApiClient.favoriteApi.removeFavorite(starId)
                    if (!response.isSuccessful && response.code() != 404) {
                        throw HttpException(response)
                    }

                    setFavoriteState(false)
                    showAppMessage("Removed from favorites.", MessageKind.SUCCESS)
                } else {
                    ApiClient.favoriteApi.addFavorite(FavoriteRequest(spaceObjectId = starId))
                    setFavoriteState(true)
                    showAppMessage("Added to favorites.", MessageKind.SUCCESS)
                }
            } catch (e: HttpException) {
                showAppMessage(
                    ApiErrorMapper.fromHttpException(e, "Could not update favorites."),
                    MessageKind.ERROR
                )
            } catch (e: IOException) {
                showAppMessage(ApiErrorMapper.fromIOException(e), MessageKind.ERROR)
            } finally {
                favoriteBusy = false
                btnFavorite.isEnabled = true
            }
        }
    }

    private fun setFavoriteState(value: Boolean) {
        isFavorite = value
        btnFavorite.setImageResource(
            if (value) R.drawable.ic_star_filled else R.drawable.ic_star_outline
        )
        btnFavorite.contentDescription = if (value) {
            "Remove from favorites"
        } else {
            "Add to favorites"
        }
    }
}
