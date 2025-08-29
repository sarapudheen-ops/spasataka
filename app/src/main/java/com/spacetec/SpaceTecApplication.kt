package com.spacetec

import android.app.Application
import android.content.Context
import android.os.StrictMode
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.work.Configuration
import com.spacetec.BuildConfig
import com.spacetec.diagnostic.vci.NativeLoader
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class SpaceTecApplication : Application(), Configuration.Provider {
    companion object {
        lateinit var instance: SpaceTecApplication
            private set
    }
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize native libraries
        try {
            NativeLoader.loadAll()
        } catch (e: Exception) {
            Log.e("SpaceTecApplication", "Native library initialization failed: ${e.message}")
        }
        
        // Apply saved theme preference
        applyThemePreference()
        
        // Configure StrictMode for development
        if (BuildConfig.DEBUG) {
            configureStrictMode()
        }
        
        // Initialize application components
        applicationScope.launch {
            initializeComponents()
        }
        
        Log.d("SpaceTecApplication", "SpaceTecApplication initialized")
    }
    
    private fun applyThemePreference() {
        val sharedPreferences = getSharedPreferences("spacetec_prefs", Context.MODE_PRIVATE)
        val isDarkMode = sharedPreferences.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }
        )
        Log.d("SpaceTecApplication", "Applied theme preference: Dark mode = $isDarkMode")
    }
    
    private fun initializeComponents() {
        // Initialize core application components here
        Log.d("SpaceTecApplication", "Components initialized")
    }
    
    private fun configureStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        Log.w("SpaceTecApplication", "Low memory warning received")
    }
    
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when (level) {
            TRIM_MEMORY_UI_HIDDEN -> {
                Log.d("SpaceTecApplication", "UI hidden - trimming memory")
            }
            TRIM_MEMORY_RUNNING_MODERATE,
            TRIM_MEMORY_RUNNING_LOW -> {
                Log.d("SpaceTecApplication", "Running low memory - moderate trim")
            }
            TRIM_MEMORY_RUNNING_CRITICAL -> {
                Log.w("SpaceTecApplication", "Critical memory situation")
            }
            TRIM_MEMORY_BACKGROUND,
            TRIM_MEMORY_MODERATE,
            TRIM_MEMORY_COMPLETE -> {
                Log.d("SpaceTecApplication", "Background memory trim")
            }
        }
    }
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) android.util.Log.DEBUG else android.util.Log.ERROR)
            .build()
}
