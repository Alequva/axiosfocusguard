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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.axios.focusguard.util.Constants
import java.util.UUID
import com.axios.focusguard.domain.model.TimerPreset
import javax.inject.Inject
import javax.inject.Singleton
import javax.inject.Provider

@Singleton
class FocusManager @Inject constructor(
    private val repositoryProvider: Provider<AppRepository>,
    private val presetRepository: PresetRepository
) {
    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

    private var _lastCompletedSessionId: String? = null
    val lastCompletedSessionId: String? get() = _lastCompletedSessionId

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var timerJob: Job? = null
    
    private var activePreset: TimerPreset? = null

    val isFocusActive: Boolean
        get() = _uiState.value.isRunning && _uiState.value.sessionType == SessionType.FOCUS

    init {
        scope.launch {
            combine(
                presetRepository.getSelectedPresetId(),
                presetRepository.getPresets()
            ) { id, presets ->
                presets.find { it.id == id } ?: presetRepository.defaultPresets.first()
            }.collect { preset ->
                activePreset = preset
                if (!_uiState.value.isRunning) {
                    resetToStart()
                }
            }
        }
    }

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
        val preset = activePreset
        val focusSec = (preset?.focusTimeMin ?: 25) * 60
        val rounds = preset?.rounds ?: 4
        moveToState(SessionType.FOCUS, focusSec, 0, rounds)
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
        val preset = activePreset
        val focusSec = (preset?.focusTimeMin ?: 25) * 60
        val breakSec = (preset?.breakTimeMin ?: 5) * 60
        val longBreakSec = breakSec * 3 // or whatever default
        val rounds = preset?.rounds ?: 4

        if (currentState.sessionType == SessionType.FOCUS) {
            val newCompletedCount = currentState.completedFocusSessions + 1
            if (newCompletedCount % rounds == 0) {
                moveToState(SessionType.LONG_BREAK, longBreakSec, newCompletedCount, rounds)
            } else {
                moveToState(SessionType.SHORT_BREAK, breakSec, newCompletedCount, rounds)
            }
        } else {
            moveToState(SessionType.FOCUS, focusSec, currentState.completedFocusSessions, rounds)
        }
        _currentSessionId.value = null
    }

    private fun moveToState(type: SessionType, seconds: Int, completedCount: Int, totalRounds: Int) {
        _uiState.update {
            it.copy(
                sessionType = type,
                timeLeftSeconds = seconds,
                initialSessionSeconds = seconds,
                completedFocusSessions = completedCount,
                totalRounds = totalRounds,
                isRunning = false
            )
        }
    }
    
    fun updatePermissionStatus(hasPermissions: Boolean) {
        _uiState.update { it.copy(hasPermissions = hasPermissions) }
    }
}
