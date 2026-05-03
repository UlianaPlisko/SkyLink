package com.codepalace.accelerometer.api.websocket

import android.util.Log
import com.codepalace.accelerometer.config.ApiConfig
import com.codepalace.accelerometer.data.model.dto.ChatMessageResponse
import com.codepalace.accelerometer.data.local.SessionStorage
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.TimeUnit

private const val TAG = "ChatWebSocket"

class ChatWebSocketManager(
    private val sessionStorage: SessionStorage,
    private val gson: Gson = Gson(),
    private val onNewMessage: (ChatMessageResponse) -> Unit,
    private val onError: (String) -> Unit = {}
) {

    private var webSocket: WebSocket? = null
    private var isAuthenticated = false
    private var currentRoomId: Long? = null

    fun connectAndJoin(roomId: Long) {
        currentRoomId = roomId
        if (webSocket != null) {
            joinRoom(roomId)
            return
        }

        val token = sessionStorage.getToken() ?: run {
            onError("No auth token found")
            return
        }

        val baseUrl = ApiConfig.BASE_URL
        val wsUrl = if (baseUrl.startsWith("https")) {
            baseUrl.replaceFirst("https", "wss") + "/ws-chat"
        } else {
            baseUrl.replaceFirst("http", "ws") + "/ws-chat"
        }

        Log.d(TAG, "Connecting to: $wsUrl")

        val request = Request.Builder().url(wsUrl).build()
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected")
                sendAuth(token)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure", t)
                isAuthenticated = false
                onError("Connection failed: ${t.message}")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code - $reason")
                isAuthenticated = false
            }
        })
    }

    private fun sendAuth(token: String) {
        val msg = mapOf("type" to "AUTH", "token" to token)
        webSocket?.send(gson.toJson(msg))
        Log.d(TAG, "Sent AUTH")
    }

    private fun joinRoom(roomId: Long) {
        if (!isAuthenticated) {
            Log.w(TAG, "Cannot join room yet - not authenticated")
            return
        }
        currentRoomId = roomId
        val msg = mapOf("type" to "JOIN_ROOM", "roomId" to roomId)
        webSocket?.send(gson.toJson(msg))
        Log.d(TAG, "Sent JOIN_ROOM for room $roomId")
    }

    fun sendMessage(roomId: Long, content: String) {
        if (!isAuthenticated) {
            Log.e(TAG, "Cannot send message - not authenticated")
            return
        }
        val msg = mapOf("type" to "SEND_MESSAGE", "roomId" to roomId, "content" to content)
        webSocket?.send(gson.toJson(msg))
        Log.d(TAG, "Sent SEND_MESSAGE: $content")
    }

    private fun handleIncomingMessage(text: String) {
        try {
            val json = gson.fromJson(text, JsonObject::class.java)
            val type = json.get("type")?.asString ?: return

            Log.d(TAG, "Received: $type")

            when (type) {
                "CONNECTED", "AUTH_OK", "JOINED_ROOM" -> {
                    if (type == "AUTH_OK") isAuthenticated = true
                    if (type == "AUTH_OK" && currentRoomId != null) {
                        joinRoom(currentRoomId!!)   // ensure JOIN after AUTH
                    }
                    Log.d(TAG, "Success: $type")
                }
                "NEW_MESSAGE" -> {
                    val payload = json.getAsJsonObject("payload")
                    payload?.let {
                        val message = gson.fromJson(it, ChatMessageResponse::class.java)
                        onNewMessage(message)
                    }
                }
                "ERROR" -> {
                    val errorMsg = json.get("message")?.asString ?: json.get("payload")?.asString ?: "Unknown error"
                    Log.e(TAG, "❌ BACKEND ERROR: $errorMsg")
                    onError(errorMsg)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse message: $text", e)
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "User left chat")
        webSocket = null
        isAuthenticated = false
        currentRoomId = null
    }
}