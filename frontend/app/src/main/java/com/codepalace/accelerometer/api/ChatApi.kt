package com.codepalace.accelerometer.api

import com.codepalace.accelerometer.data.model.dto.ChatMessageResponse
import com.codepalace.accelerometer.data.model.dto.ChatRoomResponse
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ChatApi {

    @GET("/api/chat/rooms")
    suspend fun getAllRooms(): List<ChatRoomResponse>

    @GET("/api/chat/rooms/me")
    suspend fun getMySubscribedRooms(): List<ChatRoomResponse>

    @POST("/api/chat/rooms/{roomId}/subscriptions")
    suspend fun subscribeCurrentUser(@Path("roomId") roomId: Long)

    @DELETE("/api/chat/rooms/{roomId}/subscriptions/me")
    suspend fun unsubscribeCurrentUser(@Path("roomId") roomId: Long)

    @GET("/api/chat/rooms/{roomId}/messages")
    suspend fun getRoomMessages(@Path("roomId") roomId: Long): List<ChatMessageResponse>}