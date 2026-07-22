package com.silentpdf.app.bionic

import androidx.compose.ui.text.style.TextDirection

object LanguageDetector {

    data class DetectionResult(
        val language: BionicLanguage,
        val script: ScriptType,
        val textDirection: TextDirection
    )

    fun detect(text: String, preferredLanguage: BionicLanguage = BionicLanguage.AUTO): DetectionResult {
        if (preferredLanguage != BionicLanguage.AUTO) {
            val script = getScriptForLanguage(preferredLanguage)
            val dir = if (preferredLanguage.isRtl) TextDirection.ContentOrRtl else TextDirection.ContentOrLtr
            return DetectionResult(preferredLanguage, script, dir)
        }

        if (text.isBlank()) {
            return DetectionResult(BionicLanguage.ENGLISH, ScriptType.LATIN, TextDirection.ContentOrLtr)
        }

        var arabicCount = 0
        var hebrewCount = 0
        var cyrillicCount = 0
        var cjkCount = 0
        var devanagariCount = 0
        var latinCount = 0
        var totalLetters = 0

        for (char in text) {
            val code = char.code
            when {
                // Arabic
                code in 0x0600..0x06FF || code in 0x0750..0x077F || code in 0x08A0..0x08FF || code in 0xFB50..0xFDFF || code in 0xFE70..0xFEFF -> {
                    arabicCount++
                    totalLetters++
                }
                // Hebrew
                code in 0x0590..0x05FF -> {
                    hebrewCount++
                    totalLetters++
                }
                // Cyrillic
                code in 0x0400..0x04FF || code in 0x0500..0x052F -> {
                    cyrillicCount++
                    totalLetters++
                }
                // CJK (Hanzi, Kana, Hangul)
                code in 0x4E00..0x9FFF || code in 0x3040..0x309F || code in 0x30A0..0x30FF || code in 0xAC00..0xD7AF -> {
                    cjkCount++
                    totalLetters++
                }
                // Devanagari
                code in 0x0900..0x097F -> {
                    devanagariCount++
                    totalLetters++
                }
                // Latin
                (code in 0x0041..0x005A) || (code in 0x0061..0x007A) || (code in 0x00C0..0x024F) || (code in 0x1E00..0x1EFF) -> {
                    latinCount++
                    totalLetters++
                }
            }
            if (totalLetters > 500) break // Sample up to 500 chars for instant performance
        }

        if (totalLetters == 0) {
            return DetectionResult(BionicLanguage.ENGLISH, ScriptType.LATIN, TextDirection.ContentOrLtr)
        }

        return when {
            arabicCount.toFloat() / totalLetters > 0.2f -> {
                val lang = detectArabicScriptSubLanguage(text)
                DetectionResult(lang, ScriptType.ARABIC, TextDirection.ContentOrRtl)
            }
            hebrewCount.toFloat() / totalLetters > 0.2f -> {
                DetectionResult(BionicLanguage.HEBREW, ScriptType.HEBREW, TextDirection.ContentOrRtl)
            }
            cjkCount.toFloat() / totalLetters > 0.15f -> {
                val lang = detectCjkSubLanguage(text)
                DetectionResult(lang, ScriptType.CJK, TextDirection.ContentOrLtr)
            }
            cyrillicCount.toFloat() / totalLetters > 0.2f -> {
                DetectionResult(BionicLanguage.CYRILLIC, ScriptType.CYRILLIC, TextDirection.ContentOrLtr)
            }
            devanagariCount.toFloat() / totalLetters > 0.2f -> {
                DetectionResult(BionicLanguage.HINDI, ScriptType.DEVANAGARI, TextDirection.ContentOrLtr)
            }
            latinCount.toFloat() / totalLetters > 0.3f -> {
                val latinLang = detectLatinSubLanguage(text)
                DetectionResult(latinLang, ScriptType.LATIN, TextDirection.ContentOrLtr)
            }
            else -> DetectionResult(BionicLanguage.ENGLISH, ScriptType.LATIN, TextDirection.ContentOrLtr)
        }
    }

    private fun detectArabicScriptSubLanguage(text: String): BionicLanguage {
        val lower = text.lowercase()
        // Check for Somali words written in Arabic or Somali-specific markers if any, else Arabic
        return if (lower.contains("somali") || lower.contains("soomaali")) BionicLanguage.SOMALI else BionicLanguage.ARABIC
    }

    private fun detectCjkSubLanguage(text: String): BionicLanguage {
        var hiraganaKatakana = 0
        var hangul = 0
        for (char in text) {
            val code = char.code
            if (code in 0x3040..0x30FF) hiraganaKatakana++
            if (code in 0xAC00..0xD7AF) hangul++
        }
        return when {
            hiraganaKatakana > 3 -> BionicLanguage.JAPANESE
            hangul > 3 -> BionicLanguage.KOREAN
            else -> BionicLanguage.CHINESE
        }
    }

    private fun detectLatinSubLanguage(text: String): BionicLanguage {
        val lower = text.lowercase()
        val words = lower.split(Regex("\\s+")).take(100)
        
        var frenchScore = 0
        var germanScore = 0
        var spanishScore = 0
        var somaliScore = 0

        for (w in words) {
            when (w) {
                "le", "la", "les", "des", "une", "est", "dans", "pour", "avec" -> frenchScore++
                "der", "die", "das", "und", "ist", "nicht", "mit", "einen" -> germanScore++
                "el", "las", "los", "una", "del", "como", "para", "con" -> spanishScore++
                "waa", "iyo", "uu", "ay", "in", "ee", "soo", "sida", "mid", "dadka" -> somaliScore++
            }
        }

        return when {
            somaliScore >= 2 -> BionicLanguage.SOMALI
            frenchScore >= 3 -> BionicLanguage.FRENCH
            germanScore >= 3 -> BionicLanguage.GERMAN
            spanishScore >= 3 -> BionicLanguage.SPANISH
            else -> BionicLanguage.ENGLISH
        }
    }

    private fun getScriptForLanguage(language: BionicLanguage): ScriptType {
        return when (language) {
            BionicLanguage.ARABIC -> ScriptType.ARABIC
            BionicLanguage.HEBREW -> ScriptType.HEBREW
            BionicLanguage.CYRILLIC -> ScriptType.CYRILLIC
            BionicLanguage.CHINESE, BionicLanguage.JAPANESE, BionicLanguage.KOREAN -> ScriptType.CJK
            BionicLanguage.HINDI -> ScriptType.DEVANAGARI
            BionicLanguage.ENGLISH, BionicLanguage.SOMALI, BionicLanguage.FRENCH, BionicLanguage.SPANISH, BionicLanguage.GERMAN -> ScriptType.LATIN
            BionicLanguage.AUTO -> ScriptType.LATIN
        }
    }
}
