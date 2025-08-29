package com.spacetec.diagnostic.core

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * Centralized configuration management for the SpaceTec diagnostic app
 */
class ConfigManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "spacetec_config", Context.MODE_PRIVATE
    )
    
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    private val _config = MutableStateFlow(loadConfig())
    val config: StateFlow<AppConfig> = _config.asStateFlow()
    
    fun updateConfig(newConfig: AppConfig) {
        _config.value = newConfig
        saveConfig(newConfig)
    }
    
    fun updateConnectionConfig(connectionConfig: ConnectionConfig) {
        val currentConfig = _config.value
        updateConfig(currentConfig.copy(connection = connectionConfig))
    }
    
    fun updateUIConfig(uiConfig: UIConfig) {
        val currentConfig = _config.value
        updateConfig(currentConfig.copy(ui = uiConfig))
    }
    
    fun updateDiagnosticConfig(diagnosticConfig: DiagnosticConfig) {
        val currentConfig = _config.value
        updateConfig(currentConfig.copy(diagnostic = diagnosticConfig))
    }
    
    private fun loadConfig(): AppConfig {
        return try {
            val configJson = prefs.getString(CONFIG_KEY, null)
            if (configJson != null) {
                json.decodeFromString<AppConfig>(configJson)
            } else {
                AppConfig() // Return default config
            }
        } catch (e: Exception) {
            android.util.Log.w("ConfigManager", "Failed to load config: ${e.message}")
            AppConfig() // Return default config on error
        }
    }
    
    private fun saveConfig(config: AppConfig) {
        try {
            val configJson = json.encodeToString(config)
            prefs.edit()
                .putString(CONFIG_KEY, configJson)
                .apply()
        } catch (e: Exception) {
            android.util.Log.e("ConfigManager", "Failed to save config: ${e.message}")
        }
    }
    
    // Convenience methods for commonly accessed settings
    fun getConnectionTimeout(): Long = _config.value.connection.timeout
    fun getMaxRetries(): Int = _config.value.connection.maxRetries
    fun isSpaceThemeEnabled(): Boolean = _config.value.ui.spaceThemeEnabled
    fun getPidPollingInterval(): Long = _config.value.diagnostic.pidPollingInterval
    
    companion object {
        private const val CONFIG_KEY = "app_config"
    }
}

@Serializable
data class AppConfig(
    val connection: ConnectionConfig = ConnectionConfig(),
    val ui: UIConfig = UIConfig(),
    val diagnostic: DiagnosticConfig = DiagnosticConfig(),
    val data: DataConfig = DataConfig(),
    val advanced: AdvancedConfig = AdvancedConfig()
)

@Serializable
data class ConnectionConfig(
    val timeout: Long = 5000,
    val maxRetries: Int = 3,
    val retryDelay: Long = 500,
    val autoReconnect: Boolean = true,
    val connectionType: ConnectionType = ConnectionType.BLUETOOTH_CLASSIC,
    val preferredAdapter: String? = null,
    val keepAliveInterval: Long = 10000
)

@Serializable
data class UIConfig(
    val spaceThemeEnabled: Boolean = true,
    val darkModeEnabled: Boolean = false,
    val showAdvancedMetrics: Boolean = false,
    val compactMode: Boolean = false,
    val animationsEnabled: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val screenAlwaysOn: Boolean = false,
    val language: String = "en"
)

@Serializable
data class DiagnosticConfig(
    val pidPollingInterval: Long = 1000,
    val useEnhancedPids: Boolean = true
)

@Serializable
data class DataConfig(
    val enableDataLogging: Boolean = true,
    val maxLogFileSize: Long = 50 * 1024 * 1024, // 50MB
    val maxLogFiles: Int = 10,
    val autoExportEnabled: Boolean = false,
    val exportFormat: ExportFormat = ExportFormat.CSV,
    val cloudSyncEnabled: Boolean = false,
    val anonymizeData: Boolean = true
)

@Serializable
data class AdvancedConfig(
    val debugLoggingEnabled: Boolean = false,
    val developerModeEnabled: Boolean = false,
    val customCommandsEnabled: Boolean = false,
    val allowUnsafeCommands: Boolean = false,
    val performanceModeEnabled: Boolean = false,
    val batteryOptimizationEnabled: Boolean = true,
    val networkTimeoutMs: Long = 10000,
    val cacheMaxSizeMB: Int = 100
)

@Serializable
enum class ConnectionType {
    BLUETOOTH_CLASSIC,
    BLUETOOTH_LE,
    WIFI,
    USB
}

@Serializable
enum class ExportFormat {
    CSV,
    JSON,
    XML
}

/**
 * Configuration presets for different use cases
 */
object ConfigPresets {
    
    fun performance(): AppConfig = AppConfig(
        connection = ConnectionConfig(
            timeout = 3000,
            maxRetries = 2,
            retryDelay = 200
        ),
        diagnostic = DiagnosticConfig(
            pidPollingInterval = 100,
            useEnhancedPids = true
        ),
        ui = UIConfig(
            compactMode = true,
            animationsEnabled = false
        ),
        advanced = AdvancedConfig(
            performanceModeEnabled = true,
            batteryOptimizationEnabled = false
        )
    )
    
    fun batteryOptimized(): AppConfig = AppConfig(
        connection = ConnectionConfig(
            timeout = 8000,
            maxRetries = 1,
            keepAliveInterval = 30000
        ),
        diagnostic = DiagnosticConfig(
            pidPollingInterval = 1000,
            useEnhancedPids = true
        ),
        ui = UIConfig(
            animationsEnabled = false,
            screenAlwaysOn = false
        ),
        advanced = AdvancedConfig(
            performanceModeEnabled = false,
            batteryOptimizationEnabled = true
        )
    )
    
    fun professional(): AppConfig = AppConfig(
        connection = ConnectionConfig(
            timeout = 10000,
            maxRetries = 5,
            autoReconnect = true
        ),
        diagnostic = DiagnosticConfig(
            pidPollingInterval = 100,
            useEnhancedPids = true
        ),
        data = DataConfig(
            enableDataLogging = true,
            maxLogFileSize = 100 * 1024 * 1024,
            autoExportEnabled = true
        ),
        ui = UIConfig(
            showAdvancedMetrics = true,
            spaceThemeEnabled = false // Professional look
        ),
        advanced = AdvancedConfig(
            debugLoggingEnabled = true,
            customCommandsEnabled = true
        )
    )
    
    fun beginner(): AppConfig = AppConfig(
        connection = ConnectionConfig(
            timeout = 8000,
            maxRetries = 3,
            autoReconnect = true
        ),
        diagnostic = DiagnosticConfig(
            pidPollingInterval = 500,
            useEnhancedPids = true
        ),
        ui = UIConfig(
            spaceThemeEnabled = true,
            compactMode = false,
            showAdvancedMetrics = false
        ),
        advanced = AdvancedConfig(
            debugLoggingEnabled = false,
            developerModeEnabled = false,
            customCommandsEnabled = false
        )
    )
}
