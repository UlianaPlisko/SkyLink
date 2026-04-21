package com.codepalace.accelerometer.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.codepalace.accelerometer.api.ApiClient
import com.codepalace.accelerometer.data.local.AppDatabase
import com.codepalace.accelerometer.data.model.SpaceObjectDetail
import com.codepalace.accelerometer.data.repository.CelestialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StarDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "StarDetailViewModel"

    private val repository = CelestialRepository(
        api = ApiClient.celestialApi,
        database = AppDatabase.getDatabase(application)
    )

    private val _detail = MutableStateFlow<SpaceObjectDetail?>(null)
    val detail: StateFlow<SpaceObjectDetail?> = _detail.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadingMessage = MutableStateFlow<String?>(null)
    val loadingMessage: StateFlow<String?> = _loadingMessage.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadDetail(id: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val result = repository.getSpaceObjectDetail(id)
                _detail.value = result
                Log.d(TAG, "Loaded base detail for id=$id: ${result.displayName}")

                val wiki = repository.getSpaceObjectWiki(id)
                if (wiki != null) {
                    _detail.value = result.copy(
                        wikiSummary = wiki.summary,
                        wikiUrl = wiki.url,
                        imageUrl = wiki.imageUrl
                    )

                    Log.d(TAG, "Updated detail imageUrl for id=$id: ${_detail.value?.imageUrl}")
                    Log.d(TAG, "Loaded wiki info for id=$id")
                } else {
                    Log.d(TAG, "No wiki info available for id=$id")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to load detail for id=$id", e)
                _error.value = "Failed to load details"
            } finally {
                _isLoading.value = false
            }
        }
    }
}