package com.silentpdf.app.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.silentpdf.app.util.PdfCreator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOptionsSheet(
    onDismiss: () -> Unit,
    onImportPdf: () -> Unit,
    onImagesToPdfClick: () -> Unit,
    onTextToPdfClick: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, top = 8.dp)
        ) {
            Text(
                "Create & Import",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            ListItem(
                headlineContent = { Text("Import existing PDF", fontWeight = FontWeight.Medium) },
                supportingContent = { Text("Browse device storage") },
                leadingContent = { 
                    Box(modifier = Modifier.size(48.dp).background(Color(0xFF2F80ED).copy(alpha=0.1f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.UploadFile, null, tint = Color(0xFF2F80ED)) 
                    }
                },
                modifier = Modifier.clickable { onDismiss(); onImportPdf() },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            
            ListItem(
                headlineContent = { Text("Scan / Images to PDF", fontWeight = FontWeight.Medium) },
                supportingContent = { Text("Select photos to combine into a PDF") },
                leadingContent = { 
                    Box(modifier = Modifier.size(48.dp).background(Color(0xFF00C853).copy(alpha=0.1f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.CameraAlt, null, tint = Color(0xFF00C853)) 
                    }
                },
                modifier = Modifier.clickable { onDismiss(); onImagesToPdfClick() },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            
            ListItem(
                headlineContent = { Text("Text to PDF", fontWeight = FontWeight.Medium) },
                supportingContent = { Text("Write or paste text to generate a PDF") },
                leadingContent = { 
                    Box(modifier = Modifier.size(48.dp).background(Color(0xFFFF9800).copy(alpha=0.1f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Description, null, tint = Color(0xFFFF9800)) 
                    }
                },
                modifier = Modifier.clickable { onDismiss(); onTextToPdfClick() },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
        }
    }
}

@Composable
fun TextToPdfDialog(
    onDismiss: () -> Unit,
    onPdfCreated: (Uri) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var textContent by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                    Text(
                        "Text to PDF",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Button(
                        onClick = {
                            if (title.isBlank() || textContent.isBlank()) {
                                Toast.makeText(context, "Title and content cannot be empty", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isGenerating = true
                            scope.launch {
                                val uri = PdfCreator.createTextPdf(context, textContent, title)
                                isGenerating = false
                                if (uri != null) {
                                    Toast.makeText(context, "PDF generated successfully", Toast.LENGTH_SHORT).show()
                                    onPdfCreated(uri)
                                    onDismiss()
                                } else {
                                    Toast.makeText(context, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !isGenerating && title.isNotBlank() && textContent.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2F80ED))
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Save")
                        }
                    }
                }
                
                // Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Document Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2F80ED),
                            focusedLabelColor = Color(0xFF2F80ED)
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = textContent,
                        onValueChange = { textContent = it },
                        label = { Text("Text Content") },
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(bottom = 24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2F80ED),
                            focusedLabelColor = Color(0xFF2F80ED)
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun ImagesToPdfDialog(
    imageUris: List<Uri>,
    onDismiss: () -> Unit,
    onPdfCreated: (Uri) -> Unit
) {
    var title by remember { mutableStateOf("Scanned_Document") }
    var isGenerating by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save as PDF", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("You have selected ${imageUris.size} image(s).")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Document Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2F80ED),
                        focusedLabelColor = Color(0xFF2F80ED)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        Toast.makeText(context, "Title cannot be empty", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isGenerating = true
                    scope.launch {
                        val uri = PdfCreator.createImagesPdf(context, imageUris, title)
                        isGenerating = false
                        if (uri != null) {
                            Toast.makeText(context, "PDF generated successfully", Toast.LENGTH_SHORT).show()
                            onPdfCreated(uri)
                            onDismiss()
                        } else {
                            Toast.makeText(context, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                enabled = !isGenerating && title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2F80ED))
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Create", color = Color.White)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp)
    )
}
