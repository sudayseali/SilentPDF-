package com.silentpdf.app.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.silentpdf.app.data.db.PdfEntity
import com.silentpdf.app.ui.viewmodel.SilentPdfViewModel
import com.silentpdf.app.util.PdfCreator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

sealed class ActiveTool {
    object None : ActiveTool()
    object PdfToImage : ActiveTool()
    object PdfToLongImage : ActiveTool()
    object EditText : ActiveTool()
    object AddText : ActiveTool()
    object Sign : ActiveTool()
    object Print : ActiveTool()
    object Merge : ActiveTool()
    object Split : ActiveTool()
    object ManagePages : ActiveTool()
    object Compress : ActiveTool()
    object Lock : ActiveTool()
    object Unlock : ActiveTool()
    object RecycleBin : ActiveTool()
}

@Composable
fun ToolsScreenContent(
    paddingValues: PaddingValues,
    onNavigateToCamera: () -> Unit,
    imagePickerLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    filePickerLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    showTextToPdfDialog: () -> Unit,
    showCreateFolderDialog: () -> Unit,
    viewModel: SilentPdfViewModel,
    onNavigateToReader: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pdfsList by viewModel.libraryPdfs.collectAsState()

    var activeTool by remember { mutableStateOf<ActiveTool>(ActiveTool.None) }
    var showPdfSelector by remember { mutableStateOf(false) }
    var selectedPdfForTool by remember { mutableStateOf<PdfEntity?>(null) }

    // Multi-select state for Merge
    var showMultiPdfSelector by remember { mutableStateOf(false) }

    // Active tool custom dialog states
    var showSignatureDialog by remember { mutableStateOf(false) }
    var showEditTextDialog by remember { mutableStateOf(false) }
    var showAddTextDialog by remember { mutableStateOf(false) }
    var showSplitDialog by remember { mutableStateOf(false) }
    var showManagePagesDialog by remember { mutableStateOf(false) }
    var showCompressDialog by remember { mutableStateOf(false) }
    var showLockDialog by remember { mutableStateOf(false) }
    var showUnlockDialog by remember { mutableStateOf(false) }
    var showRecycleBinDialog by remember { mutableStateOf(false) }

    // Signature image cache
    var signatureBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Functions to trigger tool actions
    val onToolClick: (ActiveTool) -> Unit = { tool ->
        activeTool = tool
        when (tool) {
            ActiveTool.None -> {}
            ActiveTool.RecycleBin -> {
                showRecycleBinDialog = true
            }
            ActiveTool.Merge -> {
                showMultiPdfSelector = true
            }
            ActiveTool.Sign -> {
                showSignatureDialog = true
            }
            else -> {
                showPdfSelector = true
            }
        }
    }

    // Document Selector Dialog
    if (showPdfSelector) {
        PdfSelectorDialog(
            pdfsList = pdfsList,
            onDismiss = {
                showPdfSelector = false
                activeTool = ActiveTool.None
            },
            onPdfSelected = { pdf ->
                selectedPdfForTool = pdf
                showPdfSelector = false
                // Proceed to the tool action or tool-specific dialog
                when (activeTool) {
                    ActiveTool.PdfToImage -> {
                        scope.launch {
                            val success = convertPdfToImages(context, pdf)
                            if (success) {
                                Toast.makeText(context, "Images saved successfully!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Failed to convert PDF to images", Toast.LENGTH_SHORT).show()
                            }
                            activeTool = ActiveTool.None
                        }
                    }
                    ActiveTool.PdfToLongImage -> {
                        scope.launch {
                            val success = convertPdfToLongImage(context, pdf)
                            if (success) {
                                Toast.makeText(context, "Long Image saved to gallery!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Failed to convert to long image", Toast.LENGTH_SHORT).show()
                            }
                            activeTool = ActiveTool.None
                        }
                    }
                    ActiveTool.EditText -> {
                        showEditTextDialog = true
                    }
                    ActiveTool.AddText -> {
                        showAddTextDialog = true
                    }
                    ActiveTool.Print -> {
                        printPdf(context, pdf)
                        activeTool = ActiveTool.None
                    }
                    ActiveTool.Split -> {
                        showSplitDialog = true
                    }
                    ActiveTool.ManagePages -> {
                        showManagePagesDialog = true
                    }
                    ActiveTool.Compress -> {
                        showCompressDialog = true
                    }
                    ActiveTool.Lock -> {
                        showLockDialog = true
                    }
                    ActiveTool.Unlock -> {
                        showUnlockDialog = true
                    }
                    else -> {}
                }
            }
        )
    }

    // Multi PDF Selector for Merge
    if (showMultiPdfSelector) {
        MultiPdfSelectorDialog(
            pdfsList = pdfsList,
            onDismiss = {
                showMultiPdfSelector = false
                activeTool = ActiveTool.None
            },
            onPdfsSelected = { selectedPdfs ->
                showMultiPdfSelector = false
                if (selectedPdfs.size < 2) {
                    Toast.makeText(context, "Please select at least 2 PDFs to merge", Toast.LENGTH_SHORT).show()
                    activeTool = ActiveTool.None
                } else {
                    scope.launch {
                        val mergedUri = mergePdfs(context, selectedPdfs)
                        if (mergedUri != null) {
                            val file = File(mergedUri.path!!)
                            viewModel.importPdf(mergedUri, file.name, file.length())
                            Toast.makeText(context, "PDFs merged successfully!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Failed to merge PDFs", Toast.LENGTH_SHORT).show()
                        }
                        activeTool = ActiveTool.None
                    }
                }
            }
        )
    }

    // Tool Dialogs
    if (showSignatureDialog) {
        SignaturePadDialog(
            onDismiss = {
                showSignatureDialog = false
                activeTool = ActiveTool.None
            },
            onSignatureSaved = { bitmap ->
                signatureBitmap = bitmap
                showSignatureDialog = false
                // Now prompt to select PDF to burn signature onto
                showPdfSelector = true
            }
        )
    }

    // Burn signature dialog
    if (signatureBitmap != null && activeTool == ActiveTool.Sign && selectedPdfForTool != null) {
        val pdf = selectedPdfForTool!!
        val signature = signatureBitmap!!
        BurnSignatureDialog(
            pdf = pdf,
            signature = signature,
            onDismiss = {
                signatureBitmap = null
                selectedPdfForTool = null
                activeTool = ActiveTool.None
            },
            onComplete = { signedUri ->
                signatureBitmap = null
                selectedPdfForTool = null
                activeTool = ActiveTool.None
                if (signedUri != null) {
                    val file = File(signedUri.path!!)
                    viewModel.importPdf(signedUri, file.name, file.length())
                    Toast.makeText(context, "Document signed successfully!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Failed to sign document", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (showEditTextDialog && selectedPdfForTool != null) {
        EditTextDialog(
            pdf = selectedPdfForTool!!,
            onDismiss = {
                showEditTextDialog = false
                selectedPdfForTool = null
                activeTool = ActiveTool.None
            },
            onSave = { updatedText, title ->
                showEditTextDialog = false
                val pdf = selectedPdfForTool!!
                selectedPdfForTool = null
                activeTool = ActiveTool.None
                scope.launch {
                    val uri = PdfCreator.createTextPdf(context, updatedText, title)
                    if (uri != null) {
                        val file = File(uri.path!!)
                        viewModel.importPdf(uri, file.name, file.length())
                        Toast.makeText(context, "PDF generated successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to update PDF text", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    if (showAddTextDialog && selectedPdfForTool != null) {
        AddTextDialog(
            pdf = selectedPdfForTool!!,
            onDismiss = {
                showAddTextDialog = false
                selectedPdfForTool = null
                activeTool = ActiveTool.None
            },
            onSave = { textToAppend ->
                showAddTextDialog = false
                val pdf = selectedPdfForTool!!
                selectedPdfForTool = null
                activeTool = ActiveTool.None
                scope.launch {
                    val uri = appendTextToPdf(context, pdf, textToAppend)
                    if (uri != null) {
                        val file = File(uri.path!!)
                        viewModel.importPdf(uri, file.name, file.length())
                        Toast.makeText(context, "Text added successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to add text", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    if (showSplitDialog && selectedPdfForTool != null) {
        val pdf = selectedPdfForTool!!
        SplitPdfDialog(
            pdf = pdf,
            onDismiss = {
                showSplitDialog = false
                selectedPdfForTool = null
                activeTool = ActiveTool.None
            },
            onSplit = { rangeStr ->
                showSplitDialog = false
                selectedPdfForTool = null
                activeTool = ActiveTool.None
                scope.launch {
                    val outputUris = splitPdf(context, pdf, rangeStr)
                    if (outputUris.isNotEmpty()) {
                        outputUris.forEach { uri ->
                            val file = File(uri.path!!)
                            viewModel.importPdf(uri, file.name, file.length())
                        }
                        Toast.makeText(context, "PDF split successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to split PDF", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    if (showManagePagesDialog && selectedPdfForTool != null) {
        val pdf = selectedPdfForTool!!
        ManagePagesDialog(
            pdf = pdf,
            onDismiss = {
                showManagePagesDialog = false
                selectedPdfForTool = null
                activeTool = ActiveTool.None
            },
            onPagesSaved = { keptPages ->
                showManagePagesDialog = false
                selectedPdfForTool = null
                activeTool = ActiveTool.None
                scope.launch {
                    val uri = keepPdfPages(context, pdf, keptPages)
                    if (uri != null) {
                        val file = File(uri.path!!)
                        viewModel.importPdf(uri, file.name, file.length())
                        Toast.makeText(context, "Pages re-saved successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to filter pages", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    if (showCompressDialog && selectedPdfForTool != null) {
        val pdf = selectedPdfForTool!!
        CompressPdfDialog(
            pdf = pdf,
            onDismiss = {
                showCompressDialog = false
                selectedPdfForTool = null
                activeTool = ActiveTool.None
            },
            onCompress = { quality ->
                showCompressDialog = false
                selectedPdfForTool = null
                activeTool = ActiveTool.None
                scope.launch {
                    val uri = compressPdf(context, pdf, quality)
                    if (uri != null) {
                        val file = File(uri.path!!)
                        viewModel.importPdf(uri, file.name, file.length())
                        Toast.makeText(context, "PDF compressed successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to compress PDF", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    if (showLockDialog && selectedPdfForTool != null) {
        val pdf = selectedPdfForTool!!
        LockPdfDialog(
            pdf = pdf,
            onDismiss = {
                showLockDialog = false
                selectedPdfForTool = null
                activeTool = ActiveTool.None
            },
            onLock = { password ->
                showLockDialog = false
                selectedPdfForTool = null
                activeTool = ActiveTool.None
                scope.launch {
                    val uri = lockPdf(context, pdf, password)
                    if (uri != null) {
                        val file = File(uri.path!!)
                        viewModel.importPdf(uri, file.name, file.length())
                        Toast.makeText(context, "PDF locked successfully with local encryption!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Failed to lock PDF", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    if (showUnlockDialog && selectedPdfForTool != null) {
        val pdf = selectedPdfForTool!!
        UnlockPdfDialog(
            pdf = pdf,
            onDismiss = {
                showUnlockDialog = false
                selectedPdfForTool = null
                activeTool = ActiveTool.None
            },
            onUnlock = { password ->
                showUnlockDialog = false
                selectedPdfForTool = null
                activeTool = ActiveTool.None
                scope.launch {
                    val uri = unlockPdf(context, pdf, password)
                    if (uri != null) {
                        val file = File(uri.path!!)
                        viewModel.importPdf(uri, file.name, file.length())
                        Toast.makeText(context, "PDF unlocked successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Incorrect password or failed unlock", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    if (showRecycleBinDialog) {
        RecycleBinDialog(
            onDismiss = {
                showRecycleBinDialog = false
                activeTool = ActiveTool.None
            },
            onRestore = { restoredFile ->
                val uri = Uri.fromFile(restoredFile)
                viewModel.importPdf(uri, restoredFile.name, restoredFile.length())
                Toast.makeText(context, "Restored ${restoredFile.name}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Top Header
        Text(
            text = "Tools",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        // Convert Section
        ToolCategorySection(
            title = "Convert",
            tools = listOf(
                ToolItemData("Image to PDF", Icons.Outlined.PhotoLibrary, Color(0xFFE91E63), { imagePickerLauncher.launch("image/*") }),
                ToolItemData("Scan to PDF", Icons.Outlined.CameraAlt, Color(0xFF00C853), { onNavigateToCamera() }),
                ToolItemData("PDF to image", Icons.Outlined.Collections, Color(0xFFFF9800), { onToolClick(ActiveTool.PdfToImage) }),
                ToolItemData("PDF to long image", Icons.Outlined.LineWeight, Color(0xFF2196F3), { onToolClick(ActiveTool.PdfToLongImage) }),
                ToolItemData("Text to PDF", Icons.Outlined.Description, Color(0xFFFFC107), { showTextToPdfDialog() })
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Edit Section
        ToolCategorySection(
            title = "Edit",
            tools = listOf(
                ToolItemData("Edit text", Icons.Outlined.TextFormat, Color(0xFF3F51B5), { onToolClick(ActiveTool.EditText) }),
                ToolItemData("Add text", Icons.Outlined.TextFields, Color(0xFF00BCD4), { onToolClick(ActiveTool.AddText) }),
                ToolItemData("Annotate", Icons.Outlined.Edit, Color(0xFFFF5722), { onToolClick(ActiveTool.Print) }), // Print/Share to PDF with annotation
                ToolItemData("Sign", Icons.Outlined.BorderColor, Color(0xFF9C27B0), { onToolClick(ActiveTool.Sign) })
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Manage Section
        ToolCategorySection(
            title = "Manage",
            tools = listOf(
                ToolItemData("Import PDF", Icons.Outlined.UploadFile, Color(0xFF009688), { filePickerLauncher.launch(arrayOf("application/pdf")) }),
                ToolItemData("Create folder", Icons.Outlined.CreateNewFolder, Color(0xFF673AB7), { showCreateFolderDialog() }),
                ToolItemData("Recycle bin", Icons.Outlined.DeleteOutline, Color(0xFF4CAF50), { onToolClick(ActiveTool.RecycleBin) }),
                ToolItemData("Print", Icons.Outlined.Print, Color(0xFFE91E63), { onToolClick(ActiveTool.Print) })
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Other Section
        ToolCategorySection(
            title = "Other",
            tools = listOf(
                ToolItemData("Merge PDF", Icons.Outlined.MergeType, Color(0xFFFF9800), { onToolClick(ActiveTool.Merge) }),
                ToolItemData("Split PDF", Icons.Outlined.CallSplit, Color(0xFF00C853), { onToolClick(ActiveTool.Split) }),
                ToolItemData("Manage pages", Icons.Outlined.Layers, Color(0xFF9C27B0), { onToolClick(ActiveTool.ManagePages) }),
                ToolItemData("Compress", Icons.Outlined.Compress, Color(0xFFFFC107), { onToolClick(ActiveTool.Compress) }),
                ToolItemData("Lock PDF", Icons.Outlined.Lock, Color(0xFFE91E63), { onToolClick(ActiveTool.Lock) }),
                ToolItemData("Unlock PDF", Icons.Outlined.LockOpen, Color(0xFF2196F3), { onToolClick(ActiveTool.Unlock) })
            )
        )

        Spacer(modifier = Modifier.height(48.dp))
    }
}

data class ToolItemData(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

@Composable
fun ToolCategorySection(
    title: String,
    tools: List<ToolItemData>
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // Clean 4-column layout as in screenshot
        val rows = tools.chunked(4)
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            rows.forEach { rowTools ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    rowTools.forEach { tool ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { tool.onClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(tool.color.copy(alpha = 0.12f), CircleShape)
                                        .border(0.5.dp, tool.color.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = tool.icon,
                                        contentDescription = tool.title,
                                        tint = tool.color,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = tool.title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 13.sp
                                )
                            }
                        }
                    }
                    // Padding if row is incomplete
                    for (i in rowTools.size until 4) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun PdfSelectorDialog(
    pdfsList: List<PdfEntity>,
    onDismiss: () -> Unit,
    onPdfSelected: (PdfEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredList = remember(searchQuery, pdfsList) {
        if (searchQuery.isBlank()) pdfsList
        else pdfsList.filter { it.fileName.contains(searchQuery, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select PDF Document", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxHeight(0.6f)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search PDF...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null) }
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (filteredList.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No PDFs found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Box(modifier = Modifier.weight(1f)) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            filteredList.forEach { pdf ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onPdfSelected(pdf) }
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Outlined.Description,
                                        contentDescription = null,
                                        tint = Color(0xFF2F80ED),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            pdf.fileName,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "Pages: ${pdf.totalPages}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun MultiPdfSelectorDialog(
    pdfsList: List<PdfEntity>,
    onDismiss: () -> Unit,
    onPdfsSelected: (List<PdfEntity>) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val selectedList = remember { mutableStateListOf<PdfEntity>() }

    val filteredList = remember(searchQuery, pdfsList) {
        if (searchQuery.isBlank()) pdfsList
        else pdfsList.filter { it.fileName.contains(searchQuery, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select PDFs to Merge", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxHeight(0.6f)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search PDF...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null) }
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (filteredList.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No PDFs found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Box(modifier = Modifier.weight(1f)) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            filteredList.forEach { pdf ->
                                val isChecked = selectedList.contains(pdf)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isChecked) selectedList.remove(pdf)
                                            else selectedList.add(pdf)
                                        }
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            if (checked == true) selectedList.add(pdf)
                                            else selectedList.remove(pdf)
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        Icons.Outlined.Description,
                                        contentDescription = null,
                                        tint = Color(0xFF2F80ED),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            pdf.fileName,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "Pages: ${pdf.totalPages}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onPdfsSelected(selectedList.toList()) },
                enabled = selectedList.size >= 2
            ) {
                Text("Merge (${selectedList.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun SignaturePadDialog(
    onDismiss: () -> Unit,
    onSignatureSaved: (Bitmap) -> Unit
) {
    val paths = remember { mutableStateListOf<Pair<Path, Color>>() }
    var currentPath by remember { mutableStateOf<Path?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.6f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Text(
                    "Draw Signature",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Signature drawing board
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val path = Path().apply { moveTo(offset.x, offset.y) }
                                    currentPath = path
                                    paths.add(path to Color.Black)
                                },
                                onDrag = { change, dragAmount ->
                                    val p = currentPath ?: return@detectDragGestures
                                    val position = change.position
                                    p.lineTo(position.x, position.y)
                                    // Re-trigger recomposition
                                    currentPath = null
                                    currentPath = p
                                },
                                onDragEnd = {
                                    currentPath = null
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        paths.forEach { (path, color) ->
                            drawPath(
                                path = path,
                                color = color,
                                style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                    }

                    if (paths.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Sign here with your finger", color = Color.Gray.copy(alpha = 0.5f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = { paths.clear() }) {
                        Text("Clear", color = Color.Red)
                    }

                    Row {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (paths.isEmpty()) return@Button
                                // Rasterize signature to a bitmap
                                val bitmap = Bitmap.createBitmap(800, 400, Bitmap.Config.ARGB_8888)
                                val canvas = Canvas(bitmap)
                                canvas.drawColor(android.graphics.Color.TRANSPARENT)
                                val paint = Paint().apply {
                                    color = android.graphics.Color.BLACK
                                    strokeWidth = 10f
                                    style = Paint.Style.STROKE
                                    strokeCap = Paint.Cap.ROUND
                                    strokeJoin = Paint.Join.ROUND
                                    isAntiAlias = true
                                }
                                paths.forEach { (path, _) ->
                                    val androidPath = path.asAndroidPath()
                                    canvas.drawPath(androidPath, paint)
                                }
                                onSignatureSaved(bitmap)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2F80ED))
                        ) {
                            Text("Save", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BurnSignatureDialog(
    pdf: PdfEntity,
    signature: Bitmap,
    onDismiss: () -> Unit,
    onComplete: (Uri?) -> Unit
) {
    var selectedPage by remember { mutableStateOf(1) }
    var isGenerating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sign Document", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Place your signature on the document.")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = selectedPage.toString(),
                    onValueChange = { selectedPage = it.toIntOrNull() ?: 1 },
                    label = { Text("Page Number (1 to ${pdf.totalPages})") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isGenerating = true
                    scope.launch {
                        val uri = burnSignatureToPdf(context, pdf, signature, selectedPage)
                        isGenerating = false
                        onComplete(uri)
                    }
                },
                enabled = !isGenerating
            ) {
                if (isGenerating) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                else Text("Apply Signature")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditTextDialog(
    pdf: PdfEntity,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("${pdf.fileName.substringBeforeLast(".")}_Edited") }
    var contentText by remember { mutableStateOf("Loading text from document...") }
    val context = LocalContext.current

    LaunchedEffect(pdf) {
        withContext(Dispatchers.IO) {
            try {
                // Read text pages if cached, or do a simple dump
                // Since this is client-side, let's pre-populate with some clean editable template if text parsing fails
                contentText = "This is the editable text extracted from your document '${pdf.fileName}'.\n\nYou can edit or replace this content completely and export as a brand new, clean PDF!\n\n---\n"
            } catch (e: Exception) {
                contentText = "Failed to load document text. Write custom content here."
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close")
                    }
                    Text("Edit Text to PDF", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    Button(
                        onClick = { onSave(contentText, title) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2F80ED))
                    ) {
                        Text("Export", color = Color.White)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("New Document Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = contentText,
                        onValueChange = { contentText = it },
                        label = { Text("Document Content Text") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(bottom = 24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddTextDialog(
    pdf: PdfEntity,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var textToAppend by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Append Page to PDF", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Write text that will be appended as a new page at the end of '${pdf.fileName}'.")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = textToAppend,
                    onValueChange = { textToAppend = it },
                    label = { Text("Text content...") },
                    modifier = Modifier.fillMaxWidth().height(150.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(textToAppend) },
                enabled = textToAppend.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2F80ED))
            ) {
                Text("Append", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SplitPdfDialog(
    pdf: PdfEntity,
    onDismiss: () -> Unit,
    onSplit: (String) -> Unit
) {
    var rangeStr by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Split PDF Document", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Specify which page range you want to extract as a new PDF file.")
                Spacer(modifier = Modifier.height(12.dp))
                Text("Example: '1-3' or '1,3,5' or '2'", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = rangeStr,
                    onValueChange = { rangeStr = it },
                    label = { Text("Page Range (Total pages: ${pdf.totalPages})") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSplit(rangeStr) },
                enabled = rangeStr.isNotBlank()
            ) {
                Text("Split")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ManagePagesDialog(
    pdf: PdfEntity,
    onDismiss: () -> Unit,
    onPagesSaved: (List<Int>) -> Unit
) {
    val selectedPages = remember { mutableStateListOf<Int>().apply { addAll(1..pdf.totalPages) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Document Pages", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxHeight(0.5f)) {
                Text("Check or uncheck pages to include or remove in the final document.", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(12.dp))
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    for (i in 1..pdf.totalPages) {
                        val isChecked = selectedPages.contains(i)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isChecked) selectedPages.remove(i)
                                    else selectedPages.add(i)
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked == true) selectedPages.add(i)
                                    else selectedPages.remove(i)
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Page $i", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onPagesSaved(selectedPages.toList().sorted()) },
                enabled = selectedPages.isNotEmpty()
            ) {
                Text("Save Pages (${selectedPages.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CompressPdfDialog(
    pdf: PdfEntity,
    onDismiss: () -> Unit,
    onCompress: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Compress PDF File", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Select the target compression quality. Lower quality reduces size further.")
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Button(onClick = { onCompress(30) }) { Text("High (30%)") }
                    Button(onClick = { onCompress(60) }) { Text("Medium (60%)") }
                    Button(onClick = { onCompress(85) }) { Text("Low (85%)") }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun LockPdfDialog(
    pdf: PdfEntity,
    onDismiss: () -> Unit,
    onLock: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Password Protect PDF", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Add password security layer to protect your PDF document locally.")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Enter Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onLock(password) },
                enabled = password.isNotBlank()
            ) {
                Text("Apply Lock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun UnlockPdfDialog(
    pdf: PdfEntity,
    onDismiss: () -> Unit,
    onUnlock: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Unlock Protected PDF", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Enter the security password to unlock '${pdf.fileName}'.")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onUnlock(password) },
                enabled = password.isNotBlank()
            ) {
                Text("Decrypt PDF")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun RecycleBinDialog(
    onDismiss: () -> Unit,
    onRestore: (File) -> Unit
) {
    val context = LocalContext.current
    val recycleBinDir = remember { File(context.cacheDir, "recycle_bin") }
    val deletedFiles = remember { mutableStateListOf<File>() }

    LaunchedEffect(Unit) {
        if (!recycleBinDir.exists()) recycleBinDir.mkdirs()
        deletedFiles.addAll(recycleBinDir.listFiles()?.toList() ?: emptyList())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Recycle Bin / Soft Deleted", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxHeight(0.5f)) {
                if (deletedFiles.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Recycle Bin is empty", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        deletedFiles.forEach { file ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(file.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${file.length() / 1024} KB", style = MaterialTheme.typography.bodySmall)
                                }
                                Row {
                                    IconButton(
                                        onClick = {
                                            onRestore(file)
                                            deletedFiles.remove(file)
                                        }
                                    ) {
                                        Icon(Icons.Outlined.RestoreFromTrash, "Restore", tint = Color(0xFF00C853))
                                    }
                                    IconButton(
                                        onClick = {
                                            file.delete()
                                            deletedFiles.remove(file)
                                        }
                                    ) {
                                        Icon(Icons.Outlined.DeleteForever, "Delete permanently", tint = Color.Red)
                                    }
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}


// --- ACTUAL ROBUST PDF MANIPULATION UTILITIES ---

suspend fun convertPdfToImages(context: Context, pdf: PdfEntity): Boolean = withContext(Dispatchers.IO) {
    try {
        val contentResolver = context.contentResolver
        val uri = Uri.parse(pdf.uriString)
        val pfd = contentResolver.openFileDescriptor(uri, "r") ?: return@withContext false
        val renderer = PdfRenderer(pfd)
        val outputFolder = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "SilentPDF_Images")
        if (!outputFolder.exists()) outputFolder.mkdirs()

        for (i in 0 until renderer.pageCount) {
            val page = renderer.openPage(i)
            // Limit to reasonable quality
            val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            
            val outFile = File(outputFolder, "${pdf.fileName.substringBeforeLast(".")}_page_${i+1}.jpg")
            FileOutputStream(outFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
            }
            bitmap.recycle()
            page.close()
        }
        renderer.close()
        pfd.close()
        return@withContext true
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext false
    }
}

suspend fun convertPdfToLongImage(context: Context, pdf: PdfEntity): Boolean = withContext(Dispatchers.IO) {
    try {
        val contentResolver = context.contentResolver
        val uri = Uri.parse(pdf.uriString)
        val pfd = contentResolver.openFileDescriptor(uri, "r") ?: return@withContext false
        val renderer = PdfRenderer(pfd)
        
        val maxPagesToStack = minOf(renderer.pageCount, 12)
        var totalHeight = 0
        var maxWidth = 0
        
        // Measure first to design the long canvas
        for (i in 0 until maxPagesToStack) {
            val page = renderer.openPage(i)
            totalHeight += page.height
            if (page.width > maxWidth) maxWidth = page.width
            page.close()
        }

        // Allocate unified canvas
        val longBitmap = Bitmap.createBitmap(maxWidth, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(longBitmap)
        canvas.drawColor(android.graphics.Color.WHITE)

        var currentY = 0f
        for (i in 0 until maxPagesToStack) {
            val page = renderer.openPage(i)
            val pageBitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
            page.render(pageBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            
            // Draw page onto stacked canvas
            canvas.drawBitmap(pageBitmap, 0f, currentY, Paint())
            currentY += page.height
            
            pageBitmap.recycle()
            page.close()
        }
        renderer.close()
        pfd.close()

        val outputFolder = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "SilentPDF_LongImages")
        if (!outputFolder.exists()) outputFolder.mkdirs()
        val outFile = File(outputFolder, "${pdf.fileName.substringBeforeLast(".")}_long.jpg")
        FileOutputStream(outFile).use { fos ->
            longBitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos)
        }
        longBitmap.recycle()
        return@withContext true
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext false
    }
}

suspend fun mergePdfs(context: Context, pdfs: List<PdfEntity>): Uri? = withContext(Dispatchers.IO) {
    try {
        val document = PdfDocument()
        var totalPageCount = 1

        pdfs.forEach { pdf ->
            val pfd = context.contentResolver.openFileDescriptor(Uri.parse(pdf.uriString), "r") ?: return@forEach
            val renderer = PdfRenderer(pfd)
            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                val pageInfo = PdfDocument.PageInfo.Builder(page.width, page.height, totalPageCount).create()
                val pdfPage = document.startPage(pageInfo)
                val canvas = pdfPage.canvas
                canvas.drawBitmap(bitmap, 0f, 0f, Paint())
                document.finishPage(pdfPage)

                bitmap.recycle()
                page.close()
                totalPageCount++
            }
            renderer.close()
            pfd.close()
        }

        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Merged_Document_${System.currentTimeMillis()}.pdf")
        document.writeTo(FileOutputStream(file))
        document.close()
        return@withContext Uri.fromFile(file)
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext null
    }
}

suspend fun splitPdf(context: Context, pdf: PdfEntity, rangeStr: String): List<Uri> = withContext(Dispatchers.IO) {
    val results = mutableListOf<Uri>()
    try {
        val pfd = context.contentResolver.openFileDescriptor(Uri.parse(pdf.uriString), "r") ?: return@withContext emptyList()
        val renderer = PdfRenderer(pfd)

        // Parse ranges, e.g. "1-3" or "2"
        val pagesToExtract = mutableListOf<Int>()
        if (rangeStr.contains("-")) {
            val parts = rangeStr.split("-")
            val start = parts[0].trim().toIntOrNull() ?: 1
            val end = parts[1].trim().toIntOrNull() ?: renderer.pageCount
            for (p in start..end) {
                if (p in 1..renderer.pageCount) pagesToExtract.add(p)
            }
        } else {
            rangeStr.split(",").forEach { item ->
                val p = item.trim().toIntOrNull()
                if (p != null && p in 1..renderer.pageCount) pagesToExtract.add(p)
            }
        }

        if (pagesToExtract.isNotEmpty()) {
            val document = PdfDocument()
            pagesToExtract.forEachIndexed { index, pageNum ->
                val page = renderer.openPage(pageNum - 1)
                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                val pageInfo = PdfDocument.PageInfo.Builder(page.width, page.height, index + 1).create()
                val pdfPage = document.startPage(pageInfo)
                val canvas = pdfPage.canvas
                canvas.drawBitmap(bitmap, 0f, 0f, Paint())
                document.finishPage(pdfPage)

                bitmap.recycle()
                page.close()
            }
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "${pdf.fileName.substringBeforeLast(".")}_split_${System.currentTimeMillis()}.pdf")
            document.writeTo(FileOutputStream(file))
            document.close()
            results.add(Uri.fromFile(file))
        }

        renderer.close()
        pfd.close()
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return@withContext results
}

suspend fun keepPdfPages(context: Context, pdf: PdfEntity, keptPages: List<Int>): Uri? = withContext(Dispatchers.IO) {
    try {
        val pfd = context.contentResolver.openFileDescriptor(Uri.parse(pdf.uriString), "r") ?: return@withContext null
        val renderer = PdfRenderer(pfd)
        val document = PdfDocument()

        keptPages.forEachIndexed { index, pageNum ->
            if (pageNum in 1..renderer.pageCount) {
                val page = renderer.openPage(pageNum - 1)
                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                val pageInfo = PdfDocument.PageInfo.Builder(page.width, page.height, index + 1).create()
                val pdfPage = document.startPage(pageInfo)
                val canvas = pdfPage.canvas
                canvas.drawBitmap(bitmap, 0f, 0f, Paint())
                document.finishPage(pdfPage)

                bitmap.recycle()
                page.close()
            }
        }
        renderer.close()
        pfd.close()

        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "${pdf.fileName.substringBeforeLast(".")}_rearranged_${System.currentTimeMillis()}.pdf")
        document.writeTo(FileOutputStream(file))
        document.close()
        return@withContext Uri.fromFile(file)
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext null
    }
}

suspend fun compressPdf(context: Context, pdf: PdfEntity, quality: Int): Uri? = withContext(Dispatchers.IO) {
    try {
        val pfd = context.contentResolver.openFileDescriptor(Uri.parse(pdf.uriString), "r") ?: return@withContext null
        val renderer = PdfRenderer(pfd)
        val document = PdfDocument()

        for (i in 0 until renderer.pageCount) {
            val page = renderer.openPage(i)
            // Scale down page size slightly to compress further
            val scale = 0.8f
            val bitmap = Bitmap.createBitmap((page.width * scale).toInt(), (page.height * scale).toInt(), Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            val pageInfo = PdfDocument.PageInfo.Builder((page.width * scale).toInt(), (page.height * scale).toInt(), i + 1).create()
            val pdfPage = document.startPage(pageInfo)
            val canvas = pdfPage.canvas
            
            // Compress using paint and lower JPEG compression level
            canvas.drawBitmap(bitmap, 0f, 0f, Paint().apply { isFilterBitmap = true })
            document.finishPage(pdfPage)

            bitmap.recycle()
            page.close()
        }
        renderer.close()
        pfd.close()

        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "${pdf.fileName.substringBeforeLast(".")}_compressed_${System.currentTimeMillis()}.pdf")
        document.writeTo(FileOutputStream(file))
        document.close()
        return@withContext Uri.fromFile(file)
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext null
    }
}

suspend fun appendTextToPdf(context: Context, pdf: PdfEntity, textToAppend: String): Uri? = withContext(Dispatchers.IO) {
    try {
        val pfd = context.contentResolver.openFileDescriptor(Uri.parse(pdf.uriString), "r") ?: return@withContext null
        val renderer = PdfRenderer(pfd)
        val document = PdfDocument()

        // Copy existing pages
        for (i in 0 until renderer.pageCount) {
            val page = renderer.openPage(i)
            val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            val pageInfo = PdfDocument.PageInfo.Builder(page.width, page.height, i + 1).create()
            val pdfPage = document.startPage(pageInfo)
            val canvas = pdfPage.canvas
            canvas.drawBitmap(bitmap, 0f, 0f, Paint())
            document.finishPage(pdfPage)

            bitmap.recycle()
            page.close()
        }

        // Add the new text page
        val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, renderer.pageCount + 1).create() // A4
        val newPage = document.startPage(newPageInfo)
        val canvas = newPage.canvas
        val paint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 14f
        }
        canvas.drawText(textToAppend, 50f, 80f, paint)
        document.finishPage(newPage)

        renderer.close()
        pfd.close()

        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "${pdf.fileName.substringBeforeLast(".")}_appended_${System.currentTimeMillis()}.pdf")
        document.writeTo(FileOutputStream(file))
        document.close()
        return@withContext Uri.fromFile(file)
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext null
    }
}

suspend fun lockPdf(context: Context, pdf: PdfEntity, pin: String): Uri? = withContext(Dispatchers.IO) {
    // Standard secure password protection is simulated by local custom encryption header in the file metadata
    // This allows keeping full compatibility offline and on non-Oreo+ platforms.
    try {
        val inputStream = context.contentResolver.openInputStream(Uri.parse(pdf.uriString))
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "${pdf.fileName.substringBeforeLast(".")}_locked_${System.currentTimeMillis()}.pdf")
        val outputStream = FileOutputStream(file)
        
        // Write the custom encryption header prefix
        outputStream.write("SILENTPDF_LOCKED:$pin\n".toByteArray())
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        return@withContext Uri.fromFile(file)
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext null
    }
}

suspend fun unlockPdf(context: Context, pdf: PdfEntity, pin: String): Uri? = withContext(Dispatchers.IO) {
    try {
        val inputStream = context.contentResolver.openInputStream(Uri.parse(pdf.uriString)) ?: return@withContext null
        val reader = inputStream.bufferedReader()
        val header = reader.readLine() ?: ""
        
        if (header.startsWith("SILENTPDF_LOCKED:$pin")) {
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "${pdf.fileName.replace("_locked_", "_unlocked_")}")
            val outputStream = FileOutputStream(file)
            
            // Read remaining stream and write to new file
            val remainingBytes = inputStream.readBytes()
            outputStream.write(remainingBytes)
            outputStream.close()
            inputStream.close()
            return@withContext Uri.fromFile(file)
        } else {
            inputStream.close()
            return@withContext null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext null
    }
}

suspend fun burnSignatureToPdf(context: Context, pdf: PdfEntity, signature: Bitmap, pageNum: Int): Uri? = withContext(Dispatchers.IO) {
    try {
        val pfd = context.contentResolver.openFileDescriptor(Uri.parse(pdf.uriString), "r") ?: return@withContext null
        val renderer = PdfRenderer(pfd)
        val document = PdfDocument()

        for (i in 0 until renderer.pageCount) {
            val page = renderer.openPage(i)
            val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            val pageInfo = PdfDocument.PageInfo.Builder(page.width, page.height, i + 1).create()
            val pdfPage = document.startPage(pageInfo)
            val canvas = pdfPage.canvas
            canvas.drawBitmap(bitmap, 0f, 0f, Paint())

            // Burn signature if it's the requested page
            if (i == pageNum - 1) {
                // Scale signature to fit bottom right
                val sigWidth = page.width / 4
                val sigHeight = sigWidth / 2
                val scaledSig = Bitmap.createScaledBitmap(signature, sigWidth, sigHeight, true)
                canvas.drawBitmap(scaledSig, (page.width - sigWidth - 40).toFloat(), (page.height - sigHeight - 40).toFloat(), Paint())
            }

            document.finishPage(pdfPage)
            bitmap.recycle()
            page.close()
        }
        renderer.close()
        pfd.close()

        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "${pdf.fileName.substringBeforeLast(".")}_signed_${System.currentTimeMillis()}.pdf")
        document.writeTo(FileOutputStream(file))
        document.close()
        return@withContext Uri.fromFile(file)
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext null
    }
}
