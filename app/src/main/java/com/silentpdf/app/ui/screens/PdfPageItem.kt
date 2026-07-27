package com.silentpdf.app.ui.screens

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.graphics.graphicsLayer
import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silentpdf.app.bionic.BionicConfig
import com.silentpdf.app.bionic.ProcessedBionicPage
import com.silentpdf.app.data.db.PdfEntity
import com.silentpdf.app.data.db.NoteEntity
import com.silentpdf.app.ui.viewmodel.DrawingStroke
import com.silentpdf.app.ui.viewmodel.SilentPdfViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun PdfPageItem(
    pageIndex: Int,
    targetWidth: Int,
    viewModel: SilentPdfViewModel,
    bionicConfig: BionicConfig,
    pageBackgroundColor: Color,
    pageColorFilter: ColorFilter?,
    pageDrawings: Map<String, Map<Int, List<DrawingStroke>>>,
    currentNotes: List<NoteEntity>,
    isDrawingMode: Boolean,
    isHighlighterMode: Boolean,
    isEraserMode: Boolean,
    selectedColor: Color,
    strokeWidth: Float,
    currentPdf: PdfEntity?,
    readerOnSurfaceColor: Color,
    onOpenBionicSettings: () -> Unit,
    onNoteClick: (String) -> Unit,
    onNoteDelete: (Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var currentStroke by remember { mutableStateOf<DrawingStroke?>(null) }

    val searchInPdfResults by viewModel.pdfSearchResults.collectAsState()
    val activeSearchMatchIndex by viewModel.activeSearchMatchIndex.collectAsState()

    LaunchedEffect(pageIndex, targetWidth) {
        if (targetWidth > 0) {
            launch(Dispatchers.IO) {
                val newBitmap = viewModel.getPageBitmap(pageIndex, targetWidth)
                bitmap = newBitmap
            }
        }
    }

    if (bionicConfig.isEnabled && bitmap != null) {
        val pageText by viewModel.openedPdfTextPages.collectAsState()
        val text = pageText.getOrNull(pageIndex) ?: ""
        var processedPage by remember(pageIndex, bionicConfig, text, bitmap) { mutableStateOf<ProcessedBionicPage?>(null) }

        LaunchedEffect(pageIndex, bionicConfig, text, bitmap) {
            processedPage = viewModel.processBionicPage(
                pdfUri = currentPdf?.uriString ?: "",
                pageIndex = pageIndex,
                rawText = text,
                bitmap = bitmap!!,
                textColor = readerOnSurfaceColor
            )
        }

        Box(modifier = modifier.fillMaxWidth().padding(16.dp)) {
            BionicText(
                processedPage = processedPage,
                fallbackText = text,
                config = bionicConfig,
                textColor = readerOnSurfaceColor,
                onOpenSettings = onOpenBionicSettings
            )
        }
    } else {
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(pageBackgroundColor)
                .pointerInput(isDrawingMode) {
                    if (isDrawingMode) {
                        detectDragGestures(
                            onDragStart = { startOffset ->
                                currentStroke = DrawingStroke(
                                    points = listOf(startOffset),
                                    color = if (isHighlighterMode) selectedColor.copy(alpha = 0.4f) else selectedColor,
                                    width = strokeWidth,
                                    isEraser = isEraserMode
                                )
                            },
                            onDrag = { change, _ ->
                                currentStroke = currentStroke?.copy(
                                    points = currentStroke!!.points + change.position
                                )
                            },
                            onDragEnd = {
                                currentStroke?.let { stroke ->
                                    if (stroke.points.size > 1) {
                                        currentPdf?.uriString?.let { uri ->
                                            viewModel.addStroke(uri, pageIndex, stroke)
                                        }
                                    }
                                }
                                currentStroke = null
                            },
                            onDragCancel = {
                                currentStroke = null
                            }
                        )
                    } else {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(1f, 5f)
                            scale = newScale
                            
                            if (scale == 1f) {
                                offset = androidx.compose.ui.geometry.Offset.Zero
                            } else {
                                val newOffset = offset + pan * scale
                                offset = newOffset
                            }
                        }
                    }
                }
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                ),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "PDF Page ${pageIndex + 1}",
                    colorFilter = pageColorFilter,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth
                )

                Canvas(modifier = Modifier.matchParentSize()) {
                    val pdfUri = currentPdf?.uriString ?: return@Canvas
                    val strokes = pageDrawings[pdfUri]?.get(pageIndex) ?: emptyList()
                    
                    val drawStroke = { stroke: DrawingStroke ->
                        if (stroke.points.size > 1) {
                            val path = androidx.compose.ui.graphics.Path().apply {
                                val points = stroke.points
                                moveTo(points.first().x, points.first().y)
                                if (points.size > 2) {
                                    var currentX = points[0].x
                                    var currentY = points[0].y
                                    for (i in 1 until points.size - 1) {
                                        val nextX = points[i].x
                                        val nextY = points[i].y
                                        val midX = (currentX + nextX) / 2f
                                        val midY = (currentY + nextY) / 2f
                                        quadraticBezierTo(currentX, currentY, midX, midY)
                                        currentX = nextX
                                        currentY = nextY
                                    }
                                    lineTo(points.last().x, points.last().y)
                                } else {
                                    lineTo(points.last().x, points.last().y)
                                }
                            }
                            drawPath(
                                path = path,
                                color = if (stroke.isEraser) Color.Transparent else stroke.color,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = stroke.width,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                ),
                                blendMode = if (stroke.isEraser) androidx.compose.ui.graphics.BlendMode.Clear else androidx.compose.ui.graphics.BlendMode.SrcOver
                            )
                        }
                    }

                    strokes.forEach(drawStroke)
                    currentStroke?.let(drawStroke)

                    // Draw search highlights
                    searchInPdfResults.forEachIndexed { index, result ->
                        if (result.pageNumber == pageIndex) {
                            val isActive = index == activeSearchMatchIndex
                            val highlightColor = if (isActive) Color(0x66FF9800) else Color(0x33FFEB3B)
                            result.bounds.forEach { rect ->
                                val highlightRect = androidx.compose.ui.geometry.Rect(rect.left * size.width, rect.top * size.height, rect.right * size.width, rect.bottom * size.height)
                                drawRect(color = highlightColor, topLeft = highlightRect.topLeft, size = highlightRect.size)
                            }
                        }
                    }
                }
            } else {
                CircularProgressIndicator(modifier = Modifier.padding(32.dp))
            }

            // Note overlay
            val currentPageNote = currentNotes.find { it.pageNumber == pageIndex }
            if (currentPageNote != null) {
                val audioPath = if (currentPageNote.noteText.startsWith("[audio:")) {
                    currentPageNote.noteText.substringAfter("[audio:").substringBefore("]")
                } else null
                val cleanText = if (currentPageNote.noteText.startsWith("[audio:")) {
                    currentPageNote.noteText.substringAfter("]").trim()
                } else currentPageNote.noteText

                Box(
                    modifier = Modifier.matchParentSize().padding(16.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    if (audioPath != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                VoiceNotePlayer(
                                    filePath = audioPath,
                                    onDelete = { onNoteDelete(pageIndex, cleanText) }
                                )
                                if (cleanText.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = cleanText,
                                        fontSize = 11.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 8.dp).clickable {
                                            onNoteClick(cleanText)
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFF59D).copy(alpha = 0.95f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            modifier = Modifier.widthIn(max = 200.dp).clickable {
                                onNoteClick(cleanText)
                            }
                        ) {
                            Text(
                                text = cleanText,
                                color = Color.Black,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
