package com.axios.focusguard.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axios.focusguard.data.AppInfo
import com.axios.focusguard.data.AppRepository
import com.axios.focusguard.util.PermissionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val apps: List<AppInfo> = emptyList(),
    val isLoading: Boolean = true,
    val isUsageEnabled: Boolean = false,
    val isOverlayEnabled: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: AppRepository,
    private val permissionManager: PermissionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadApps()
        updatePermissions()
    }

    fun updatePermissions() {
        _uiState.update { it.copy(
            isUsageEnabled = permissionManager.isUsageStatsPermissionGranted(),
            isOverlayEnabled = permissionManager.isOverlayPermissionGranted()
        )}
    }

    fun openUsageSettings() = permissionManager.openUsageSettings()
    fun openOverlaySettings() = permissionManager.openOverlaySettings()

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
