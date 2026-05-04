package com.codepalace.accelerometer.ui.viewmodel

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codepalace.accelerometer.api.ApiClient
import com.codepalace.accelerometer.data.local.ChatRoomDao
import com.codepalace.accelerometer.data.local.ChatRoomEntity
import com.codepalace.accelerometer.data.local.PendingChatActionEntity
import com.codepalace.accelerometer.data.local.SessionStorage
import com.codepalace.accelerometer.data.model.dto.ChatRoomResponse
import com.codepalace.accelerometer.data.model.dto.ChatRoomUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatRoomsViewModel(
    private val chatRoomDao: ChatRoomDao,
    private val sessionStorage: SessionStorage
) : ViewModel() {

    private val _chatRooms = MutableStateFlow<List<ChatRoomUi>>(emptyList())
    val chatRooms: StateFlow<List<ChatRoomUi>> = _chatRooms.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val currentUserId: Long get() = sessionStorage.getUserId()

    // -------------------------------------------------------------------------
    // Initial load
    // -------------------------------------------------------------------------

    @RequiresApi(Build.VERSION_CODES.O)
    fun loadInitialData() = viewModelScope.launch {
        val userId = currentUserId
        if (userId <= 0) {
            _chatRooms.value = emptyList()
            return@launch
        }

        _isLoading.value = true
        Log.d(TAG, "Loading for user $userId")

        val cached = chatRoomDao.getSubscribedRooms(userId)
        if (cached.isNotEmpty()) {
            _chatRooms.value = cached.map { it.toUi() }
            Log.d(TAG, "Showing ${cached.size} cached rooms while fetching server data")
        }

        try {
            val serverRooms = ApiClient.chatApi.getMySubscribedRooms()
            val entities = serverRooms.map { it.toEntity(userId, true) }

            chatRoomDao.deleteAllForUser(userId)
            chatRoomDao.insertAll(entities)
            _chatRooms.value = entities.map { it.toUi() }
            Log.d(TAG, "Loaded ${entities.size} rooms from server")

            // Only process pending actions when we know the server is reachable.
            processPendingActions(userId)

        } catch (e: Exception) {
            Log.e(TAG, "Server unavailable — showing cached data", e)
        } finally {
            _isLoading.value = false
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun processPendingActions(userId: Long) {
        val pending = chatRoomDao.getAllPendingActions()
        if (pending.isEmpty()) return

        Log.d(TAG, "Processing ${pending.size} pending actions")
        var anySucceeded = false

        pending.forEach { action ->
            try {
                if (action.action == "SUBSCRIBE") {
                    ApiClient.chatApi.subscribeCurrentUser(action.roomId)
                } else {
                    ApiClient.chatApi.unsubscribeCurrentUser(action.roomId)
                }
                chatRoomDao.deletePendingAction(action.id)
                anySucceeded = true
            } catch (e: Exception) {
                val isPermanentFailure = e is retrofit2.HttpException &&
                        e.code() in listOf(403, 404, 410)

                if (isPermanentFailure) {
                    // Room gone or no permission — discard, will never succeed
                    Log.w(TAG, "Discarding pending action ${action.id} (${e.message})")
                    chatRoomDao.deletePendingAction(action.id)
                } else {
                    // Network error, 5xx, timeout — keep for retry
                    Log.w(TAG, "Pending action ${action.id} failed — will retry later", e)
                }
            }
        }

         if (anySucceeded) {
            try {
                val refreshed = ApiClient.chatApi.getMySubscribedRooms()
                val entities = refreshed.map { it.toEntity(userId, true) }
                chatRoomDao.deleteAllForUser(userId)
                chatRoomDao.insertAll(entities)
                _chatRooms.value = entities.map { it.toUi() }
                Log.d(TAG, "Refreshed ${entities.size} rooms after pending sync")
            } catch (e: Exception) {
                Log.w(TAG, "Post-sync refresh failed — showing local state", e)
                refreshLocal()
            }
        }
    }

    // -------------------------------------------------------------------------
    // Search / filter
    // -------------------------------------------------------------------------

    @RequiresApi(Build.VERSION_CODES.O)
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        applyFilter()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun applyFilter() = viewModelScope.launch {
        val query = _searchQuery.value.trim().lowercase()

        if (query.isEmpty()) {
            // Empty query → show subscribed rooms from local DB (works offline).
            _chatRooms.value = chatRoomDao.getSubscribedRooms(currentUserId).map { it.toUi() }
            return@launch
        }

        _isLoading.value = true
        try {
            // Online: fetch all rooms so we can search beyond subscribed ones.
            val allRooms = ApiClient.chatApi.getAllRooms()
            val entities = allRooms.map { it.toEntity(currentUserId, false) }
            chatRoomDao.insertAll(entities) // cache them; don't wipe subscribed rows

            _chatRooms.value = entities
                .map { it.toUi() }
                .filter { it.name.lowercase().contains(query) }

        } catch (e: Exception) {
            // FIX 4: Offline search — fall back to searching what we have locally.
            Log.w(TAG, "Search unavailable offline — filtering local cache", e)
            val localRooms = chatRoomDao.getAllRoomsForUser(currentUserId)
            _chatRooms.value = localRooms
                .map { it.toUi() }
                .filter { it.name.lowercase().contains(query) }
        } finally {
            _isLoading.value = false
        }
    }

    // -------------------------------------------------------------------------
    // Subscription toggle with optimistic update + offline queue
    // -------------------------------------------------------------------------

    @RequiresApi(Build.VERSION_CODES.O)
    fun toggleSubscription(roomId: Long) = viewModelScope.launch {
        val userId = currentUserId
        if (userId <= 0) return@launch

        val currentRoom = _chatRooms.value.find { it.id == roomId } ?: return@launch
        val newStatus = !currentRoom.isSubscribed

        // Optimistic local update — instant UI feedback.
        chatRoomDao.updateSubscription(roomId, userId, newStatus)
        refreshLocal()

        try {
            if (newStatus) {
                ApiClient.chatApi.subscribeCurrentUser(roomId)
            } else {
                ApiClient.chatApi.unsubscribeCurrentUser(roomId)
            }
            Log.d(TAG, "Subscription toggled on server: room $roomId → subscribed=$newStatus")
        } catch (e: Exception) {
            // FIX 5: Queue the action; the optimistic local state remains correct.
            val actionType = if (newStatus) "SUBSCRIBE" else "UNSUBSCRIBE"
            chatRoomDao.insertPendingAction(
                PendingChatActionEntity(roomId = roomId, action = actionType)
            )
            Log.w(TAG, "API failed → queued $actionType for room $roomId (will sync when online)", e)
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Refresh the UI list from local DB only — no network call. */
    private fun refreshLocal() = viewModelScope.launch {
        _chatRooms.value = chatRoomDao.getSubscribedRooms(currentUserId).map { it.toUi() }
    }

    private fun ChatRoomResponse.toEntity(userId: Long, isSubscribed: Boolean): ChatRoomEntity =
        ChatRoomEntity(
            id = id,
            userId = userId,
            name = name,
            type = type,
            regionGeom = regionGeom,
            eventId = eventId,
            createdBy = createdBy,
            createdAt = createdAt,
            isSubscribed = isSubscribed
        )

    private fun ChatRoomEntity.toUi(): ChatRoomUi =
        ChatRoomUi(id = id, name = name, isSubscribed = isSubscribed, unreadCount = unreadCount)

    companion object {
        private const val TAG = "ChatRoomsVM"
    }

    fun refresh() = viewModelScope.launch {
        _chatRooms.value = chatRoomDao.getSubscribedRooms(currentUserId).map { it.toUi() }
    }
}