package com.silentpdf.app.ui.screens

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import kotlin.math.ceil

@Composable
fun BionicText(text: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    val annotatedString = buildAnnotatedString {
        val words = text.split(Regex("(?<=\\s)|(?=\\s)"))
        for (word in words) {
            if (word.isNotBlank()) {
                val boldLength = ceil(word.length / 2.0).toInt().coerceAtLeast(1)
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = color)) {
                    append(word.substring(0, boldLength))
                }
                withStyle(style = SpanStyle(fontWeight = FontWeight.Normal, color = color)) {
                    append(word.substring(boldLength))
                }
            } else {
                append(word)
            }
        }
    }
    Text(text = annotatedString, modifier = modifier, fontSize = 18.sp, lineHeight = 28.sp)
}
