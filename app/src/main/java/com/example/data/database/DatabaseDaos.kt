package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CalcHistoryDao {
    @Query("SELECT * FROM calc_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<CalcHistoryItem>>

    @Query("SELECT * FROM calc_history WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavorites(): Flow<List<CalcHistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryItem(item: CalcHistoryItem)

    @Update
    suspend fun updateHistoryItem(item: CalcHistoryItem)

    @Query("DELETE FROM calc_history WHERE id = :id")
    suspend fun deleteHistoryItem(id: Int)

    @Query("DELETE FROM calc_history")
    suspend fun clearAllHistory()
}

@Dao
interface AiSessionDao {
    @Query("SELECT * FROM ai_sessions ORDER BY isPinned DESC, timestamp DESC")
    fun getAllSessions(): Flow<List<AiSession>>

    @Query("SELECT * FROM ai_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: String): AiSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: AiSession)

    @Update
    suspend fun updateSession(session: AiSession)

    @Query("DELETE FROM ai_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Query("SELECT * FROM ai_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: String): Flow<List<AiMessageItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: AiMessageItem)

    @Query("DELETE FROM ai_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: String)
}
