package com.silentpdf.app.ui.viewmodel.controllers

import android.content.Context
import android.content.SharedPreferences
import com.silentpdf.app.bionic.BionicConfig
import com.silentpdf.app.bionic.BionicIntensity
import com.silentpdf.app.bionic.BionicLanguage
import com.silentpdf.app.bionic.BionicPerformanceMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsController(context: Context) {
    private val viewSettingsPrefs: SharedPreferences = context.getSharedPreferences("app_view_settings", Context.MODE_PRIVATE)
    private val bionicPrefs: SharedPreferences = context.getSharedPreferences("app_bionic_settings", Context.MODE_PRIVATE)

    private val _isTrueDarkMode = MutableStateFlow(viewSettingsPrefs.getBoolean("true_dark_mode", false))
    val isTrueDarkMode: StateFlow<Boolean> = _isTrueDarkMode

    private val _isAppDarkMode = MutableStateFlow(viewSettingsPrefs.getBoolean("app_dark_mode", false))
    val isAppDarkMode: StateFlow<Boolean> = _isAppDarkMode

    private val _isGridView = MutableStateFlow(viewSettingsPrefs.getBoolean("grid_view", false))
    val isGridView: StateFlow<Boolean> = _isGridView

    private val _isHorizontalScroll = MutableStateFlow(viewSettingsPrefs.getBoolean("horizontal_scroll", false))
    val isHorizontalScroll: StateFlow<Boolean> = _isHorizontalScroll

    private val _bionicConfig = MutableStateFlow(
        BionicConfig(
            isEnabled = bionicPrefs.getBoolean("is_enabled", false),
            intensity = try { BionicIntensity.valueOf(bionicPrefs.getString("intensity", "MEDIUM") ?: "MEDIUM") } catch (e: Exception) { BionicIntensity.MEDIUM },
            customIntensityPercentage = bionicPrefs.getFloat("custom_percentage", 0.50f),
            language = try { BionicLanguage.valueOf(bionicPrefs.getString("language", "AUTO") ?: "AUTO") } catch (e: Exception) { BionicLanguage.AUTO },
            performanceMode = try { BionicPerformanceMode.valueOf(bionicPrefs.getString("performance_mode", "QUALITY") ?: "QUALITY") } catch (e: Exception) { BionicPerformanceMode.QUALITY },
            autoOcrForScanned = bionicPrefs.getBoolean("auto_ocr", true)
        )
    )
    val bionicConfig: StateFlow<BionicConfig> = _bionicConfig

    fun updateBionicConfig(
        isEnabled: Boolean = _bionicConfig.value.isEnabled,
        intensity: BionicIntensity = _bionicConfig.value.intensity,
        customPercentage: Float = _bionicConfig.value.customIntensityPercentage,
        language: BionicLanguage = _bionicConfig.value.language,
        performanceMode: BionicPerformanceMode = _bionicConfig.value.performanceMode,
        autoOcrForScanned: Boolean = _bionicConfig.value.autoOcrForScanned
    ) {
        val newConfig = BionicConfig(
            isEnabled = isEnabled,
            intensity = intensity,
            customIntensityPercentage = customPercentage,
            language = language,
            performanceMode = performanceMode,
            autoOcrForScanned = autoOcrForScanned
        )
        _bionicConfig.value = newConfig
        bionicPrefs.edit()
            .putBoolean("is_enabled", isEnabled)
            .putString("intensity", intensity.name)
            .putFloat("custom_percentage", customPercentage)
            .putString("language", language.name)
            .putString("performance_mode", performanceMode.name)
            .putBoolean("auto_ocr", autoOcrForScanned)
            .apply()
    }
    
    fun toggleTrueDarkMode() {
        val newValue = !_isTrueDarkMode.value
        _isTrueDarkMode.value = newValue
        viewSettingsPrefs.edit().putBoolean("true_dark_mode", newValue).apply()
    }

    fun toggleAppDarkMode() {
        val newValue = !_isAppDarkMode.value
        _isAppDarkMode.value = newValue
        viewSettingsPrefs.edit().putBoolean("app_dark_mode", newValue).apply()
    }

    fun toggleGridView() {
        val newValue = !_isGridView.value
        _isGridView.value = newValue
        viewSettingsPrefs.edit().putBoolean("grid_view", newValue).apply()
    }

    fun toggleHorizontalScroll() {
        val newValue = !_isHorizontalScroll.value
        _isHorizontalScroll.value = newValue
        viewSettingsPrefs.edit().putBoolean("horizontal_scroll", newValue).apply()
    }
}
