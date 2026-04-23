package com.axios.focusguard.data

import com.axios.focusguard.ui.timer.SessionType
import com.axios.focusguard.ui.timer.TimerUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FocusManager @Inject constructor() {
    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var timerJob: Job? = null

    val isFocusActive: Boolean
        get() = _uiState.value.isRunning && _uiState.value.sessionType == SessionType.FOCUS

    fun toggleTimer() {
        if (_uiState.value.isRunning) {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    private fun startTimer() {
        if (_uiState.value.sessionType == SessionType.FOCUS) {
            _currentSessionId.value = UUID.randomUUID().toString()
        }
        _uiState.update { it.copy(isRunning = true) }
        timerJob = scope.launch {
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
        if (type != SessionType.FOCUS) {
            _currentSessionId.value = null
        }
    }
    
    fun updatePermissionStatus(hasPermissions: Boolean) {
        _uiState.update { it.copy(hasPermissions = hasPermissions) }
    }
}
