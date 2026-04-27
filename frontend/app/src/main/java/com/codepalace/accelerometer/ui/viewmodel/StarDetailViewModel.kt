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

            val cached = repository.getCachedDetail(id)
            if (cached != null) {
                _detail.value = cached
                Log.d(TAG, "Loaded detail from cache for id=$id")
            }

            try {
                val result = repository.getSpaceObjectDetail(id)

                val wiki = repository.getSpaceObjectWiki(id)

                val finalDetail = if (wiki != null) {
                    result.copy(
                        wikiSummary = wiki.summary,
                        wikiUrl = wiki.url,
                        imageUrl = wiki.imageUrl
                    )
                } else {
                    result
                }

                _detail.value = finalDetail

                repository.saveDetail(finalDetail)

                Log.d(TAG, "Loaded detail from API and cached for id=$id")

            } catch (e: Exception) {
                Log.e(TAG, "Network failed for id=$id", e)

                if (cached == null) {
                    _error.value = "No internet and no cached data available"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
}