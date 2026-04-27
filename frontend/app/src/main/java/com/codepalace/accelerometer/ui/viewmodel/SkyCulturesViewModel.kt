package com.codepalace.accelerometer.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.codepalace.accelerometer.api.ApiClient
import com.codepalace.accelerometer.data.local.AppDatabase
import com.codepalace.accelerometer.data.model.dto.ConstellationCultureResponse
import com.codepalace.accelerometer.data.repository.CelestialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SkyCulturesViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "SkyCultureVM"

    private val repository = CelestialRepository(
        api = ApiClient.celestialApi,
        database = AppDatabase.getDatabase(application)
    )

    private val _cultures = MutableStateFlow<List<ConstellationCultureResponse>>(emptyList())
    val cultures: StateFlow<List<ConstellationCultureResponse>> = _cultures

    private val _expandedId = MutableStateFlow<Long?>(null)
    val expandedId: StateFlow<Long?> = _expandedId

    fun loadCultures() {
        viewModelScope.launch {
            Log.d(TAG, "📡 Loading cultures...")
            try {
                val result = repository.getAllCultures()
                    .sortedBy { it.id }   // ← Stable order by id

                Log.d(TAG, "✅ Received ${result.size} cultures (sorted by id)")
                result.forEach {
                    Log.d(TAG, "➡️ id=${it.id}, name=${it.name}, current=${it.current}")
                }

                _cultures.value = result
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to load cultures", e)
            }
        }
    }

    fun toggleExpand(id: Long) {
        val newValue = if (_expandedId.value == id) null else id

        Log.d(TAG, "🔽 Toggle expand: clicked=$id -> $newValue")

        _expandedId.value = newValue
    }

    fun setCurrentCulture(id: Long) {
        viewModelScope.launch {
            Log.d(TAG, "🌍 Setting current culture: $id")
            try {
                repository.setCurrentCulture(id)
                Log.d(TAG, "✅ Culture set successfully, reloading...")
                loadCultures()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to set culture", e)
            }
        }
    }
}