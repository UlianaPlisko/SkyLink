package com.codepalace.accelerometer.ui.viewmodel

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.codepalace.accelerometer.api.ApiClient
import com.codepalace.accelerometer.data.local.AppDatabase
import com.codepalace.accelerometer.data.local.SpaceObjectEntity
import com.codepalace.accelerometer.data.model.Star
import com.codepalace.accelerometer.data.repository.CelestialRepository
import com.codepalace.accelerometer.util.CelestialConverter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.math.log10

@RequiresApi(Build.VERSION_CODES.O)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "MainViewModel"

    private val repository = CelestialRepository(
        api = ApiClient.celestialApi,
        database = AppDatabase.getDatabase(application)
    )

    private val _stars = MutableStateFlow<List<Star>>(emptyList())
    val stars: StateFlow<List<Star>> = _stars.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var currentLat = 51.5
    private var currentLon = -0.1
    private var displayMaxMagnitude = 3.0

    private var currentZoomLevel: Float = 1.0f          // 1.0 = default (90° FOV)
    private val baseFovHorizontal = 90f
    private val baseMaxMagnitude = 3.0

    private val _fovHorizontal = MutableStateFlow(baseFovHorizontal)
    val fovHorizontal: StateFlow<Float> = _fovHorizontal.asStateFlow()

    init {
        loadData()
        updateZoomDependentValues()   // ensures initial values are consistent
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadData() {
        viewModelScope.launch {
            Log.d(TAG, "🚀 Starting loadData()")

            _isLoading.value = true

            val cached = try {
                repository.getCachedObjects().first()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error reading cache", e)
                emptyList()
            }

            if (cached.isNotEmpty()) {
                Log.d(TAG, "✅ Using ${cached.size} cached objects (NO network call)")
                projectAndUpdateUI(cached)

                _isLoading.value = false
                return@launch   // 🔥 THIS LINE STOPS NETWORK CALL
            }

            Log.d(TAG, "🌐 Cache empty → fetching from backend")

            val success = repository.refreshAllObjects()

            if (success) {
                val fresh = repository.getCachedObjects().first()
                Log.d(TAG, "✅ Loaded ${fresh.size} objects from backend")
                projectAndUpdateUI(fresh)
            } else {
                Log.w(TAG, "❌ Backend failed and no cache available")
            }

            _isLoading.value = false
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun projectAndUpdateUI(objects: List<SpaceObjectEntity>) {
        Log.d(TAG, "🗺️ Projecting ${objects.size} objects to sky coordinates...")
        val referenceInstant = Instant.now()

        val projected = objects
            .asSequence()
            .filter { it.objectType == "STAR" && it.magnitude <= displayMaxMagnitude }
            .sortedBy { it.magnitude }
            .take(1000)
            .map { entity ->
                val (az, alt) = CelestialConverter.raDecToAzAlt(
                    entity.raDeg, entity.decDeg, currentLat, currentLon, referenceInstant
                )
                val v = CelestialConverter.azAltToENU(az, alt)
                Star(
                    spaceObjectId = entity.id,
                    name = entity.displayName,
                    raDegrees = entity.raDeg,
                    decDegrees = entity.decDeg,
                    magnitude = entity.magnitude,
                    azimuth = az,
                    altitude = alt,
                    east = v.x,
                    north = v.y,
                    up = v.z
                )
            }
            .toList()

        _stars.value = projected
        Log.d(TAG, "✅ Projected ${projected.size} visible stars (mag ≤ $displayMaxMagnitude)")
    }

    fun setMaxMagnitude(maxMag: Double) {
        displayMaxMagnitude = maxMag
        viewModelScope.launch {
            val cached = repository.getCachedObjects().first()
            projectAndUpdateUI(cached)
        }
    }

    fun updateObserverLocation(lat: Double, lon: Double) {
        currentLat = lat
        currentLon = lon

        viewModelScope.launch {
            val cached = repository.getCachedObjects().first()
            projectAndUpdateUI(cached)
        }
    }

    private fun updateZoomDependentValues() {
        val newFov = (baseFovHorizontal / currentZoomLevel).coerceIn(30f, 120f)
        _fovHorizontal.value = newFov

        val logValue = log10(currentZoomLevel.toDouble())
        val newMaxMag = (baseMaxMagnitude + 3.0 * logValue).coerceIn(2.0, 5.2)

        displayMaxMagnitude = newMaxMag
    }

    fun zoomBy(factor: Float) {
        currentZoomLevel *= factor
        currentZoomLevel = currentZoomLevel.coerceIn(0.7f, 5f)

        updateZoomDependentValues()

        viewModelScope.launch {
            val cached = repository.getCachedObjects().first()
            projectAndUpdateUI(cached)
        }
    }

    fun zoomIn() = zoomBy(1.25f)      // slightly faster zoom
    fun zoomOut() = zoomBy(1f / 1.25f)

    fun resetZoom() {
        currentZoomLevel = 1.0f
        updateZoomDependentValues()
        viewModelScope.launch {
            val cached = repository.getCachedObjects().first()
            projectAndUpdateUI(cached)
        }
    }
}