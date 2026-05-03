package com.codepalace.accelerometer.ui.activity

import com.codepalace.accelerometer.R
import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
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
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codepalace.accelerometer.api.ApiClient
import com.codepalace.accelerometer.data.local.AppSettingsStorage
import com.codepalace.accelerometer.data.model.Star
import com.codepalace.accelerometer.sensors.CompassController
import com.codepalace.accelerometer.ui.view.HalfCompassView
import com.codepalace.accelerometer.ui.MessageKind
import com.codepalace.accelerometer.ui.showAppMessage
import com.codepalace.accelerometer.util.SearchResultAdapter

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    private lateinit var skyView: SkyView

    private lateinit var halfCompassView: HalfCompassView
    private lateinit var compassController: CompassController

    private lateinit var orientationHelper: OrientationHelper
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var settingsStorage: AppSettingsStorage

    private val viewModel: MainViewModel by viewModels()

    private var smoothedAzimuth: Float = 0f
    private var smoothedAltitude: Float = 0f
    private var lastSensorsEnabled: Boolean? = null

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

    private lateinit var searchOverlay: androidx.constraintlayout.widget.ConstraintLayout
    private lateinit var searchInput: EditText
    private lateinit var btnSearchClose: android.widget.ImageButton
    private lateinit var btnSearchClear: android.widget.ImageButton
    private lateinit var rvSearchResults: RecyclerView
    private lateinit var tvSearchEmpty: android.widget.TextView
    private lateinit var searchAdapter: SearchResultAdapter

    private lateinit var scaleGestureDetector: ScaleGestureDetector

    @RequiresApi(Build.VERSION_CODES.O)
    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                fetchLocation()
            } else {
                showAppMessage(
                    "Allow location access or enter coordinates manually in settings.",
                    MessageKind.INFO
                )
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
        starPreviewPanel.setOnClickListener {
            openSelectedStarDetails()
        }
        tvPreviewName.setOnClickListener {
            openSelectedStarDetails()
        }
        tvPreviewType.setOnClickListener {
            openSelectedStarDetails()
        }

        searchOverlay    = findViewById(R.id.searchOverlay)
        searchInput      = findViewById(R.id.searchInput)
        btnSearchClose   = findViewById(R.id.btnSearchClose)
        btnSearchClear   = findViewById(R.id.btnSearchClear)
        rvSearchResults  = findViewById(R.id.rvSearchResults)
        tvSearchEmpty    = findViewById(R.id.tvSearchEmpty)

        // Adapter
        searchAdapter = SearchResultAdapter { result ->
            viewModel.closeSearch()

            val intent = Intent(this, StarDetailActivity::class.java).apply {
                putExtra("star_id", result.id)
                putExtra("star_name", result.displayName)
            }
            startActivity(intent)
        }

        rvSearchResults.layoutManager = LinearLayoutManager(this)
        rvSearchResults.adapter = searchAdapter

        updateCurrentTime()

        btnMenu.setOnClickListener {
            startActivity(Intent(this, MenuActivity::class.java))
        }

        // Open search
        btnSearch.setOnClickListener {
            viewModel.openSearch()
        }

        // Close search
        btnSearchClose.setOnClickListener {
            viewModel.closeSearch()
        }

        // Clear input
        btnSearchClear.setOnClickListener {
            searchInput.text.clear()
        }

        // Text changes → ViewModel
        searchInput.doAfterTextChanged { editable ->
            val text = editable?.toString() ?: ""
            btnSearchClear.isVisible = text.isNotEmpty()
            viewModel.onSearchQueryChanged(text)
        }

        // Dismiss overlay on back press
        onBackPressedDispatcher.addCallback(this,
            object : androidx.activity.OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (viewModel.isSearchVisible.value) {
                        viewModel.closeSearch()
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )

        btnChat.setOnClickListener {
            if (!ApiClient.getSessionStorage().isLoggedIn()) {
                showAppMessage("Log in to view calendar.", MessageKind.INFO)
                startActivity(Intent(this, AuthActivity::class.java))
            } else {
            startActivity(Intent(this, ChatRoomsActivity::class.java))}
        }

        btnCalendar.setOnClickListener {
            if (!ApiClient.getSessionStorage().isLoggedIn()) {
                showAppMessage("Log in to view calendar.", MessageKind.INFO)
                startActivity(Intent(this, AuthActivity::class.java))
            } else {
                startActivity(Intent(this, CalendarActivity::class.java))
            }
        }

        val loadingOverlay = findViewById<View>(R.id.loadingOverlay)
        loadingOverlay.bringToFront()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        settingsStorage = AppSettingsStorage(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, 0)

            findViewById<View>(R.id.topBar).updateMargins(
                left = systemBars.left + 12.dp(),
                top = systemBars.top + 10.dp(),
                right = systemBars.right + 12.dp()
            )
            btnChat.updateMargins(
                left = systemBars.left + 16.dp(),
                bottom = systemBars.bottom + 20.dp()
            )
            btnCalendar.updateMargins(
                right = systemBars.right + 16.dp(),
                bottom = systemBars.bottom + 20.dp()
            )
            findViewById<View>(R.id.compassContainer).updateMargins(
                bottom = systemBars.bottom
            )
            starPreviewPanel.updateMargins(
                left = systemBars.left + 12.dp(),
                right = systemBars.right + 12.dp(),
                bottom = systemBars.bottom + 90.dp()
            )
            findViewById<View>(R.id.searchBarRow).updateMargins(
                left = systemBars.left + 12.dp(),
                top = systemBars.top + 10.dp(),
                right = systemBars.right + 12.dp()
            )

            insets
        }

        updateCurrentTime()

        skyView.onZoom = { scaleFactor ->
            viewModel.zoomBy(scaleFactor)
        }

        skyView.onManualViewChanged = { azimuth, _ ->
            tvDegrees.text = "${azimuth.toInt()}°"
            halfCompassView.headingDeg = azimuth
        }

        skyView.onStarClick = { star ->
            selectedStar = star
            tvPreviewName.text = star.name
            tvPreviewType.text = "Tap to view details"
            if (!starPreviewPanel.isVisible) {
                starPreviewPanel.visibility = View.VISIBLE
                starPreviewPanel.bringToFront()
                starPreviewPanel.startAnimation(
                    android.view.animation.AnimationUtils.loadAnimation(
                        this,
                        R.anim.slide_up_soft
                    )
                )
            }
        }

        skyView.onEmptySpaceClick = {
            if (starPreviewPanel.isVisible) {
                val anim = android.view.animation.AnimationUtils.loadAnimation(
                    this,
                    R.anim.slide_down_soft
                )

                anim.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                    override fun onAnimationStart(animation: android.view.animation.Animation?) {}

                    override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                        starPreviewPanel.visibility = View.GONE
                        selectedStar = null
                    }

                    override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
                })

                starPreviewPanel.startAnimation(anim)
            }
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
                launch { viewModel.stars.collect         { stars   -> skyView.stars          = stars  } }
                launch { viewModel.fovHorizontal.collect { fov     -> skyView.fovHorizontal  = fov    } }
                launch { viewModel.isLoading.collect     { loading -> setLoadingVisible(loadingOverlay, loading) } }

                // Search overlay visibility + keyboard
                launch {
                    viewModel.isSearchVisible.collect { visible ->
                        searchOverlay.isVisible = visible
                        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                        if (visible) {
                            searchInput.requestFocus()
                            imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT)
                        } else {
                            searchInput.clearFocus()
                            imm.hideSoftInputFromWindow(searchInput.windowToken, 0)
                        }
                    }
                }

                // Search results
                launch {
                    viewModel.searchResults.collect { results ->
                        searchAdapter.submitList(results)
                        val hasQuery  = viewModel.searchQuery.value.isNotBlank()
                        rvSearchResults.isVisible = results.isNotEmpty()
                        tvSearchEmpty.isVisible   = results.isEmpty() && hasQuery
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

    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        applyRuntimeSettings()
        requestLocationIfNeeded()

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
        if (!settingsStorage.autoLocationEnabled) {
            val latitude = settingsStorage.manualLatitude()
            val longitude = settingsStorage.manualLongitude()

            if (latitude != null && longitude != null) {
                viewModel.updateObserverLocation(latitude, longitude)
                compassController.updateLocation(
                    Location("manual").apply {
                        this.latitude = latitude
                        this.longitude = longitude
                    }
                )
                return
            }

            showAppMessage(
                "Enter manual latitude and longitude in location settings.",
                MessageKind.INFO
            )
            return
        }

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

    private fun applyRuntimeSettings() {
        skyView.redModeEnabled = settingsStorage.redModeEnabled

        val sensorsEnabled = settingsStorage.automaticSensors
        skyView.manualControlEnabled = !sensorsEnabled

        if (sensorsEnabled) {
            orientationHelper.start()
            compassController.start()
        } else {
            orientationHelper.stop()
            compassController.stop()

            if (lastSensorsEnabled != false) {
                skyView.setManualLook(smoothedAzimuth, skyView.manualAltitude)
            }
        }

        lastSensorsEnabled = sensorsEnabled
    }

    private fun setLoadingVisible(loadingOverlay: View, visible: Boolean) {
        loadingOverlay.animate().cancel()

        if (visible) {
            loadingOverlay.alpha = 0f
            loadingOverlay.isVisible = true
            loadingOverlay.animate()
                .alpha(1f)
                .setDuration(220L)
                .start()
        } else {
            loadingOverlay.animate()
                .alpha(0f)
                .setDuration(180L)
                .withEndAction {
                    loadingOverlay.isVisible = false
                }
                .start()
        }
    }

    private fun openSelectedStarDetails() {
        val star = selectedStar ?: return

        if (star.spaceObjectId <= 0L) {
            showAppMessage("Could not open details for this object.", MessageKind.ERROR)
            return
        }

        val intent = Intent(this, StarDetailActivity::class.java).apply {
            putExtra("star_id", star.spaceObjectId)
            putExtra("star_name", star.name)
        }

        startActivity(intent)
        overridePendingTransition(R.anim.slide_up_in, R.anim.stay)
    }

    private fun View.updateMargins(
        left: Int? = null,
        top: Int? = null,
        right: Int? = null,
        bottom: Int? = null
    ) {
        val params = layoutParams as? ViewGroup.MarginLayoutParams ?: return
        left?.let { params.leftMargin = it }
        top?.let { params.topMargin = it }
        right?.let { params.rightMargin = it }
        bottom?.let { params.bottomMargin = it }
        layoutParams = params
    }

    private fun Int.dp(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}
