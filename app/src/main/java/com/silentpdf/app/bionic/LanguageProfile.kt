package com.silentpdf.app.bionic

import kotlin.math.ceil
import kotlin.math.roundToInt

interface LanguageProfile {
    val language: BionicLanguage
    val script: ScriptType

    fun calculateFocusLength(word: String, focusRatio: Float, isQualityMode: Boolean): Int {
        val cleanLen = word.count { it.isLetterOrDigit() }
        if (cleanLen == 0) return 0
        if (cleanLen <= 3) return 1

        val rawFocus = cleanLen * focusRatio
        return if (isQualityMode) {
            when {
                cleanLen in 4..6 -> ceil(rawFocus).toInt().coerceIn(1, cleanLen)
                cleanLen in 7..10 -> (rawFocus * 0.9f).roundToInt().coerceIn(2, cleanLen)
                else -> (rawFocus * 0.8f).roundToInt().coerceIn(3, cleanLen - 1)
            }
        } else {
            ceil(rawFocus).toInt().coerceAtLeast(1)
        }
    }

    fun calculateEmphasisRange(word: String, focusLength: Int, isQualityMode: Boolean): IntRange {
        if (word.isEmpty() || focusLength <= 0) return IntRange.EMPTY
        var count = 0
        var endIndex = 0
        for (i in word.indices) {
            if (word[i].isLetterOrDigit()) {
                count++
            }
            if (count == focusLength) {
                endIndex = i + 1
                break
            }
        }
        if (endIndex == 0) endIndex = word.length.coerceAtMost(focusLength)
        return 0 until endIndex
    }
}

class DefaultLatinProfile(override val language: BionicLanguage) : LanguageProfile {
    override val script: ScriptType = ScriptType.LATIN
}

class SomaliProfile : LanguageProfile {
    override val language: BionicLanguage = BionicLanguage.SOMALI
    override val script: ScriptType = ScriptType.LATIN

    override fun calculateFocusLength(word: String, focusRatio: Float, isQualityMode: Boolean): Int {
        val cleanLen = word.count { it.isLetterOrDigit() }
        if (cleanLen <= 3) return 1
        val base = ceil(cleanLen * focusRatio).toInt()
        // Somali digraph awareness (dh, kh, sh, ny) - if boundary splits digraph, extend by 1
        return base.coerceIn(1, cleanLen)
    }

    override fun calculateEmphasisRange(word: String, focusLength: Int, isQualityMode: Boolean): IntRange {
        val baseRange = super.calculateEmphasisRange(word, focusLength, isQualityMode)
        if (baseRange.isEmpty() || baseRange.last >= word.length - 1) return baseRange

        val nextChar = word.getOrNull(baseRange.last + 1)?.lowercaseChar()
        val currChar = word.getOrNull(baseRange.last)?.lowercaseChar()
        
        // Handle Somali digraphs (dh, kh, sh, ny)
        if (currChar in listOf('d', 'k', 's', 'n') && nextChar == 'h') {
            return 0..(baseRange.last + 1)
        }
        return baseRange
    }
}

class ArabicProfile : LanguageProfile {
    override val language: BionicLanguage = BionicLanguage.ARABIC
    override val script: ScriptType = ScriptType.ARABIC

    override fun calculateFocusLength(word: String, focusRatio: Float, isQualityMode: Boolean): Int {
        // Exclude diacritics (tashkeel 0x064B..0x0652) when counting letters
        val letterCount = word.count { it.code !in 0x064B..0x0652 && it.isLetterOrDigit() }
        if (letterCount == 0) return 0
        if (letterCount <= 3) return 1

        var focus = ceil(letterCount * focusRatio).toInt()
        
        // Quality mode: If word starts with "ال" (Al- article), include the article + first root letter
        if (isQualityMode && word.startsWith("ال")) {
            focus = (focus + 1).coerceAtMost(letterCount)
        }
        return focus.coerceIn(1, letterCount)
    }

    override fun calculateEmphasisRange(word: String, focusLength: Int, isQualityMode: Boolean): IntRange {
        if (word.isEmpty() || focusLength <= 0) return IntRange.EMPTY
        var letterSeen = 0
        var endIndex = 0
        for (i in word.indices) {
            val code = word[i].code
            // Don't count diacritics towards focusLength limit, but keep them attached in the highlight range
            if (code !in 0x064B..0x0652 && word[i].isLetterOrDigit()) {
                letterSeen++
            }
            if (letterSeen == focusLength) {
                // Include trailing diacritics on the last focused character
                var j = i + 1
                while (j < word.length && word[j].code in 0x064B..0x0652) {
                    j++
                }
                endIndex = j
                break
            }
        }
        if (endIndex == 0) endIndex = word.length
        return 0 until endIndex
    }
}

class CjkProfile(override val language: BionicLanguage) : LanguageProfile {
    override val script: ScriptType = ScriptType.CJK

    override fun calculateFocusLength(word: String, focusRatio: Float, isQualityMode: Boolean): Int {
        val len = word.length
        if (len <= 1) return 1
        return if (isQualityMode) {
            when (len) {
                2 -> 1
                3, 4 -> 2
                else -> ceil(len * focusRatio).toInt().coerceIn(1, len - 1)
            }
        } else {
            ceil(len * focusRatio).toInt().coerceIn(1, len)
        }
    }
}

class CyrillicProfile : LanguageProfile {
    override val language: BionicLanguage = BionicLanguage.CYRILLIC
    override val script: ScriptType = ScriptType.CYRILLIC
}

class DevanagariProfile : LanguageProfile {
    override val language: BionicLanguage = BionicLanguage.HINDI
    override val script: ScriptType = ScriptType.DEVANAGARI

    override fun calculateEmphasisRange(word: String, focusLength: Int, isQualityMode: Boolean): IntRange {
        val baseRange = super.calculateEmphasisRange(word, focusLength, isQualityMode)
        if (baseRange.isEmpty() || baseRange.last >= word.length - 1) return baseRange

        var end = baseRange.last
        // Ensure Virama (0x094D / halant) and attached Matras/vowels are included in the cluster
        while (end + 1 < word.length) {
            val nextCode = word[end + 1].code
            if (nextCode == 0x094D || nextCode in 0x093E..0x094C) {
                end++
            } else {
                break
            }
        }
        return 0..end
    }
}

class HebrewProfile : LanguageProfile {
    override val language: BionicLanguage = BionicLanguage.HEBREW
    override val script: ScriptType = ScriptType.HEBREW
}
