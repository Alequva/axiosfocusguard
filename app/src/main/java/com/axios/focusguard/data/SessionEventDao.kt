package com.axios.focusguard.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionEventDao {
    @Insert
    suspend fun insertEvent(event: SessionEvent)

    @Query("SELECT * FROM session_events WHERE sessionId = :sessionId")
    fun getEventsForSession(sessionId: String): Flow<List<SessionEvent>>

    @Query("SELECT * FROM session_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<SessionEvent>>
    
    @Query("DELETE FROM session_events WHERE sessionId = :sessionId")
    suspend fun deleteEventsForSession(sessionId: String)
}
