package com.silentpdf.app.bionic

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withStyle

object HighlightGenerator {

    fun generate(
        tokens: List<TextAnalyzer.Token>,
        profile: LanguageProfile,
        config: BionicConfig,
        direction: TextDirection,
        textColor: Color = Color.Unspecified
    ): AnnotatedString {
        val focusRatio = config.focusRatio
        val isQualityMode = config.performanceMode == BionicPerformanceMode.QUALITY

        return buildAnnotatedString {
            // Apply paragraph style for RTL vs LTR text direction if needed
            if (direction == TextDirection.ContentOrRtl) {
                pushStyle(ParagraphStyle(textDirection = TextDirection.ContentOrRtl))
            }

            for (token in tokens) {
                when (token) {
                    is TextAnalyzer.Token.Word -> {
                        val word = token.text
                        val focusLen = profile.calculateFocusLength(word, focusRatio, isQualityMode)
                        val range = profile.calculateEmphasisRange(word, focusLen, isQualityMode)

                        if (!range.isEmpty() && range.first < word.length) {
                            val boldEnd = (range.last + 1).coerceAtMost(word.length)
                            val boldPart = word.substring(0, boldEnd)
                            val normalPart = word.substring(boldEnd)

                            withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, color = textColor)) {
                                append(boldPart)
                            }
                            if (normalPart.isNotEmpty()) {
                                withStyle(SpanStyle(fontWeight = FontWeight.Normal, color = textColor.copy(alpha = 0.85f))) {
                                    append(normalPart)
                                }
                            }
                        } else {
                            withStyle(SpanStyle(fontWeight = FontWeight.Normal, color = textColor)) {
                                append(word)
                            }
                        }
                    }
                    is TextAnalyzer.Token.Delimiter -> {
                        withStyle(SpanStyle(fontWeight = FontWeight.Normal, color = textColor)) {
                            append(token.text)
                        }
                    }
                }
            }

            if (direction == TextDirection.ContentOrRtl) {
                pop()
            }
        }
    }
}
