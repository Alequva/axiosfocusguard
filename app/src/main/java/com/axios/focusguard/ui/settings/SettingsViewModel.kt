package com.axios.focusguard.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axios.focusguard.data.AppInfo
import com.axios.focusguard.data.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val apps: List<AppInfo> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            val installedApps = repository.getInstalledApps()
            repository.getBlockedApps().collect { blockedApps ->
                val blockedPackages = blockedApps.map { it.packageName }.toSet()
                _uiState.update { state ->
                    state.copy(
                        apps = installedApps.map { it.copy(isBlocked = blockedPackages.contains(it.packageName)) },
                        isLoading = false
                    )
                }
            }
        }
    }

    fun toggleAppBlock(app: AppInfo) {
        viewModelScope.launch {
            repository.toggleAppBlock(app.packageName, app.name, !app.isBlocked)
        }
    }
}
