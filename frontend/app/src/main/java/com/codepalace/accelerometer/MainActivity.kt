package com.codepalace.accelerometer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ScaleGestureDetector
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.codepalace.accelerometer.sensors.OrientationHelper
import com.codepalace.accelerometer.ui.view.SkyView
import com.codepalace.accelerometer.ui.viewmodel.MainViewModel
import com.codepalace.accelerometer.util.CelestialConverter
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import com.codepalace.accelerometer.data.model.Star
import com.codepalace.accelerometer.sensors.CompassController
import com.codepalace.accelerometer.ui.view.HalfCompassView
import com.codepalace.accelerometer.ui.view.StarDetailActivity

class MainActivity : AppCompatActivity() {

    private lateinit var skyView: SkyView

    private lateinit var halfCompassView: HalfCompassView
    private lateinit var compassController: CompassController

    private lateinit var orientationHelper: OrientationHelper
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val viewModel: MainViewModel by viewModels()

    private var smoothedAzimuth: Float = 0f
    private var smoothedAltitude: Float = 0f

    private lateinit var tvTime: TextView
    private lateinit var tvDegrees: TextView
    private lateinit var btnMenu: ImageButton
    private lateinit var btnSearch: ImageButton
    private lateinit var btnChat: ImageButton
    private lateinit var btnCalendar: ImageButton

    private lateinit var starPreviewPanel: LinearLayout
    private lateinit var tvPreviewName: TextView
    private lateinit var tvPreviewType: TextView

    private var selectedStar: Star? = null

    private val timeHandler = Handler(Looper.getMainLooper())

    private val timeRunnable = object : Runnable {
        override fun run() {
            updateCurrentTime()
            timeHandler.postDelayed(this, 60_000)
        }
    }

    private lateinit var scaleGestureDetector: ScaleGestureDetector

    @RequiresApi(Build.VERSION_CODES.O)
    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                fetchLocation()
            }
        }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        skyView = findViewById(R.id.skyView)
        tvTime = findViewById(R.id.tvTime)
        tvDegrees = findViewById(R.id.tvDegrees)
        btnMenu = findViewById(R.id.btnMenu)
        btnSearch = findViewById(R.id.btnSearch)
        btnChat = findViewById(R.id.btnChat)
        btnCalendar = findViewById(R.id.btnCalendar)

        starPreviewPanel = findViewById(R.id.starPreviewPanel)
        tvPreviewName = findViewById(R.id.tvPreviewName)
        tvPreviewType = findViewById(R.id.tvPreviewType)

        updateCurrentTime()

        btnMenu.setOnClickListener {
            startActivity(Intent(this, MenuActivity::class.java))
        }

        btnSearch.setOnClickListener {
            // search later
        }

        btnChat.setOnClickListener {
            // chat later
        }

        btnCalendar.setOnClickListener {
            // calendar later
        }

        val loadingOverlay = findViewById<View>(R.id.loadingOverlay)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        updateCurrentTime()

        skyView.onZoom = { scaleFactor ->
            viewModel.zoomBy(scaleFactor)
        }

        skyView.onStarClick = { star ->
            selectedStar = star
            tvPreviewName.text = star.name
            tvPreviewType.text = "Tap to view details"
            starPreviewPanel.visibility = View.VISIBLE
        }

        starPreviewPanel.setOnClickListener {
            val star = selectedStar ?: return@setOnClickListener

            val intent = Intent(this, StarDetailActivity::class.java)
            intent.putExtra("star_id", star.spaceObjectId)
            intent.putExtra("star_name", star.name)

            startActivity(intent)
            overridePendingTransition(R.anim.slide_up_in, R.anim.stay)
        }

        halfCompassView = findViewById(R.id.halfCompassView)

        compassController = CompassController(this).apply {
            useTrueNorth = true
            smoothingAlpha = 0.12f

            onHeadingChanged = { heading ->
                smoothedAzimuth = heading

                runOnUiThread {
                    tvDegrees.text = "${heading.toInt()}°"
                    halfCompassView.headingDeg = heading
                    skyView.phoneAzimuth = heading
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.stars.collect { stars ->
                        skyView.stars = stars
                    }
                }

                launch {
                    viewModel.fovHorizontal.collect { newFov ->
                        skyView.fovHorizontal = newFov
                    }
                }

                launch {
                    viewModel.isLoading.collect { isLoading ->
                        loadingOverlay.isVisible = isLoading
                    }
                }
            }
        }

        orientationHelper = OrientationHelper(this)
        orientationHelper.onOrientationChanged = { _, pitch, _ ->
            smoothedAltitude = 0.85f * smoothedAltitude + 0.15f * (-pitch)
        }

        orientationHelper.onMatrixChanged = { matrix ->
            skyView.rotationMatrix = matrix
        }

        requestLocationIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        orientationHelper.start()
        compassController.start()

        updateCurrentTime()
        timeHandler.post(timeRunnable)
    }

    override fun onPause() {
        super.onPause()
        orientationHelper.stop()
        compassController.stop()

        timeHandler.removeCallbacks(timeRunnable)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun requestLocationIfNeeded() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                fetchLocation()
            }

            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }

            else -> {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingPermission")
    private fun fetchLocation() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    viewModel.updateObserverLocation(location.latitude, location.longitude)
                    compassController.updateLocation(location)
                } else {
                    requestFreshLocation()
                }
            }
            .addOnFailureListener {
                requestFreshLocation()
            }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingPermission")
    private fun requestFreshLocation() {
        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()

        fusedLocationClient
            .getCurrentLocation(request, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    viewModel.updateObserverLocation(location.latitude, location.longitude)
                    compassController.updateLocation(location)
                }
            }
    }

    private fun updateCurrentTime() {
        val formatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        tvTime.text = formatter.format(java.util.Date())
    }
}