package com.axios.focusguard.ui.timer

import androidx.lifecycle.ViewModel
import com.axios.focusguard.data.FocusManager
import com.axios.focusguard.util.PermissionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val focusManager: FocusManager,
    private val permissionManager: PermissionManager
) : ViewModel() {

    val uiState: StateFlow<TimerUiState> = focusManager.uiState

    init {
        checkPermissions()
    }

    fun checkPermissions() {
        val hasAll = permissionManager.isAccessibilityServiceEnabled() && 
                     permissionManager.isOverlayPermissionGranted()
        focusManager.updatePermissionStatus(hasAll)
    }

    fun toggleTimer() {
        focusManager.toggleTimer()
    }

    fun finishSessionEarly() {
        focusManager.finishSessionEarly()
    }
}
