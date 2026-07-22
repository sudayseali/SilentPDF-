package com.silentpdf.app.bionic

object TextAnalyzer {

    sealed class Token {
        data class Word(val text: String, val cleanText: String) : Token()
        data class Delimiter(val text: String) : Token()
    }

    fun tokenize(text: String, script: ScriptType): List<Token> {
        if (text.isEmpty()) return emptyList()

        if (script == ScriptType.CJK) {
            return tokenizeCjk(text)
        }

        val tokens = mutableListOf<Token>()
        val sb = StringBuilder()
        var inWord = false

        for (char in text) {
            val isLetterOrNum = char.isLetterOrDigit()
            if (isLetterOrNum) {
                if (!inWord && sb.isNotEmpty()) {
                    tokens.add(Token.Delimiter(sb.toString()))
                    sb.clear()
                }
                inWord = true
                sb.append(char)
            } else {
                if (inWord && sb.isNotEmpty()) {
                    val w = sb.toString()
                    tokens.add(Token.Word(w, w))
                    sb.clear()
                }
                inWord = false
                sb.append(char)
            }
        }

        if (sb.isNotEmpty()) {
            if (inWord) {
                val w = sb.toString()
                tokens.add(Token.Word(w, w))
            } else {
                tokens.add(Token.Delimiter(sb.toString()))
            }
        }

        return tokens
    }

    private fun tokenizeCjk(text: String): List<Token> {
        val tokens = mutableListOf<Token>()
        val sbWord = StringBuilder()
        val sbDelim = StringBuilder()

        for (char in text) {
            val code = char.code
            val isCjkChar = (code in 0x4E00..0x9FFF || code in 0x3040..0x309F || code in 0x30A0..0x30FF || code in 0xAC00..0xD7AF)

            if (isCjkChar || char.isLetterOrDigit()) {
                if (sbDelim.isNotEmpty()) {
                    tokens.add(Token.Delimiter(sbDelim.toString()))
                    sbDelim.clear()
                }
                sbWord.append(char)
                // CJK words are typically 2-3 chars, break CJK clusters into 2-char chunks for bionic focus
                if (sbWord.length >= 2) {
                    val w = sbWord.toString()
                    tokens.add(Token.Word(w, w))
                    sbWord.clear()
                }
            } else {
                if (sbWord.isNotEmpty()) {
                    val w = sbWord.toString()
                    tokens.add(Token.Word(w, w))
                    sbWord.clear()
                }
                sbDelim.append(char)
            }
        }

        if (sbWord.isNotEmpty()) {
            val w = sbWord.toString()
            tokens.add(Token.Word(w, w))
        }
        if (sbDelim.isNotEmpty()) {
            tokens.add(Token.Delimiter(sbDelim.toString()))
        }

        return tokens
    }
}
