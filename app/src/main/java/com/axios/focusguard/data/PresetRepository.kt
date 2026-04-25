package com.axios.focusguard.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.axios.focusguard.domain.model.TimerPreset
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PresetRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val gson: Gson
) {
    companion object {
        val CUSTOM_PRESETS_KEY = stringPreferencesKey("custom_presets")
        val SELECTED_PRESET_ID_KEY = stringPreferencesKey("selected_preset_id")
    }

    val defaultPresets = listOf(
        TimerPreset("easy", "Easy Task", 25, 5, 4, true, true, "Best for simple tasks. 25 min focus keeps you fresh."),
        TimerPreset("medium", "Medium Task", 40, 15, 4, true, true, "Ideal for moderate work. 40 min focus gives enough time to dive deep."),
        TimerPreset("hard", "Hard Task", 120, 30, 4, true, true, "120 min focus. User more likely to go into flow state and do task more productively."),
        TimerPreset("pomodoro", "Pomodoro", 25, 5, 4, true, false),
        TimerPreset("5217", "52/17", 52, 17, 4, true, false)
    )

    fun getPresets(): Flow<List<TimerPreset>> = dataStore.data.map { prefs ->
        val customJson = prefs[CUSTOM_PRESETS_KEY] ?: "[]"
        val type = object : TypeToken<List<TimerPreset>>() {}.type
        val customPresets: List<TimerPreset> = try {
            gson.fromJson(customJson, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        defaultPresets + customPresets
    }

    fun getSelectedPresetId(): Flow<String> = dataStore.data.map { prefs ->
        prefs[SELECTED_PRESET_ID_KEY] ?: "pomodoro"
    }

    suspend fun saveCustomPreset(preset: TimerPreset) {
        dataStore.edit { prefs ->
            val customJson = prefs[CUSTOM_PRESETS_KEY] ?: "[]"
            val type = object : TypeToken<List<TimerPreset>>() {}.type
            val customPresets: MutableList<TimerPreset> = try {
                gson.fromJson(customJson, type) ?: mutableListOf()
            } catch (e: Exception) {
                mutableListOf()
            }
            customPresets.add(preset)
            prefs[CUSTOM_PRESETS_KEY] = gson.toJson(customPresets)
        }
    }

    suspend fun selectPreset(id: String) {
        dataStore.edit { prefs ->
            prefs[SELECTED_PRESET_ID_KEY] = id
        }
    }
}
