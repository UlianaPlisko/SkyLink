package com.codepalace.accelerometer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_favorite_actions")
data class PendingFavoriteActionEntity(
    @PrimaryKey val spaceObjectId: Long,
    val action: String,
    val timestamp: Long = System.currentTimeMillis()
)
