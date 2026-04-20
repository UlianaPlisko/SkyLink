package com.codepalace.accelerometer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "space_objects")
data class SpaceObjectEntity(
    @PrimaryKey val id: Long,
    val displayName: String,
    val objectType: String,
    val raDeg: Double,
    val decDeg: Double,
    val magnitude: Double,
    val timestampCached: Long = System.currentTimeMillis()
)