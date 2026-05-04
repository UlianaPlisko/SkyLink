package com.codepalace.accelerometer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface EventDao {

    @Query("SELECT * FROM events WHERE date(startAt) = date(:date)")
    suspend fun getEventsByDate(date: String): List<EventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<EventEntity>)

    @Query("DELETE FROM events WHERE date(startAt) = date(:date)")
    suspend fun deleteByDate(date: String)

    @Query("DELETE FROM events")                    // ← Fixed + annotated
    suspend fun deleteAll()

    @Insert
    suspend fun insertPendingAction(action: PendingEventActionEntity)

    @Query("SELECT * FROM pending_event_actions ORDER BY timestamp ASC")
    suspend fun getAllPendingActions(): List<PendingEventActionEntity>

    @Query("DELETE FROM pending_event_actions WHERE id = :id")
    suspend fun deletePendingAction(id: Long)

    @Query("DELETE FROM pending_event_actions")
    suspend fun deleteAllPendingActions()

    @Query("SELECT * FROM events WHERE id = :eventId")
    suspend fun getEventById(eventId: Long): EventEntity?
}