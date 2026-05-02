package com.codepalace.accelerometer.api

import com.codepalace.accelerometer.data.model.dto.CreateEventRequest
import com.codepalace.accelerometer.data.model.dto.EventResponse
import retrofit2.http.*

interface EventApi {

    @GET("/api/events")
    suspend fun getAllEvents(): List<EventResponse>

    @GET("/api/events/date")
    suspend fun getEventsByDate(
        @Query("date") date: String // format: yyyy-MM-dd
    ): List<EventResponse>

    @GET("/api/events/my")
    suspend fun getMyEvents(): List<EventResponse>

    @POST("/api/events/{eventId}/enroll")
    suspend fun enroll(@Path("eventId") eventId: Long)

    @DELETE("/api/events/{eventId}/enroll")
    suspend fun signOut(@Path("eventId") eventId: Long)

    @POST("/api/events")
    suspend fun createEvent(@Body request: CreateEventRequest): EventResponse
}