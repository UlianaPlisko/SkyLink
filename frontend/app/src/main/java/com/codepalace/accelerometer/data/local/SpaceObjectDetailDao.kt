package com.codepalace.accelerometer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SpaceObjectDetailDao {

    @Query("SELECT * FROM space_object_details WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SpaceObjectDetailEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(detail: SpaceObjectDetailEntity)

    @Query("""
        DELETE FROM space_object_details 
        WHERE id NOT IN (
            SELECT id FROM space_object_details 
            ORDER BY timestamp DESC 
            LIMIT 50
        )
    """)
    suspend fun keepOnlyLatest50()
}