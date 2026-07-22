package com.silentpdf.app.bionic

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextDirection

enum class BionicIntensity(val defaultPercentage: Float, val displayName: String) {
    LOW(0.30f, "Low (30%)"),
    MEDIUM(0.50f, "Medium (50%)"),
    HIGH(0.70f, "High (70%)"),
    CUSTOM(0.50f, "Custom")
}

enum class BionicLanguage(val code: String, val displayName: String, val isRtl: Boolean = false) {
    AUTO("auto", "Auto Detect"),
    ENGLISH("en", "English"),
    ARABIC("ar", "Arabic (العربية)", isRtl = true),
    SOMALI("so", "Somali (Soomaali)"),
    FRENCH("fr", "French (Français)"),
    SPANISH("es", "Spanish (Español)"),
    GERMAN("de", "German (Deutsch)"),
    CHINESE("zh", "Chinese (中文)"),
    JAPANESE("ja", "Japanese (日本語)"),
    KOREAN("ko", "Korean (한국어)"),
    HINDI("hi", "Hindi (हिन्दी)"),
    CYRILLIC("ru", "Cyrillic (Русский/Українська)"),
    HEBREW("he", "Hebrew (עברית)", isRtl = true)
}

enum class BionicPerformanceMode(val displayName: String) {
    FAST("Fast Mode (Simplified)"),
    QUALITY("Quality Mode (Linguistic Rules)")
}

enum class ScriptType {
    LATIN,
    ARABIC,
    CYRILLIC,
    CJK,
    DEVANAGARI,
    HEBREW,
    OTHER
}

data class BionicConfig(
    val isEnabled: Boolean = false,
    val intensity: BionicIntensity = BionicIntensity.MEDIUM,
    val customIntensityPercentage: Float = 0.50f,
    val language: BionicLanguage = BionicLanguage.AUTO,
    val performanceMode: BionicPerformanceMode = BionicPerformanceMode.QUALITY,
    val autoOcrForScanned: Boolean = true
) {
    val focusRatio: Float
        get() = if (intensity == BionicIntensity.CUSTOM) customIntensityPercentage else intensity.defaultPercentage

    fun toCacheKey(): String {
        return "${isEnabled}_${intensity.name}_${customIntensityPercentage}_${language.name}_${performanceMode.name}_${autoOcrForScanned}"
    }
}

data class ProcessedBionicPage(
    val pageIndex: Int,
    val annotatedText: AnnotatedString,
    val rawText: String,
    val detectedLanguage: BionicLanguage,
    val scriptType: ScriptType,
    val textDirection: TextDirection,
    val isOcrUsed: Boolean,
    val processingTimeMs: Long
)
