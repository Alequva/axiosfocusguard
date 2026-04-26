package com.axios.focusguard.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "session_events")
data class SessionEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String, // Unique ID for the current Pomodoro focus session
    val packageName: String,
    val appName: String,
    val category: String, // SOCIAL, VIDEO, GAME, etc.
    val sessionOffsetSeconds: Int, // seconds into the session
    val timestamp: Long = System.currentTimeMillis()
)
