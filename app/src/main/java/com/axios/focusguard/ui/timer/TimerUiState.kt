package com.axios.focusguard.ui.timer

enum class SessionType {
    FOCUS, SHORT_BREAK, LONG_BREAK
}

data class TimerUiState(
    val timeLeftSeconds: Int = 25 * 60,
    val initialSessionSeconds: Int = 25 * 60,
    val isRunning: Boolean = false,
    val sessionType: SessionType = SessionType.FOCUS,
    val completedFocusSessions: Int = 0,
    val totalRounds: Int = 4,
    val hasPermissions: Boolean = false
)
