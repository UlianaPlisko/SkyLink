package com.codepalace.accelerometer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_event_actions")
data class PendingEventActionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val eventId: Long,
    val action: String,           // "ENROLL" or "SIGN_OUT"
    val timestamp: Long = System.currentTimeMillis()
)