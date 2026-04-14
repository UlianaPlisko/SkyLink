package com.codepalace.accelerometer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SpaceObjectDao {
    @Query("SELECT * FROM space_objects")
    fun getAll(): Flow<List<SpaceObjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(objects: List<SpaceObjectEntity>)

    @Query("DELETE FROM space_objects")
    suspend fun deleteAll()
}