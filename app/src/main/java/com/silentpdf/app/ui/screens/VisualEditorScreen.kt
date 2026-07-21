package com.silentpdf.app.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.silentpdf.app.data.db.PdfEntity
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.math.roundToInt

@Composable
fun VisualEditorScreen(
    pdf: PdfEntity,
    initialTool: ActiveTool,
    onDismiss: () -> Unit,
    onSaveSuccess: (Uri, File) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var activeTab by remember { mutableStateOf(initialTool) }
    
    // State for text box
    var textValue by remember { mutableStateOf("Tap to edit") }
    var textOffsetX by remember { mutableStateOf(100f) }
    var textOffsetY by remember { mutableStateOf(200f) }
    var isEditingText by remember { mutableStateOf(false) }
    var showTextBox by remember { mutableStateOf(true) }
    
    LaunchedEffect(pdf) {
        withContext(Dispatchers.IO) {
            try {
                val pfd = context.contentResolver.openFileDescriptor(Uri.parse(pdf.uriString), "r")
                if (pfd != null) {
                    val renderer = PdfRenderer(pfd)
                    if (renderer.pageCount > 0) {
                        val page = renderer.openPage(0)
                        val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                        val canvas = android.graphics.Canvas(bitmap)
                        canvas.drawColor(android.graphics.Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()
                        pageBitmap = bitmap
                    }
                    renderer.close()
                    pfd.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            isLoading = false
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                    Text(
                        "Edit Document", 
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                // Save logic
                                val result = savePdfWithText(context, pdf, textValue, textOffsetX, textOffsetY, pageBitmap)
                                if (result != null) {
                                    onSaveSuccess(result.first, result.second)
                                } else {
                                    Toast.makeText(context, "Failed to save", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2F80ED))
                    ) {
                        Text("Done")
                    }
                }
                
                // Editor Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFFEEEEEE)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator()
                    } else if (pageBitmap != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = pageBitmap!!.asImageBitmap(),
                                contentDescription = "PDF Page",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pointerInput(Unit) {
                                        detectTapGestures { offset ->
                                            if (!isEditingText) {
                                                textOffsetX = offset.x
                                                textOffsetY = offset.y
                                                showTextBox = true
                                            }
                                        }
                                    }
                            )
                            
                            if (showTextBox) {
                                Box(
                                    modifier = Modifier
                                        .offset { IntOffset(textOffsetX.roundToInt(), textOffsetY.roundToInt()) }
                                        .pointerInput(Unit) {
                                            detectDragGestures { change, dragAmount ->
                                                change.consume()
                                                textOffsetX += dragAmount.x
                                                textOffsetY += dragAmount.y
                                            }
                                        }
                                        .border(2.dp, Color(0xFF2F80ED), RoundedCornerShape(4.dp))
                                        .background(Color.White.copy(alpha = 0.8f))
                                        .padding(8.dp)
                                ) {
                                    if (isEditingText) {
                                        OutlinedTextField(
                                            value = textValue,
                                            onValueChange = { textValue = it },
                                            modifier = Modifier.width(200.dp),
                                            trailingIcon = {
                                                IconButton(onClick = { isEditingText = false }) {
                                                    Icon(Icons.Default.Check, "Done")
                                                }
                                            }
                                        )
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = textValue.ifEmpty { "Edit Text" },
                                                color = Color.Black,
                                                modifier = Modifier.clickable { isEditingText = true }
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            IconButton(
                                                onClick = { showTextBox = false },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, "Delete", tint = Color.Red, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Bottom Toolbar (WPS Style)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    BottomToolbarItem(
                        icon = Icons.Default.TextFields,
                        label = "Edit",
                        isSelected = activeTab == ActiveTool.EditText || activeTab == ActiveTool.AddText,
                        onClick = { activeTab = ActiveTool.EditText; showTextBox = true }
                    )
                    BottomToolbarItem(
                        icon = Icons.Default.Edit,
                        label = "Annotate",
                        isSelected = activeTab == ActiveTool.Print,
                        onClick = { activeTab = ActiveTool.Print }
                    )
                    BottomToolbarItem(
                        icon = Icons.Default.BorderColor,
                        label = "Sign",
                        isSelected = activeTab == ActiveTool.Sign,
                        onClick = { activeTab = ActiveTool.Sign }
                    )
                }
            }
        }
    }
}

@Composable
fun BottomToolbarItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = if (isSelected) Color(0xFF2F80ED) else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Icon(icon, contentDescription = label, tint = color)
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = color, style = MaterialTheme.typography.labelSmall)
    }
}

suspend fun savePdfWithText(
    context: Context, 
    pdf: PdfEntity, 
    text: String, 
    x: Float, 
    y: Float,
    bitmap: Bitmap?
): Pair<Uri, File>? = withContext(Dispatchers.IO) {
    if (bitmap == null || text.isBlank()) return@withContext null
    try {
        val pfd = context.contentResolver.openFileDescriptor(Uri.parse(pdf.uriString), "r") ?: return@withContext null
        val document = PDDocument.load(FileInputStream(pfd.fileDescriptor))
        
        if (document.numberOfPages > 0) {
            val page = document.getPage(0)
            val contentStream = PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true)
            
            // Map UI coordinates to PDF coordinates
            // This is a rough estimation. PDF coordinates start from bottom-left
            val pdfWidth = page.cropBox.width
            val pdfHeight = page.cropBox.height
            
            // Assuming image is scaled to fit width in UI
            // Let's do a simple mapping
            val mappedX = (x / bitmap.width) * pdfWidth * 2f // Adjust for scaling
            val mappedY = pdfHeight - ((y / bitmap.height) * pdfHeight * 2f) - 20f
            
            contentStream.beginText()
            contentStream.setFont(PDType1Font.HELVETICA, 14f)
            contentStream.newLineAtOffset(mappedX.coerceIn(0f, pdfWidth), mappedY.coerceIn(0f, pdfHeight))
            contentStream.showText(text)
            contentStream.endText()
            contentStream.close()
        }
        
        val docsFolder = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "SilentPDF")
        if (!docsFolder.exists()) docsFolder.mkdirs()
        
        val outFile = File(docsFolder, "${pdf.fileName.substringBeforeLast(".")}_edited.pdf")
        document.save(outFile)
        document.close()
        pfd.close()
        
        return@withContext Pair(Uri.fromFile(outFile), outFile)
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext null
    }
}
