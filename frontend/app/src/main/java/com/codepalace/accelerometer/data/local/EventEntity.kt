package com.codepalace.accelerometer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val description: String?,
    val eventType: String,
    val startAt: String, // store as ISO string
    val endAt: String?,
    val creatorId: Long,
    val participantsCount: Int,
    val isParticipant: Boolean
)