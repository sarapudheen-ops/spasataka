package com.spacetec.diagnostic.config

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * Application configuration data class
 */
@Serializable
data class AppConfig(
    val connectionTimeout: Long = 5000,
    val pidPollingInterval: Long = 250,
    val maxRetries: Int = 3,
    val enableAdvancedDiagnostics: Boolean = true,
    val spaceThemeEnabled: Boolean = true,
    val autoReconnect: Boolean = true,
    val logLevel: LogLevel = LogLevel.INFO,
    val preferredProtocol: String = "AUTO",
    val enableDataLogging: Boolean = true,
    val maxLogFileSize: Long = 10 * 1024 * 1024, // 10MB
    val enableCloudSync: Boolean = false,
    val units: Units = Units.METRIC,
    val offlineEnabled: Boolean = false
)

@Serializable
enum class LogLevel {
    DEBUG, INFO, WARN, ERROR
}

@Serializable
enum class Units {
    METRIC, IMPERIAL
}

/**
 * Manages application configuration with persistence and reactive updates
 */
class ConfigManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("obd_config", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    
    private val _config = MutableStateFlow(loadConfig())
    val config: StateFlow<AppConfig> = _config.asStateFlow()
    
    companion object {
        private const val CONFIG_KEY = "app_config"
        private const val TAG = "ConfigManager"
    }
    
    /**
     * Get current configuration
     */
    fun getConfig(): AppConfig = _config.value
    
    /**
     * Save configuration with validation
     */
    fun saveConfig(config: AppConfig) {
        val validatedConfig = validateConfig(config)
        
        try {
            val configJson = json.encodeToString(validatedConfig)
            prefs.edit()
                .putString(CONFIG_KEY, configJson)
                .apply()
            
            _config.value = validatedConfig
            android.util.Log.i(TAG, "Configuration saved successfully")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to save configuration: ${e.message}")
        }
    }
    
    /**
     * Update specific configuration field
     */
    fun updateConnectionTimeout(timeout: Long) {
        saveConfig(getConfig().copy(connectionTimeout = timeout.coerceIn(1000, 30000)))
    }
    
    fun updatePollingInterval(interval: Long) {
        saveConfig(getConfig().copy(pidPollingInterval = interval.coerceIn(100, 5000)))
    }
    
    fun updateMaxRetries(retries: Int) {
        saveConfig(getConfig().copy(maxRetries = retries.coerceIn(1, 10)))
    }
    
    fun updateAdvancedDiagnostics(enabled: Boolean) {
        saveConfig(getConfig().copy(enableAdvancedDiagnostics = enabled))
    }
    
    fun updateSpaceTheme(enabled: Boolean) {
        saveConfig(getConfig().copy(spaceThemeEnabled = enabled))
    }
    
    fun updateAutoReconnect(enabled: Boolean) {
        saveConfig(getConfig().copy(autoReconnect = enabled))
    }

    fun updateOfflineEnabled(enabled: Boolean) {
        saveConfig(getConfig().copy(offlineEnabled = enabled))
    }
    
    fun updateLogLevel(level: LogLevel) {
        saveConfig(getConfig().copy(logLevel = level))
    }
    
    fun updatePreferredProtocol(protocol: String) {
        val validProtocols = listOf("AUTO", "ISO9141-2", "ISO14230-4", "ISO15765-4", "J1850PWM", "J1850VPW")
        if (protocol in validProtocols) {
            saveConfig(getConfig().copy(preferredProtocol = protocol))
        }
    }
    
    fun updateUnits(units: Units) {
        saveConfig(getConfig().copy(units = units))
    }
    
    /**
     * Reset to default configuration
     */
    fun resetToDefaults() {
        saveConfig(AppConfig())
        android.util.Log.i(TAG, "Configuration reset to defaults")
    }
    
    /**
     * Export configuration as JSON string
     */
    fun exportConfig(): String {
        return try {
            json.encodeToString(getConfig())
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to export configuration: ${e.message}")
            ""
        }
    }
    
    /**
     * Import configuration from JSON string
     */
    fun importConfig(configJson: String): Boolean {
        return try {
            val importedConfig = json.decodeFromString<AppConfig>(configJson)
            saveConfig(importedConfig)
            android.util.Log.i(TAG, "Configuration imported successfully")
            true
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to import configuration: ${e.message}")
            false
        }
    }
    
    /**
     * Get configuration for specific feature
     */
    fun getDiagnosticConfig(): DiagnosticConfig {
        val config = getConfig()
        return DiagnosticConfig(
            connectionTimeout = config.connectionTimeout,
            maxRetries = config.maxRetries,
            autoReconnect = config.autoReconnect,
            preferredProtocol = config.preferredProtocol,
            enableAdvancedDiagnostics = config.enableAdvancedDiagnostics
        )
    }
    
    fun getUIConfig(): UIConfig {
        val config = getConfig()
        return UIConfig(
            spaceThemeEnabled = config.spaceThemeEnabled,
            units = config.units,
            pollingInterval = config.pidPollingInterval
        )
    }
    
    fun getLoggingConfig(): LoggingConfig {
        val config = getConfig()
        return LoggingConfig(
            enableDataLogging = config.enableDataLogging,
            logLevel = config.logLevel,
            maxLogFileSize = config.maxLogFileSize
        )
    }
    
    fun loadConfig(): AppConfig {
        return try {
            val configJson = prefs.getString(CONFIG_KEY, null)
            if (configJson != null) {
                json.decodeFromString<AppConfig>(configJson)
            } else {
                AppConfig() // Return default config
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Failed to load configuration, using defaults: ${e.message}")
            AppConfig() // Return default config on error
        }
    }
    
    private fun validateConfig(config: AppConfig): AppConfig {
        return config.copy(
            connectionTimeout = config.connectionTimeout.coerceIn(1000, 30000),
            pidPollingInterval = config.pidPollingInterval.coerceIn(100, 5000),
            maxRetries = config.maxRetries.coerceIn(1, 10),
            maxLogFileSize = config.maxLogFileSize.coerceIn(1024 * 1024, 100 * 1024 * 1024) // 1MB to 100MB
        )
    }
}

/**
 * Configuration subset for diagnostic features
 */
data class DiagnosticConfig(
    val connectionTimeout: Long,
    val maxRetries: Int,
    val autoReconnect: Boolean,
    val preferredProtocol: String,
    val enableAdvancedDiagnostics: Boolean
)

/**
 * Configuration subset for UI features
 */
data class UIConfig(
    val spaceThemeEnabled: Boolean,
    val units: Units,
    val pollingInterval: Long
)

/**
 * Configuration subset for logging features
 */
data class LoggingConfig(
    val enableDataLogging: Boolean,
    val logLevel: LogLevel,
    val maxLogFileSize: Long
)

/**
 * Configuration validation extensions
 */
fun AppConfig.isValid(): Boolean {
    return connectionTimeout in 1000..30000 &&
           pidPollingInterval in 100..5000 &&
           maxRetries in 1..10 &&
           maxLogFileSize in (1024 * 1024)..(100 * 1024 * 1024)
}

fun AppConfig.getDisplayName(): String {
    return when {
        spaceThemeEnabled -> "🚀 Space Explorer Config"
        else -> "🔧 Standard Config"
    }
}
