package com.axios.focusguard.ui.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axios.focusguard.data.AiRepository
import com.axios.focusguard.data.AppRepository
import com.axios.focusguard.data.FocusManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnalysisUiState(
    val analysisText: String = "",
    val isLoading: Boolean = true,
    val events: List<com.axios.focusguard.data.SessionEvent> = emptyList()
)

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val aiRepository: AiRepository,
    private val appRepository: AppRepository,
    private val focusManager: FocusManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalysisUiState())
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    init {
        runAnalysis()
    }

    private fun runAnalysis() {
        viewModelScope.launch {
            val sessionId = focusManager.lastCompletedSessionId
            if (sessionId != null) {
                val events = appRepository.getEventsForSession(sessionId).first()
                val result = aiRepository.getSessionAnalysis(events)
                _uiState.value = AnalysisUiState(analysisText = result, isLoading = false, events = events)
            } else {
                _uiState.value = AnalysisUiState(
                    analysisText = "No session data found to analyze.",
                    isLoading = false
                )
            }
        }
    }
}
