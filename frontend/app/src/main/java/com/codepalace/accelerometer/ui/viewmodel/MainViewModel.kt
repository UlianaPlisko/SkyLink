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

    init {
        loadData()
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
                Log.d(TAG, "✅ Found ${cached.size} cached objects → projecting bright stars")
                projectAndUpdateUI(cached)
            } else {
                Log.d(TAG, "⚠️ Cache is empty on first launch")
            }

            val success = repository.refreshAllObjects()

            if (success) {
                Log.d(TAG, "✅ Backend refresh successful → reloading fresh data")
                val fresh = repository.getCachedObjects().first()
                projectAndUpdateUI(fresh)
            } else {
                Log.w(TAG, "⚠️ No internet or backend error → keeping cache (if any)")
            }

            _isLoading.value = false
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun projectAndUpdateUI(objects: List<SpaceObjectEntity>) {
        Log.d(TAG, "🗺️ Projecting ${objects.size} objects to sky coordinates...")
        val referenceInstant = Instant.now()

        val projected = objects
            .filter { it.objectType == "STAR" && it.magnitude <= displayMaxMagnitude }
            .map { entity ->
                val (az, alt) = CelestialConverter.raDecToAzAlt(
                    entity.raDeg, entity.decDeg, currentLat, currentLon, referenceInstant
                )
                val v = CelestialConverter.azAltToENU(az, alt)
                Star(
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
}