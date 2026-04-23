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
                repository.getEventsForSession(sessionId).collect { events ->
                    _uiState.value = ResultsUiState(events = events, isLoading = false)
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
