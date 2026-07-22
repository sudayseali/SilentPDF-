with open("app/src/main/java/com/silentpdf/app/ui/viewmodel/SilentPdfViewModel.kt", "r") as f:
    content = f.read()

content = content.replace(
    'private val _isTrueDarkMode = MutableStateFlow(viewSettingsPrefs.getBoolean("true_dark_mode", false))\n    val isTrueDarkMode: StateFlow<Boolean> = _isTrueDarkMode',
    'private val _isTrueDarkMode = MutableStateFlow(viewSettingsPrefs.getBoolean("true_dark_mode", false))\n    val isTrueDarkMode: StateFlow<Boolean> = _isTrueDarkMode\n\n    private val _isAppDarkMode = MutableStateFlow(viewSettingsPrefs.getBoolean("app_dark_mode", false))\n    val isAppDarkMode: StateFlow<Boolean> = _isAppDarkMode'
)

content = content.replace(
    'fun toggleTrueDarkMode() {\n        val newValue = !_isTrueDarkMode.value\n        _isTrueDarkMode.value = newValue\n        viewSettingsPrefs.edit().putBoolean("true_dark_mode", newValue).apply()\n    }',
    'fun toggleTrueDarkMode() {\n        val newValue = !_isTrueDarkMode.value\n        _isTrueDarkMode.value = newValue\n        viewSettingsPrefs.edit().putBoolean("true_dark_mode", newValue).apply()\n    }\n\n    fun toggleAppDarkMode() {\n        val newValue = !_isAppDarkMode.value\n        _isAppDarkMode.value = newValue\n        viewSettingsPrefs.edit().putBoolean("app_dark_mode", newValue).apply()\n    }'
)

with open("app/src/main/java/com/silentpdf/app/ui/viewmodel/SilentPdfViewModel.kt", "w") as f:
    f.write(content)
