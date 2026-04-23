package com.axios.focusguard.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_sessions")
data class FocusSession(
    @PrimaryKey val sessionId: String,
    val startTime: Long,
    val endTime: Long? = null,
    val type: String, // FOCUS, SHORT_BREAK, LONG_BREAK
    val isCompleted: Boolean = false
)
