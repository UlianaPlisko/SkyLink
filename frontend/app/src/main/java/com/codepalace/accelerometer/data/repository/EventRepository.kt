package com.codepalace.accelerometer.data.repository

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.codepalace.accelerometer.api.EventApi
import com.codepalace.accelerometer.data.local.AppDatabase
import com.codepalace.accelerometer.data.model.calendar.ScheduledEvent
import com.codepalace.accelerometer.data.model.dto.CreateEventRequest
import com.codepalace.accelerometer.data.model.dto.EventResponse
import com.codepalace.accelerometer.util.toDomain
import com.codepalace.accelerometer.util.toEntity
import com.codepalace.accelerometer.util.toScheduledEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EventRepository(
    private val api: EventApi,
    private val database: AppDatabase
) {

    private val eventDao = database.eventDao()

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun refreshEventsByDate(date: String): List<ScheduledEvent> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("EventRepository", "API call started for date: $date")

                val events = api.getEventsByDate(date)

                Log.d("EventRepository", "API call succeeded - Received ${events.size} events")
                events.forEach {
                    Log.d("EventRepository", "Event: ${it.id} - ${it.title} (${it.startAt}) | isParticipant=${it.participant}")
                }

                // ✅ NO extra API calls anymore
                val scheduledEvents = events.map { event ->
                    event.toScheduledEvent(event.participant)
                }

                // ✅ Store INCLUDING isParticipant
                eventDao.deleteByDate(date)
                eventDao.insertAll(events.map { it.toEntity() })

                Log.d("EventRepository", "Cache updated with ${events.size} events")

                scheduledEvents
            } catch (e: Exception) {
                Log.e("EventRepository", "API call FAILED for date: $date", e)
                getCachedEventsByDate(date)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getCachedEventsByDate(date: String): List<ScheduledEvent> {
        return withContext(Dispatchers.IO) {
            eventDao.getEventsByDate(date).map { it.toDomain() }
        }
    }

    suspend fun enroll(eventId: Long) {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("EventRepository", "Enrolling to event: $eventId")
                api.enroll(eventId)
                Log.d("EventRepository", "Successfully enrolled to event: $eventId")
            } catch (e: Exception) {
                Log.e("EventRepository", "Enrollment failed for event: $eventId", e)
                throw e  // Re-throw so the Activity can handle it
            }
        }
    }

    suspend fun signOut(eventId: Long) {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("EventRepository", "Signing out from event: $eventId")
                api.signOut(eventId)
                Log.d("EventRepository", "Successfully signed out from event: $eventId")
            } catch (e: Exception) {
                Log.e("EventRepository", "Sign out failed for event: $eventId", e)
                throw e  // Re-throw so the Activity can handle it
            }
        }
    }

    suspend fun createEvent(request: CreateEventRequest): EventResponse {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("EventRepository", "Creating event: ${request.title}")
                val createdEvent = api.createEvent(request)
                Log.d("EventRepository", "Event created successfully - ID: ${createdEvent.id}")
                createdEvent
            } catch (e: Exception) {
                Log.e("EventRepository", "Failed to create event", e)
                throw e
            }
        }
    }

}