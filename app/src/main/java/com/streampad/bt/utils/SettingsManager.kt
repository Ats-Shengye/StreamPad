package com.streampad.bt.utils

import android.content.Context
import android.content.SharedPreferences
import com.streampad.bt.model.ConnectionMode
import com.streampad.bt.model.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsManager(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("streampad_settings", Context.MODE_PRIVATE)
    
    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<Settings> = _settings
    
    private fun loadSettings(): Settings {
        val modeName = prefs.getString("connection_mode", ConnectionMode.BLUETOOTH.name)
        val mode = runCatching { ConnectionMode.valueOf(modeName ?: ConnectionMode.BLUETOOTH.name) }
            .getOrDefault(ConnectionMode.BLUETOOTH)
        return Settings(
            keyPressVibration = prefs.getBoolean("key_press_vibration", true),
            pageChangeVibration = prefs.getBoolean("page_change_vibration", true),
            visualFeedback = prefs.getBoolean("visual_feedback", true),
            silentMode = prefs.getBoolean("silent_mode", false),
            connectionMode = mode
        )
    }
    
    fun updateKeyPressVibration(enabled: Boolean) {
        val newSettings = _settings.value.copy(keyPressVibration = enabled)
        saveAndEmit(newSettings)
    }
    
    fun updatePageChangeVibration(enabled: Boolean) {
        val newSettings = _settings.value.copy(pageChangeVibration = enabled)
        saveAndEmit(newSettings)
    }
    
    fun updateVisualFeedback(enabled: Boolean) {
        val newSettings = _settings.value.copy(visualFeedback = enabled)
        saveAndEmit(newSettings)
    }
    
    fun updateSilentMode(enabled: Boolean) {
        val newSettings = _settings.value.copy(silentMode = enabled)
        saveAndEmit(newSettings)
    }
    
    fun updateConnectionMode(mode: ConnectionMode) {
        val newSettings = _settings.value.copy(connectionMode = mode)
        saveAndEmit(newSettings)
    }
    
    private fun saveAndEmit(settings: Settings) {
        prefs.edit()
            .putBoolean("key_press_vibration", settings.keyPressVibration)
            .putBoolean("page_change_vibration", settings.pageChangeVibration)
            .putBoolean("visual_feedback", settings.visualFeedback)
            .putBoolean("silent_mode", settings.silentMode)
            .putString("connection_mode", settings.connectionMode.name)
            .apply()
        _settings.value = settings
    }
}
