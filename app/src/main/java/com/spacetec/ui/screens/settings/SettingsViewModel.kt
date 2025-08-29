package com.spacetec.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.spacetec.diagnostic.config.ConfigManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val configManager = ConfigManager(app.applicationContext)

    private val _offlineEnabled = MutableStateFlow(configManager.getConfig().offlineEnabled)
    val offlineEnabled: StateFlow<Boolean> = _offlineEnabled.asStateFlow()

    fun setOfflineEnabled(enabled: Boolean) {
        viewModelScope.launch {
            configManager.updateOfflineEnabled(enabled)
            _offlineEnabled.update { enabled }
        }
    }
}
