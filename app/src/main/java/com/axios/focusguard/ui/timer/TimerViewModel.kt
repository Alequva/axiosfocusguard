package com.axios.focusguard.ui.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SessionType {
    FOCUS, SHORT_BREAK, LONG_BREAK
}

data class TimerUiState(
    val timeLeftSeconds: Int = 25 * 60,
    val isRunning: Boolean = false,
    val sessionType: SessionType = SessionType.FOCUS,
    val completedFocusSessions: Int = 0
)

@HiltViewModel
class TimerViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    fun toggleTimer() {
        if (_uiState.value.isRunning) {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    private fun startTimer() {
        _uiState.update { it.copy(isRunning = true) }
        timerJob = viewModelScope.launch {
            while (_uiState.value.timeLeftSeconds > 0) {
                delay(1000)
                _uiState.update { it.copy(timeLeftSeconds = it.timeLeftSeconds - 1) }
            }
            onTimerFinished()
        }
    }

    private fun pauseTimer() {
        timerJob?.cancel()
        _uiState.update { it.copy(isRunning = false) }
    }

    private fun onTimerFinished() {
        pauseTimer()
        val currentState = _uiState.value
        
        when (currentState.sessionType) {
            SessionType.FOCUS -> {
                val newCompletedCount = currentState.completedFocusSessions + 1
                if (newCompletedCount % 4 == 0) {
                    moveToState(SessionType.LONG_BREAK, 15 * 60, newCompletedCount)
                } else {
                    moveToState(SessionType.SHORT_BREAK, 5 * 60, newCompletedCount)
                }
            }
            SessionType.SHORT_BREAK, SessionType.LONG_BREAK -> {
                moveToState(SessionType.FOCUS, 25 * 60, currentState.completedFocusSessions)
            }
        }
    }

    private fun moveToState(type: SessionType, seconds: Int, completedCount: Int) {
        _uiState.update {
            it.copy(
                sessionType = type,
                timeLeftSeconds = seconds,
                completedFocusSessions = completedCount
            )
        }
    }
}
