package com.codepalace.accelerometer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ChatMessageDao {

    @Query("""
        SELECT * FROM chat_messages 
        WHERE roomId = :roomId 
        ORDER BY createdAt DESC 
        LIMIT 20
    """)
    suspend fun getLast20Messages(roomId: Long): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE roomId = :roomId")
    suspend fun deleteAllForRoom(roomId: Long)
}