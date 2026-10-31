package com.silentpdf.app.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import java.util.UUID
import kotlin.math.roundToInt

data class VisualTextItem(
    val id: String = UUID.randomUUID().toString(),
    val pageIndex: Int,
    var text: String,
    var normX: Float, // 0f..1f relative to page width
    var normY: Float, // 0f..1f relative to page height (from top)
    var isEditing: Boolean = false
)

data class VisualStroke(
    val pageIndex: Int,
    val pointsNorm: List<Offset>, // 0f..1f relative to page
    val color: Color,
    val strokeWidth: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualEditorScreen(
    pdf: PdfEntity,
    initialTool: ActiveTool,
    onDismiss: () -> Unit,
    onSaveSuccess: (Uri, File) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentPageIndex by remember { mutableIntStateOf(0) }
    var totalPages by remember { mutableIntStateOf(1) }
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingPage by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    // Active tool: 0 = Text, 1 = Draw/Annotate, 2 = Sign
    var selectedToolMode by remember {
        mutableIntStateOf(
            when (initialTool) {
                ActiveTool.Sign -> 2
                ActiveTool.Print -> 1
                else -> 0
            }
        )
    }

    // Annotations across pages
    val textItems = remember { mutableStateListOf<VisualTextItem>() }
    val drawStrokes = remember { mutableStateListOf<VisualStroke>() }
    var currentDrawPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var penColor by remember { mutableStateOf(Color(0xFF2F80ED)) }
    var penWidth by remember { mutableFloatStateOf(4f) }

    var showSignatureDialog by remember { mutableStateOf(false) }

    // Container and image measurement for accurate touch mapping
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    // Load page bitmap when currentPageIndex changes
    LaunchedEffect(pdf, currentPageIndex) {
        isLoadingPage = true
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openFileDescriptor(Uri.parse(pdf.uriString), "r")?.use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        totalPages = renderer.pageCount.coerceAtLeast(1)
                        val safeIndex = currentPageIndex.coerceIn(0, totalPages - 1)
                        val page = renderer.openPage(safeIndex)
                        val scale = 2f
                        val bmp = Bitmap.createBitmap(
                            (page.width * scale).toInt().coerceAtLeast(100),
                            (page.height * scale).toInt().coerceAtLeast(100),
                            Bitmap.Config.ARGB_8888
                        )
                        val canvas = android.graphics.Canvas(bmp)
                        canvas.drawColor(android.graphics.Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()
                        pageBitmap = bmp
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            isLoadingPage = false
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
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Visual Editor",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Page ${currentPageIndex + 1} of $totalPages",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        // Page navigation controls
                        IconButton(
                            onClick = { if (currentPageIndex > 0) currentPageIndex-- },
                            enabled = currentPageIndex > 0
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Page")
                        }
                        IconButton(
                            onClick = { if (currentPageIndex < totalPages - 1) currentPageIndex++ },
                            enabled = currentPageIndex < totalPages - 1
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Page")
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Button(
                            onClick = {
                                if (isSaving) return@Button
                                isSaving = true
                                scope.launch {
                                    val result = savePdfWithEdits(
                                        context = context,
                                        pdf = pdf,
                                        texts = textItems.toList(),
                                        strokes = drawStrokes.toList()
                                    )
                                    isSaving = false
                                    if (result != null) {
                                        Toast.makeText(context, "Saved successfully!", Toast.LENGTH_SHORT).show()
                                        onSaveSuccess(result.first, result.second)
                                    } else {
                                        Toast.makeText(context, "Failed to save PDF", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = !isSaving,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2F80ED))
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text("Done")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )

                // Main Document Canvas Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFFE0E0E0))
                        .onSizeChanged { containerSize = it },
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoadingPage) {
                        CircularProgressIndicator()
                    } else if (pageBitmap != null && containerSize.width > 0 && containerSize.height > 0) {
                        val bmp = pageBitmap!!
                        val containerW = containerSize.width.toFloat()
                        val containerH = containerSize.height.toFloat()

                        // Calculate aspect-fit dimensions and bounding rect inside container
                        val bmpAspect = bmp.width.toFloat() / bmp.height.toFloat()
                        val containerAspect = containerW / containerH

                        val (renderW, renderH) = if (bmpAspect > containerAspect) {
                            containerW to (containerW / bmpAspect)
                        } else {
                            (containerH * bmpAspect) to containerH
                        }

                        val leftOffset = (containerW - renderW) / 2f
                        val topOffset = (containerH - renderH) / 2f

                        Box(
                            modifier = Modifier
                                .offset { IntOffset(leftOffset.roundToInt(), topOffset.roundToInt()) }
                                .size(
                                    width = (renderW / LocalContext.current.resources.displayMetrics.density).dp,
                                    height = (renderH / LocalContext.current.resources.displayMetrics.density).dp
                                )
                                .background(Color.White)
                                .pointerInput(selectedToolMode, currentPageIndex) {
                                    if (selectedToolMode == 0) {
                                        // Tap to add text
                                        detectTapGestures { tapOffset ->
                                            val normX = (tapOffset.x / renderW).coerceIn(0.05f, 0.85f)
                                            val normY = (tapOffset.y / renderH).coerceIn(0.05f, 0.90f)
                                            textItems.add(
                                                VisualTextItem(
                                                    pageIndex = currentPageIndex,
                                                    text = "Tap to edit",
                                                    normX = normX,
                                                    normY = normY,
                                                    isEditing = true
                                                )
                                            )
                                        }
                                    } else if (selectedToolMode == 1) {
                                        // Freehand draw
                                        detectDragGestures(
                                            onDragStart = { startPt ->
                                                currentDrawPoints = listOf(
                                                    Offset(
                                                        (startPt.x / renderW).coerceIn(0f, 1f),
                                                        (startPt.y / renderH).coerceIn(0f, 1f)
                                                    )
                                                )
                                            },
                                            onDrag = { change, _ ->
                                                val normPt = Offset(
                                                    (change.position.x / renderW).coerceIn(0f, 1f),
                                                    (change.position.y / renderH).coerceIn(0f, 1f)
                                                )
                                                currentDrawPoints = currentDrawPoints + normPt
                                            },
                                            onDragEnd = {
                                                if (currentDrawPoints.size > 1) {
                                                    drawStrokes.add(
                                                        VisualStroke(
                                                            pageIndex = currentPageIndex,
                                                            pointsNorm = currentDrawPoints,
                                                            color = penColor,
                                                            strokeWidth = penWidth
                                                        )
                                                    )
                                                }
                                                currentDrawPoints = emptyList()
                                            },
                                            onDragCancel = { currentDrawPoints = emptyList() }
                                        )
                                    }
                                }
                        ) {
                            // Render base PDF page image
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Page ${currentPageIndex + 1}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.FillBounds
                            )

                            // Render existing strokes on this page
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val curPageStrokes = drawStrokes.filter { it.pageIndex == currentPageIndex }
                                curPageStrokes.forEach { stroke ->
                                    if (stroke.pointsNorm.size > 1) {
                                        val path = androidx.compose.ui.graphics.Path().apply {
                                            moveTo(stroke.pointsNorm[0].x * size.width, stroke.pointsNorm[0].y * size.height)
                                            for (i in 1 until stroke.pointsNorm.size) {
                                                lineTo(stroke.pointsNorm[i].x * size.width, stroke.pointsNorm[i].y * size.height)
                                            }
                                        }
                                        drawPath(
                                            path = path,
                                            color = stroke.color,
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                                width = stroke.strokeWidth,
                                                cap = StrokeCap.Round
                                            )
                                        )
                                    }
                                }

                                // In-progress stroke
                                if (currentDrawPoints.size > 1) {
                                    val path = androidx.compose.ui.graphics.Path().apply {
                                        moveTo(currentDrawPoints[0].x * size.width, currentDrawPoints[0].y * size.height)
                                        for (i in 1 until currentDrawPoints.size) {
                                            lineTo(currentDrawPoints[i].x * size.width, currentDrawPoints[i].y * size.height)
                                        }
                                    }
                                    drawPath(
                                        path = path,
                                        color = penColor,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                                            width = penWidth,
                                            cap = StrokeCap.Round
                                        )
                                    )
                                }
                            }

                            // Render text items on this page
                            val pageTexts = textItems.filter { it.pageIndex == currentPageIndex }
                            pageTexts.forEach { item ->
                                val itemPxX = item.normX * renderW
                                val itemPxY = item.normY * renderH

                                Box(
                                    modifier = Modifier
                                        .offset { IntOffset(itemPxX.roundToInt(), itemPxY.roundToInt()) }
                                        .pointerInput(item.id) {
                                            detectDragGestures { change, dragAmount ->
                                                change.consume()
                                                val newNormX = ((item.normX * renderW) + dragAmount.x) / renderW
                                                val newNormY = ((item.normY * renderH) + dragAmount.y) / renderH
                                                item.normX = newNormX.coerceIn(0f, 0.95f)
                                                item.normY = newNormY.coerceIn(0f, 0.95f)
                                            }
                                        }
                                        .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                                        .border(1.dp, Color(0xFF2F80ED), RoundedCornerShape(4.dp))
                                        .padding(4.dp)
                                ) {
                                    if (item.isEditing) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            OutlinedTextField(
                                                value = item.text,
                                                onValueChange = { item.text = it },
                                                modifier = Modifier.width(180.dp),
                                                singleLine = true,
                                                textStyle = MaterialTheme.typography.bodyMedium
                                            )
                                            IconButton(onClick = { item.isEditing = false }) {
                                                Icon(Icons.Default.Check, "Done", tint = Color(0xFF2F80ED))
                                            }
                                        }
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = item.text.ifEmpty { "Edit Text" },
                                                color = Color.Black,
                                                fontSize = 14.sp,
                                                modifier = Modifier.clickable { item.isEditing = true }
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            IconButton(
                                                onClick = { textItems.remove(item) },
                                                modifier = Modifier.size(20.dp)
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

                // Sub-toolbar for tool options
                if (selectedToolMode == 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Pen Color:", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(Color.Black, Color(0xFF2F80ED), Color(0xFFE91E63), Color(0xFF4CAF50)).forEach { c ->
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(c)
                                        .border(
                                            if (penColor == c) 2.dp else 0.dp,
                                            MaterialTheme.colorScheme.onSurface,
                                            CircleShape
                                        )
                                        .clickable { penColor = c }
                                )
                            }
                        }
                        Button(
                            onClick = { drawStrokes.removeAll { it.pageIndex == currentPageIndex } },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Clear Drawings", fontSize = 11.sp)
                        }
                    }
                }

                // Bottom Navigation Toolbar
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = selectedToolMode == 0,
                        onClick = { selectedToolMode = 0 },
                        icon = { Icon(Icons.Default.TextFields, contentDescription = "Add Text") },
                        label = { Text("Text") }
                    )
                    NavigationBarItem(
                        selected = selectedToolMode == 1,
                        onClick = { selectedToolMode = 1 },
                        icon = { Icon(Icons.Default.Edit, contentDescription = "Draw") },
                        label = { Text("Draw") }
                    )
                    NavigationBarItem(
                        selected = selectedToolMode == 2,
                        onClick = {
                            selectedToolMode = 2
                            showSignatureDialog = true
                        },
                        icon = { Icon(Icons.Default.BorderColor, contentDescription = "Sign") },
                        label = { Text("Sign") }
                    )
                }
            }
        }
    }

    if (showSignatureDialog) {
        SignaturePadDialog(
            onDismiss = { showSignatureDialog = false },
            onSignatureCaptured = { sigStrokes ->
                showSignatureDialog = false
                // Add the drawn signature as normalized strokes onto the current page center
                if (sigStrokes.isNotEmpty()) {
                    sigStrokes.forEach { strokePts ->
                        if (strokePts.size > 1) {
                            // Scale down and center on page
                            val centeredNorm = strokePts.map { pt ->
                                Offset(0.35f + (pt.x * 0.3f), 0.70f + (pt.y * 0.15f))
                            }
                            drawStrokes.add(
                                VisualStroke(
                                    pageIndex = currentPageIndex,
                                    pointsNorm = centeredNorm,
                                    color = Color.Black,
                                    strokeWidth = 3f
                                )
                            )
                        }
                    }
                    Toast.makeText(context, "Signature placed on Page ${currentPageIndex + 1}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
fun SignaturePadDialog(
    onDismiss: () -> Unit,
    onSignatureCaptured: (List<List<Offset>>) -> Unit
) {
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Signature", fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Draw your signature below:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                        .onSizeChanged { canvasSize = it }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val w = canvasSize.width.toFloat().coerceAtLeast(1f)
                                    val h = canvasSize.height.toFloat().coerceAtLeast(1f)
                                    currentStroke = listOf(Offset(offset.x / w, offset.y / h))
                                },
                                onDrag = { change, _ ->
                                    val w = canvasSize.width.toFloat().coerceAtLeast(1f)
                                    val h = canvasSize.height.toFloat().coerceAtLeast(1f)
                                    currentStroke = currentStroke + Offset(change.position.x / w, change.position.y / h)
                                },
                                onDragEnd = {
                                    if (currentStroke.isNotEmpty()) {
                                        strokes.add(currentStroke)
                                    }
                                    currentStroke = emptyList()
                                },
                                onDragCancel = { currentStroke = emptyList() }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        strokes.forEach { stroke ->
                            if (stroke.size > 1) {
                                val path = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(stroke[0].x * size.width, stroke[0].y * size.height)
                                    for (i in 1 until stroke.size) {
                                        lineTo(stroke[i].x * size.width, stroke[i].y * size.height)
                                    }
                                }
                                drawPath(path, Color.Black, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                            }
                        }
                        if (currentStroke.size > 1) {
                            val path = androidx.compose.ui.graphics.Path().apply {
                                moveTo(currentStroke[0].x * size.width, currentStroke[0].y * size.height)
                                for (i in 1 until currentStroke.size) {
                                    lineTo(currentStroke[i].x * size.width, currentStroke[i].y * size.height)
                                }
                            }
                            drawPath(path, Color.Black, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { strokes.clear(); currentStroke = emptyList() }) {
                    Text("Clear", color = Color.Red)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSignatureCaptured(strokes.toList()) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2F80ED))
            ) {
                Text("Use Signature")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Sanitizes input text to guarantee compatibility with standard PDF WinAnsiEncoding.
 * Prevents IllegalArgumentException crashes when typing accented or non-ASCII characters.
 */
private fun sanitizePdfText(text: String): String {
    return text.map { ch ->
        if (ch.code in 32..126 || ch.code in 160..255) ch else '?'
    }.joinToString("")
}

suspend fun savePdfWithEdits(
    context: Context,
    pdf: PdfEntity,
    texts: List<VisualTextItem>,
    strokes: List<VisualStroke>
): Pair<Uri, File>? = withContext(Dispatchers.IO) {
    try {
        val pfd = context.contentResolver.openFileDescriptor(Uri.parse(pdf.uriString), "r") ?: return@withContext null
        val document = PDDocument.load(FileInputStream(pfd.fileDescriptor), com.tom_roush.pdfbox.io.MemoryUsageSetting.setupTempFileOnly())

        // Group edits by page
        val textsByPage = texts.groupBy { it.pageIndex }
        val strokesByPage = strokes.groupBy { it.pageIndex }
        val allPagesToEdit = (textsByPage.keys + strokesByPage.keys).distinct()

        for (pageIdx in allPagesToEdit) {
            if (pageIdx in 0 until document.numberOfPages) {
                val page = document.getPage(pageIdx)
                val cropBox = page.cropBox ?: page.mediaBox
                val pdfWidth = cropBox.width
                val pdfHeight = cropBox.height

                val contentStream = PDPageContentStream(
                    document, page, PDPageContentStream.AppendMode.APPEND, true, true
                )

                // 1. Draw freehand ink & signature strokes onto PDF
                strokesByPage[pageIdx]?.forEach { stroke ->
                    if (stroke.pointsNorm.size > 1) {
                        contentStream.setStrokingColor(stroke.color.red, stroke.color.green, stroke.color.blue)
                        contentStream.setLineWidth(stroke.strokeWidth.coerceIn(1f, 10f))
                        val first = stroke.pointsNorm.first()
                        val startX = (first.x * pdfWidth).coerceIn(0f, pdfWidth)
                        val startY = (pdfHeight - (first.y * pdfHeight)).coerceIn(0f, pdfHeight)
                        contentStream.moveTo(startX, startY)
                        for (i in 1 until stroke.pointsNorm.size) {
                            val pt = stroke.pointsNorm[i]
                            val x = (pt.x * pdfWidth).coerceIn(0f, pdfWidth)
                            val y = (pdfHeight - (pt.y * pdfHeight)).coerceIn(0f, pdfHeight)
                            contentStream.lineTo(x, y)
                        }
                        contentStream.stroke()
                    }
                }

                // 2. Draw text annotations onto PDF
                textsByPage[pageIdx]?.forEach { textItem ->
                    val cleanText = sanitizePdfText(textItem.text.trim())
                    if (cleanText.isNotBlank()) {
                        val posX = (textItem.normX * pdfWidth).coerceIn(10f, pdfWidth - 30f)
                        val posY = (pdfHeight - (textItem.normY * pdfHeight)).coerceIn(10f, pdfHeight - 15f)

                        contentStream.beginText()
                        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14f)
                        contentStream.setNonStrokingColor(0f, 0f, 0f)
                        contentStream.newLineAtOffset(posX, posY)
                        contentStream.showText(cleanText)
                        contentStream.endText()
                    }
                }

                contentStream.close()
            }
        }

        val docsFolder = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "SilentPDF")
        if (!docsFolder.exists()) docsFolder.mkdirs()

        val baseName = pdf.fileName.substringBeforeLast(".")
        val outFile = File(docsFolder, "${baseName}_edited_${System.currentTimeMillis()}.pdf")
        document.save(outFile)
        document.close()
        pfd.close()

        return@withContext Pair(Uri.fromFile(outFile), outFile)
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext null
    }
}
