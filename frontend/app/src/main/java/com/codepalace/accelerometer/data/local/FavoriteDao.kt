package com.codepalace.accelerometer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorite_objects ORDER BY timestamp DESC")
    suspend fun getAll(): List<FavoriteEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_objects WHERE spaceObjectId = :spaceObjectId)")
    suspend fun exists(spaceObjectId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(favorites: List<FavoriteEntity>)

    @Query("DELETE FROM favorite_objects")
    suspend fun deleteAll()

    @Query("DELETE FROM favorite_objects WHERE spaceObjectId = :spaceObjectId")
    suspend fun deleteBySpaceObjectId(spaceObjectId: Long)

    @Transaction
    suspend fun replaceAll(favorites: List<FavoriteEntity>) {
        deleteAll()
        insertAll(favorites)
    }
}
