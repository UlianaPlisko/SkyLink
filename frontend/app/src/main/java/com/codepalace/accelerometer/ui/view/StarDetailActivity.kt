package com.codepalace.accelerometer.ui.view
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.codepalace.accelerometer.R
import com.codepalace.accelerometer.ui.viewmodel.StarDetailViewModel
import kotlinx.coroutines.launch

class StarDetailActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var tvConstellation: TextView
    private lateinit var tvMagnitude: TextView
    private lateinit var tvDistance: TextView
    private lateinit var tvRaDec: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvAlsoKnown: TextView

    private val viewModel: StarDetailViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_star_detail)

        val root = findViewById<View>(R.id.starDetailRoot)

        btnBack = findViewById(R.id.btnBack)
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

        val starId = intent.getLongExtra("star_id", -1L)
        val starName = intent.getStringExtra("star_name").orEmpty()

        tvTitle.text = starName
        tvSubtitle.text = "Loading..."
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
                            tvMagnitude.text = "Magnitude: ${detail.magnitude ?: "-"}"
                            tvRaDec.text = "RA: ${detail.raDeg ?: "-"}   Dec: ${detail.decDeg ?: "-"}"
                            tvDescription.text = "No description available."

                            // placeholders until backend has more fields
                            tvConstellation.text = ""
                            tvDistance.text = ""
                            tvAlsoKnown.text = ""
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
        } else {
            tvSubtitle.text = "Invalid star ID"
            tvDescription.text = "No star detail could be loaded."
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.stay, R.anim.slide_down_out)
    }
}