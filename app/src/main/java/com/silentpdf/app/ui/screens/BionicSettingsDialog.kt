package com.silentpdf.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silentpdf.app.bionic.BionicConfig
import com.silentpdf.app.bionic.BionicIntensity
import com.silentpdf.app.bionic.BionicLanguage
import com.silentpdf.app.bionic.BionicPerformanceMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BionicSettingsDialog(
    config: BionicConfig,
    onDismissRequest: () -> Unit,
    onConfigChanged: (BionicConfig) -> Unit
) {
    var isEnabled by remember { mutableStateOf(config.isEnabled) }
    var selectedIntensity by remember { mutableStateOf(config.intensity) }
    var customPercentage by remember { mutableFloatStateOf(config.customIntensityPercentage) }
    var selectedLanguage by remember { mutableStateOf(config.language) }
    var selectedPerformanceMode by remember { mutableStateOf(config.performanceMode) }
    var autoOcr by remember { mutableStateOf(config.autoOcrForScanned) }

    var languageDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            Button(
                onClick = {
                    onConfigChanged(
                        BionicConfig(
                            isEnabled = isEnabled,
                            intensity = selectedIntensity,
                            customIntensityPercentage = customPercentage,
                            language = selectedLanguage,
                            performanceMode = selectedPerformanceMode,
                            autoOcrForScanned = autoOcr
                        )
                    )
                    onDismissRequest()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2F80ED)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Apply Settings", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFF2F80ED),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI Bionic Engine",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp)
            ) {
                // Enable/Disable Toggle
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable Bionic Reading",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Highlights focus letters for faster reading",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { isEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF2F80ED))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Focus Intensity
                Text(
                    text = "Focus Intensity",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BionicIntensity.values().forEach { intensity ->
                        FilterChip(
                            selected = selectedIntensity == intensity,
                            onClick = { selectedIntensity = intensity },
                            label = { Text(intensity.displayName, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF2F80ED),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                if (selectedIntensity == BionicIntensity.CUSTOM) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${(customPercentage * 100).toInt()}% Focus",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2F80ED),
                            modifier = Modifier.width(75.dp)
                        )
                        Slider(
                            value = customPercentage,
                            onValueChange = { customPercentage = it },
                            valueRange = 0.20f..0.80f,
                            steps = 11,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF2F80ED),
                                activeTrackColor = Color(0xFF2F80ED)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Language Selection
                Text(
                    text = "Language Rules Engine",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = languageDropdownExpanded,
                    onExpandedChange = { languageDropdownExpanded = !languageDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedLanguage.displayName,
                        onValueChange = {},
                        readOnly = true,
                        leadingIcon = {
                            Icon(Icons.Default.Language, contentDescription = null, tint = Color(0xFF2F80ED))
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = languageDropdownExpanded,
                        onDismissRequest = { languageDropdownExpanded = false }
                    ) {
                        BionicLanguage.values().forEach { lang ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(lang.displayName, fontWeight = if (lang == selectedLanguage) FontWeight.Bold else FontWeight.Normal)
                                        if (lang.isRtl) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = Color(0xFFFF9500).copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    "RTL",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFFF9500),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                },
                                onClick = {
                                    selectedLanguage = lang
                                    languageDropdownExpanded = false
                                },
                                leadingIcon = {
                                    if (lang == selectedLanguage) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF2F80ED))
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Performance & Engine Quality Mode
                Text(
                    text = "Processing Engine Mode",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BionicPerformanceMode.values().forEach { mode ->
                        val isSelected = selectedPerformanceMode == mode
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color(0xFF2F80ED).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF2F80ED)) else null,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedPerformanceMode = mode }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = if (isSelected) Color(0xFF2F80ED) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = mode.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color(0xFF2F80ED) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Auto OCR Switch
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto OCR for Scanned Pages",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Automatically extracts text from image-only PDFs",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoOcr,
                            onCheckedChange = { autoOcr = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF2F80ED))
                        )
                    }
                }
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
