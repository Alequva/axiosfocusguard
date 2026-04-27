package com.axios.focusguard.ui.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axios.focusguard.data.AppRepository
import com.axios.focusguard.data.FocusManager
import com.axios.focusguard.data.SessionEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResultsUiState(
    val events: List<SessionEvent> = emptyList(),
    val sessionDurationSeconds: Int = 1500, // Default 25 min
    val isLoading: Boolean = true
)

@HiltViewModel
class ResultsViewModel @Inject constructor(
    private val repository: AppRepository,
    private val focusManager: FocusManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResultsUiState())
    val uiState: StateFlow<ResultsUiState> = _uiState.asStateFlow()

    init {
        loadSessionResults()
    }

    private fun loadSessionResults() {
        val sessionId = focusManager.lastCompletedSessionId
        if (sessionId != null) {
            viewModelScope.launch {
                // Fetch the session to get the actual duration
                val session = repository.getSessionById(sessionId)
                val startTime = session?.startTime ?: 0L
                val endTime = session?.endTime ?: startTime
                val duration = if (endTime > startTime) {
                    ((endTime - startTime) / 1000L).toInt()
                } else {
                    1500 // Fallback
                }
                
                repository.getEventsForSession(sessionId).collect { events ->
                    val maxOffset = events.maxOfOrNull { it.sessionOffsetSeconds } ?: 0
                    val finalDuration = duration.coerceAtLeast(maxOffset).coerceAtLeast(1)
                    
                    _uiState.value = ResultsUiState(
                        events = events, 
                        sessionDurationSeconds = finalDuration,
                        isLoading = false
                    )
                }
            }
        } else {
            _uiState.value = ResultsUiState(isLoading = false)
        }
    }

    fun resetToStart() {
        focusManager.resetToStart()
    }
}
