package com.codepalace.accelerometer.ui.viewmodel

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codepalace.accelerometer.api.ApiClient
import com.codepalace.accelerometer.api.websocket.ChatWebSocketManager
import com.codepalace.accelerometer.data.model.dto.ChatListItem
import com.codepalace.accelerometer.data.model.dto.ChatMessageResponse
import com.codepalace.accelerometer.data.model.dto.MessageUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private const val TAG = "ChatViewModel"

@RequiresApi(Build.VERSION_CODES.O)
class ChatViewModel(private val chatRoomId: Long) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatListItem>>(emptyList())
    val messages: StateFlow<List<ChatListItem>> = _messages.asStateFlow()

    private val webSocketManager = ChatWebSocketManager(
        sessionStorage = ApiClient.getSessionStorage(),
        onNewMessage = { response ->
            viewModelScope.launch {
                val currentUserId = ApiClient.getSessionStorage().getUserId()

                // 🔥 FIX: Skip our own messages (optimistic update already added them)
                if (response.userId == currentUserId) {
                    Log.d(TAG, "Ignored own NEW_MESSAGE (optimistic already shown)")
                    return@launch
                }

                val newUiMessage = response.toMessageUi(currentUserId)
                addMessageToList(newUiMessage)
            }
        }
    )

    init {
        Log.d(TAG, "✅ ViewModel created for roomId = $chatRoomId")
        loadMessages()
        webSocketManager.connectAndJoin(chatRoomId)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadMessages() {
        viewModelScope.launch {
            Log.d(TAG, "📡 loadMessages() started for room $chatRoomId")

            val currentUserId = ApiClient.getSessionStorage().getUserId()
            Log.d(TAG, "👤 Current user ID: $currentUserId")

            try {
                Log.d(TAG, "🔄 Calling getRoomMessages($chatRoomId)...")
                val responseList = ApiClient.chatApi.getRoomMessages(chatRoomId)

                Log.d(TAG, "✅ API SUCCESS - received ${responseList.size} messages")

                val uiMessages = responseList.map { it.toMessageUi(currentUserId) }
                val groupedItems = groupMessagesWithDateHeaders(uiMessages)

                _messages.value = groupedItems
                Log.d(TAG, "✅ Messages + date headers loaded successfully (${groupedItems.size} items)")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to load messages for room $chatRoomId", e)
            }
        }
    }

    private fun groupMessagesWithDateHeaders(messages: List<MessageUi>): List<ChatListItem> {
        if (messages.isEmpty()) return emptyList()

        val result = mutableListOf<ChatListItem>()
        var lastDateHeader = ""

        messages.forEach { message ->
            val currentHeader = getDateHeader(message)
            if (currentHeader != lastDateHeader) {
                result.add(ChatListItem.DateHeader(currentHeader))
                lastDateHeader = currentHeader
                Log.d(TAG, "📅 Added date header: $currentHeader")
            }
            result.add(ChatListItem.Message(message))
        }
        return result
    }

    /**
     * Real date header logic:
     * - "Today"
     * - "Yesterday"
     * - "13 Apr 2026" (or any other date)
     */
    private fun getDateHeader(message: MessageUi): String {
        return try {
            // Robust parser for full ISO format with millis + offset (Z or +0200)
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", Locale.getDefault())
            inputFormat.timeZone = java.util.TimeZone.getTimeZone("UTC") // important!

            val messageDate = inputFormat.parse(message.createdAt) ?: return "Unknown"

            val today = Calendar.getInstance()
            val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            val messageCal = Calendar.getInstance().apply { time = messageDate }

            when {
                isSameDay(messageCal, today) -> "Today"
                isSameDay(messageCal, yesterday) -> "Yesterday"
                else -> {
                    val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
                    dateFormat.format(messageDate)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Failed to create date header for: ${message.createdAt}", e)
            "Unknown"
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun addMessageToList(newMessage: MessageUi) {
        val currentList = _messages.value.toMutableList()
        val lastItem = currentList.lastOrNull()

        // Add date header if needed
        if (lastItem !is ChatListItem.DateHeader) {
            currentList.add(ChatListItem.DateHeader("Today"))
        }

        currentList.add(ChatListItem.Message(newMessage))
        _messages.value = currentList
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return

        // Optimistic update
        val time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        val optimisticMessage = MessageUi(
            id = System.currentTimeMillis(),
            content = content,
            time = time,
            isFromCurrentUser = true,
            senderDisplayName = "You",
            createdAt = ""
        )
        addMessageToList(optimisticMessage)

        // Real WebSocket send
        webSocketManager.sendMessage(chatRoomId, content)
    }

    override fun onCleared() {
        webSocketManager.disconnect()
        super.onCleared()
    }

    private fun ChatMessageResponse.toMessageUi(currentUserId: Long): MessageUi {
        val time = try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", Locale.getDefault())
            inputFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(createdAt) ?: java.util.Date()
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Failed to parse timestamp: $createdAt", e)
            "now"
        }

        return MessageUi(
            id = id,
            content = content,
            time = time,
            isFromCurrentUser = this.userId == currentUserId,
            senderDisplayName = senderEmail,
            createdAt = createdAt   // ← crucial for real date headers
        )
    }
}