package com.silentpdf.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silentpdf.app.bionic.BionicConfig
import com.silentpdf.app.bionic.ProcessedBionicPage

@Composable
fun BionicText(
    processedPage: ProcessedBionicPage?,
    fallbackText: String,
    config: BionicConfig,
    textColor: Color,
    modifier: Modifier = Modifier,
    onOpenSettings: (() -> Unit)? = null
) {
    val layoutDir = if (processedPage?.textDirection == TextDirection.ContentOrRtl) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
        Column(modifier = modifier.fillMaxWidth()) {
            // Status Info Banner for AI Language & OCR details
            if (processedPage != null && config.isEnabled) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF2F80ED).copy(alpha = 0.12f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF2F80ED),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AI Bionic: ${processedPage.detectedLanguage.displayName}" +
                                        if (processedPage.isOcrUsed) " • OCR" else "",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2F80ED)
                            )
                        }

                        if (onOpenSettings != null) {
                            TextButton(
                                onClick = onOpenSettings,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Text("Settings", fontSize = 11.sp, color = Color(0xFF2F80ED))
                            }
                        }
                    }
                }
            }

            // Bionic Text Output
            val annotated = processedPage?.annotatedText
            if (annotated != null) {
                Text(
                    text = annotated,
                    fontSize = 18.sp,
                    lineHeight = 28.sp,
                    color = textColor,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = fallbackText.ifEmpty { "Extracting text..." },
                    fontSize = 18.sp,
                    lineHeight = 28.sp,
                    color = textColor,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
