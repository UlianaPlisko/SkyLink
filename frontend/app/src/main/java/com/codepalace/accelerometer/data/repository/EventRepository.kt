package com.codepalace.accelerometer.data.repository

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.codepalace.accelerometer.api.EventApi
import com.codepalace.accelerometer.data.local.AppDatabase
import com.codepalace.accelerometer.data.local.PendingEventActionEntity
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

    // ====================== REFRESH (unchanged, just kept clean) ======================
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun refreshEventsByDate(date: String): List<ScheduledEvent> {
        return withContext(Dispatchers.IO) {
            try {
                val apiEvents = api.getEventsByDate(date)
                val sorted = apiEvents.sortedBy { it.startAt }
                val scheduled = sorted.map { it.toScheduledEvent() }

                eventDao.deleteByDate(date)
                eventDao.insertAll(sorted.map { it.toEntity() })

                scheduled
            } catch (e: Exception) {
                Log.e("EventRepository", "API failed → using cache", e)
                getCachedEventsByDate(date)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getCachedEventsByDate(date: String): List<ScheduledEvent> {
        return withContext(Dispatchers.IO) {
            eventDao.getEventsByDate(date)
                .map { it.toDomain() }
                .sortedBy { event ->
                    event.startTime?.let {
                        val (h, m) = it.split(":").map(String::toInt)
                        h * 60 + m
                    } ?: Int.MAX_VALUE
                }
        }
    }

    // ====================== OFFLINE-FRIENDLY ENROLL / SIGN OUT ======================
    suspend fun enroll(eventId: Long) {
        processAction(eventId, "ENROLL")
    }

    suspend fun signOut(eventId: Long) {
        processAction(eventId, "SIGN_OUT")
    }

    private suspend fun processAction(eventId: Long, action: String) {
        withContext(Dispatchers.IO) {
            val event = eventDao.getEventById(eventId) ?: return@withContext

            val newIsEnrolled = action == "ENROLL"
            val newCount = if (action == "ENROLL") {
                event.participantsCount + 1
            } else {
                (event.participantsCount - 1).coerceAtLeast(0)
            }

            val updatedEvent = event.copy(
                isParticipant = newIsEnrolled,
                participantsCount = newCount
            )

            eventDao.insertAll(listOf(updatedEvent))
            eventDao.insertPendingAction(PendingEventActionEntity(eventId = eventId, action = action))

            Log.d("EventRepository", "Optimistic $action queued for event $eventId")
        }
    }

    // ====================== SYNC PENDING ACTIONS ======================
    suspend fun syncPendingEventActions() {
        withContext(Dispatchers.IO) {
            val pendingList = eventDao.getAllPendingActions()
            if (pendingList.isEmpty()) return@withContext

            Log.d("EventRepository", "Syncing ${pendingList.size} pending event actions...")

            for (pending in pendingList) {
                try {
                    when (pending.action) {
                        "ENROLL" -> api.enroll(pending.eventId)
                        "SIGN_OUT" -> api.signOut(pending.eventId)
                    }
                    eventDao.deletePendingAction(pending.id)
                    Log.d("EventRepository", "✅ Synced ${pending.action} event ${pending.eventId}")

                } catch (e: Exception) {
                    Log.e("EventRepository", "Sync failed for ${pending.action} event ${pending.eventId}", e)

                    if (e is retrofit2.HttpException && e.code() == 409 && pending.action == "ENROLL") {
                        // Server says full or already enrolled → revert local state
                        revertLocalEnrollment(pending.eventId)
                        Log.d("EventRepository", "Reverted enrollment for event ${pending.eventId} (capacity full)")
                    }
                    // We still delete the pending action so we don't retry forever
                    eventDao.deletePendingAction(pending.id)
                }
            }
        }
    }

    private suspend fun revertLocalEnrollment(eventId: Long) {
        val event = eventDao.getEventById(eventId) ?: return
        val reverted = event.copy(
            isParticipant = false,
            participantsCount = (event.participantsCount - 1).coerceAtLeast(0)
        )
        eventDao.insertAll(listOf(reverted))
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

    suspend fun clearEventsCache() {
        withContext(Dispatchers.IO) {
            eventDao.deleteAll()
            Log.d("EventRepository", "All events cache cleared successfully")
        }
    }

}