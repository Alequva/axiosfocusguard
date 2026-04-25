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
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.axios.focusguard.util.Constants
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import javax.inject.Provider

@Singleton
class FocusManager @Inject constructor(
    private val repositoryProvider: Provider<AppRepository>
) {
    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

    private var _lastCompletedSessionId: String? = null
    val lastCompletedSessionId: String? get() = _lastCompletedSessionId

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
        if (_uiState.value.timeLeftSeconds <= 0) {
            moveToNextSession()
        }

        val currentState = _uiState.value
        val newId = _currentSessionId.value ?: UUID.randomUUID().toString()
        _currentSessionId.value = newId
        _lastCompletedSessionId = newId
        
        scope.launch {
            repositoryProvider.get().saveFocusSession(
                FocusSession(
                    sessionId = newId,
                    startTime = System.currentTimeMillis(),
                    type = currentState.sessionType.name,
                    isCompleted = false
                )
            )
        }

        _uiState.update { it.copy(isRunning = true) }
        startTimerLogic()
    }

    private fun startTimerLogic() {
        timerJob?.cancel()
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

    fun finishSessionEarly() {
        val sessionId = _currentSessionId.value
        pauseTimer()
        
        scope.launch {
            sessionId?.let { id ->
                repositoryProvider.get().saveFocusSession(
                    FocusSession(
                        sessionId = id,
                        startTime = 0, 
                        endTime = System.currentTimeMillis(),
                        type = _uiState.value.sessionType.name,
                        isCompleted = true
                    )
                )
            }
            moveToNextSession()
        }
    }

    fun resetToStart() {
        pauseTimer()
        _currentSessionId.value = null
        moveToState(SessionType.FOCUS, Constants.FOCUS_DURATION_SEC, 0)
    }

    private fun onTimerFinished() {
        val sessionId = _currentSessionId.value
        pauseTimer()
        
        scope.launch {
            sessionId?.let { id ->
                repositoryProvider.get().saveFocusSession(
                    FocusSession(
                        sessionId = id,
                        startTime = 0, 
                        endTime = System.currentTimeMillis(),
                        type = _uiState.value.sessionType.name,
                        isCompleted = true
                    )
                )
            }
            moveToNextSession()
        }
    }

    private fun moveToNextSession() {
        val currentState = _uiState.value
        if (currentState.sessionType == SessionType.FOCUS) {
            val newCompletedCount = currentState.completedFocusSessions + 1
            if (newCompletedCount % Constants.CYCLES_BEFORE_LONG == 0) {
                moveToState(SessionType.LONG_BREAK, Constants.LONG_BREAK_SEC, newCompletedCount)
            } else {
                moveToState(SessionType.SHORT_BREAK, Constants.SHORT_BREAK_SEC, newCompletedCount)
            }
        } else {
            moveToState(SessionType.FOCUS, Constants.FOCUS_DURATION_SEC, currentState.completedFocusSessions)
        }
        _currentSessionId.value = null
    }

    private fun moveToState(type: SessionType, seconds: Int, completedCount: Int) {
        _uiState.update {
            it.copy(
                sessionType = type,
                timeLeftSeconds = seconds,
                completedFocusSessions = completedCount,
                isRunning = false
            )
        }
    }
    
    fun updatePermissionStatus(hasPermissions: Boolean) {
        _uiState.update { it.copy(hasPermissions = hasPermissions) }
    }
}
