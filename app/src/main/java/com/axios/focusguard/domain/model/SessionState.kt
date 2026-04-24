package com.axios.focusguard.domain.model

enum class PomodoroPhase {
    FOCUS, SHORT_BREAK, LONG_BREAK
}

data class SessionState(
    val phase: PomodoroPhase = PomodoroPhase.FOCUS,
    val remainingSeconds: Int = 25 * 60,
    val cycleIndex: Int = 0,       // 0-based, out of total cycles
    val isRunning: Boolean = false,
    val isFinished: Boolean = false
)
