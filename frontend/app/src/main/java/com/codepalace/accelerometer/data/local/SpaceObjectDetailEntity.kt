package com.codepalace.accelerometer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "space_object_details")
data class SpaceObjectDetailEntity(
    @PrimaryKey val id: Long,

    val displayName: String,
    val objectClass: String?,
    val spectralType: String?,
    val constellation: String?,
    val magnitude: Double?,
    val distanceLy: Double?,
    val raDeg: Double,
    val decDeg: Double,

    val description: String?,
    val wikiSummary: String?,
    val wikiUrl: String?,
    val imageUrl: String?,
    val orbitalModel: String?,
    val lastComputed: String?,
    val catalogId: String?,
    val angularSize: Double?,

    val timestamp: Long = System.currentTimeMillis()
)
