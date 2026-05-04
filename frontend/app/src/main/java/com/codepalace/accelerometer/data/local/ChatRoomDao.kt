package com.codepalace.accelerometer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
@Dao
interface ChatRoomDao {

    @Query("""
        SELECT * FROM chat_rooms 
        WHERE userId = :userId 
          AND isSubscribed = 1 
        ORDER BY COALESCE(
            (SELECT MAX(createdAt) FROM chat_messages WHERE roomId = chat_rooms.id),
            createdAt
        ) DESC
    """)
    suspend fun getSubscribedRooms(userId: Long): List<ChatRoomEntity>

    @Query("SELECT * FROM chat_rooms WHERE userId = :userId")
    suspend fun getAllRoomsForUser(userId: Long): List<ChatRoomEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rooms: List<ChatRoomEntity>)

    @Query("UPDATE chat_rooms SET isSubscribed = :isSubscribed WHERE id = :roomId AND userId = :userId")
    suspend fun updateSubscription(roomId: Long, userId: Long, isSubscribed: Boolean)

    @Query("DELETE FROM chat_rooms WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: Long)

    // Pending actions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingAction(action: PendingChatActionEntity)

    @Query("SELECT * FROM pending_chat_actions")
    suspend fun getAllPendingActions(): List<PendingChatActionEntity>

    @Query("DELETE FROM pending_chat_actions WHERE id = :id")
    suspend fun deletePendingAction(id: Long)
}