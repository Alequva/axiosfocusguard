package com.axios.focusguard.ui.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axios.focusguard.data.FocusManager
import com.axios.focusguard.util.PermissionManager
import com.axios.focusguard.data.PresetRepository
import com.axios.focusguard.domain.model.TimerPreset
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val focusManager: FocusManager,
    private val presetRepository: PresetRepository,
    private val permissionManager: PermissionManager
) : ViewModel() {

    val uiState: StateFlow<TimerUiState> = focusManager.uiState

    private val _presets = MutableStateFlow<List<TimerPreset>>(emptyList())
    val presets: StateFlow<List<TimerPreset>> = _presets.asStateFlow()

    private val _selectedPresetId = MutableStateFlow("pomodoro")
    val selectedPresetId: StateFlow<String> = _selectedPresetId.asStateFlow()

    init {
        checkPermissions()
        loadPresets()
    }

    private fun loadPresets() {
        viewModelScope.launch {
            combine(
                presetRepository.getPresets(),
                presetRepository.getSelectedPresetId()
            ) { presets, selectedId ->
                _presets.value = presets
                _selectedPresetId.value = selectedId
            }.collect {}
        }
    }

    fun selectPreset(presetId: String) {
        viewModelScope.launch {
            presetRepository.selectPreset(presetId)
        }
    }

    fun createCustomPreset(name: String, focusTime: Int, breakTime: Int, rounds: Int) {
        viewModelScope.launch {
            val preset = TimerPreset(
                id = UUID.randomUUID().toString(),
                name = name,
                focusTimeMin = focusTime,
                breakTimeMin = breakTime,
                rounds = rounds,
                isDefault = false
            )
            presetRepository.saveCustomPreset(preset)
            presetRepository.selectPreset(preset.id)
        }
    }

    fun checkPermissions() {
        val hasAll = permissionManager.isUsageStatsPermissionGranted() && 
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
