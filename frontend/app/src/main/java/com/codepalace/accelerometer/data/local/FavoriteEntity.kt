package com.codepalace.accelerometer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_objects")
data class FavoriteEntity(
    @PrimaryKey val spaceObjectId: Long,
    val favoriteId: Long,
    val displayName: String,
    val magnitude: Double,
    val objectType: String,
    val raDeg: Double,
    val decDeg: Double,
    val description: String?,
    val note: String?,
    val visibility: Double,
    val addedAt: String?,
    val timestamp: Long = System.currentTimeMillis()
)
