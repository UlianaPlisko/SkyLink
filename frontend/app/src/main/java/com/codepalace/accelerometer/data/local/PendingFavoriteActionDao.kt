package com.codepalace.accelerometer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PendingFavoriteActionDao {
    @Query("SELECT * FROM pending_favorite_actions ORDER BY timestamp ASC")
    suspend fun getAll(): List<PendingFavoriteActionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(action: PendingFavoriteActionEntity)

    @Query("DELETE FROM pending_favorite_actions WHERE spaceObjectId = :spaceObjectId")
    suspend fun deleteBySpaceObjectId(spaceObjectId: Long)
}
