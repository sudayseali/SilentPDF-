cat << 'INNER_EOF' > app/src/main/java/com/example/ui/screens/ReaderScreen.kt
package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.DrawingStroke
import com.example.ui.viewmodel.SilentPdfViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    viewModel: SilentPdfViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val currentPdf by viewModel.currentPdf.collectAsState()
    val pageCount by viewModel.pageCount.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val pageBitmap by viewModel.currentPageBitmap.collectAsState()
    val isTrueDarkMode by viewModel.isTrueDarkMode.collectAsState()
    val isPdfLoading by viewModel.isPdfLoading.collectAsState()
    val bookmarks by viewModel.currentBookmarks.collectAsState()
    val pageDrawings by viewModel.pageDrawings.collectAsState()

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var viewWidth by remember { mutableStateOf(1080) }

    var isDrawingMode by remember { mutableStateOf(false) }
    var selectedColor by remember { mutableStateOf(Color(0xFFF44336)) } // Default Red
    var strokeWidth by remember { mutableStateOf(8f) }
    var isEraserMode by remember { mutableStateOf(false) }
    
    val colors = listOf(
        Color(0xFFF44336), // Red
        Color(0xFF2196F3), // Blue
        Color(0xFF4CAF50), // Green
        Color(0xFFFFEB3B), // Yellow
        Color(0xFFFF9800), // Orange
        Color(0xFF9C27B0), // Purple
        Color(0xFF000000), // Black
        Color(0xFFFFFFFF)  // White
    )
    var currentStroke by remember { mutableStateOf<DrawingStroke?>(null) }

    val invertColorMatrix = remember {
        ColorMatrix(floatArrayOf(
            -1f,  0f,  0f,  0f, 255f, 
             0f, -1f,  0f,  0f, 255f, 
             0f,  0f, -1f,  0f, 255f, 
             0f,  0f,  0f,  1f,   0f
        ))
    }

    LaunchedEffect(currentPage) {
        scale = 1f
        offset = Offset.Zero
        currentStroke = null
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(320.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Navigation",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    var selectedDrawerTab by remember { mutableStateOf(1) }
                    TabRow(
                        selectedTabIndex = selectedDrawerTab,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Tab(
                            selected = selectedDrawerTab == 0,
                            onClick = { selectedDrawerTab = 0 },
                            text = { Text("Outline") }
                        )
                        Tab(
                            selected = selectedDrawerTab == 1,
                            onClick = { selectedDrawerTab = 1 },
                            text = { Text("Bookmarks (${bookmarks.size})") }
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        if (selectedDrawerTab == 1) {
                            if (bookmarks.isEmpty()) {
                                item {
                                    Text("No bookmarks added yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                items(bookmarks) { bookmark ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.jumpToPage(bookmark.pageNumber, viewWidth)
                                                coroutineScope.launch { drawerState.close() }
                                            }
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text("Page ${bookmark.pageNumber + 1}", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        } else {
                            item {
                                Text("No outline available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = currentPdf?.fileName ?: "Loading...",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { isDrawingMode = !isDrawingMode }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Draw",
                                tint = if (isDrawingMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { viewModel.toggleTrueDarkMode() }) {
                            Icon(Icons.Outlined.Contrast, contentDescription = "Toggle Dark Mode")
                        }
                        val isBookmarked = bookmarks.any { it.pageNumber == currentPage }
                        IconButton(onClick = { viewModel.toggleBookmarkCurrentPage() }) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(Icons.Outlined.FormatListNumbered, contentDescription = "Outline")
                        }
                    }
                )
            },
            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isDrawingMode) {
                            // Drawing Toolbar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    IconButton(
                                        onClick = { isEraserMode = false },
                                        modifier = Modifier.background(
                                            color = if (!isEraserMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                            shape = CircleShape
                                        )
                                    ) {
                                        Icon(
                                            Icons.Default.Brush,
                                            contentDescription = "Pen",
                                            tint = if (!isEraserMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    IconButton(
                                        onClick = { isEraserMode = true },
                                        modifier = Modifier.background(
                                            color = if (isEraserMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                            shape = CircleShape
                                        )
                                    ) {
                                        Icon(
                                            Icons.Default.AutoFixHigh,
                                            contentDescription = "Eraser",
                                            tint = if (isEraserMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                
                                var showColorMenu by remember { mutableStateOf(false) }
                                Box {
                                    IconButton(
                                        onClick = { showColorMenu = true },
                                        enabled = !isEraserMode
                                    ) {
                                        Icon(
                                            Icons.Default.Palette, 
                                            contentDescription = "Color", 
                                            tint = if (isEraserMode) Color.Gray else selectedColor
                                        )
                                    }
                                    DropdownMenu(expanded = showColorMenu, onDismissRequest = { showColorMenu = false }) {
                                        colors.forEach { color ->
                                            DropdownMenuItem(
                                                text = { Text("Select") },
                                                leadingIcon = {
                                                    Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(color).border(1.dp, Color.Gray, CircleShape))
                                                },
                                                onClick = {
                                                    selectedColor = color
                                                    showColorMenu = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    IconButton(onClick = { 
                                        currentPdf?.uriString?.let { uri ->
                                            viewModel.undoLastStroke(uri, currentPage)
                                        }
                                    }) {
                                        Icon(Icons.Default.Undo, contentDescription = "Undo")
                                    }
                                }
                            }
                            
                            // Stroke width slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LineWeight, contentDescription = "Thickness", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(8.dp))
                                Slider(
                                    value = strokeWidth,
                                    onValueChange = { strokeWidth = it },
                                    valueRange = 2f..40f,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        }

                        // Navigation Controls
                        if (pageCount > 1) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "1",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Slider(
                                    value = currentPage.toFloat(),
                                    onValueChange = { viewModel.jumpToPage(it.toInt(), viewWidth) },
                                    valueRange = 0f..(pageCount - 1).toFloat(),
                                    steps = (pageCount - 2).coerceAtLeast(0),
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 12.dp)
                                        .testTag("page_slider")
                                )
                                Text(
                                    text = "$pageCount",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.jumpToPage(currentPage - 1, viewWidth) },
                                enabled = currentPage > 0,
                                modifier = Modifier.background(
                                    color = if (currentPage > 0) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                                    shape = CircleShape
                                )
                            ) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Page")
                            }

                            var showJumpDialog by remember { mutableStateOf(false) }
                            Text(
                                text = "Page ${currentPage + 1} of $pageCount",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showJumpDialog = true }
                                    .padding(8.dp)
                            )
                            if (showJumpDialog) {
                                var jumpText by remember { mutableStateOf("") }
                                AlertDialog(
                                    onDismissRequest = { showJumpDialog = false },
                                    title = { Text("Go to Page") },
                                    text = {
                                        OutlinedTextField(
                                            value = jumpText,
                                            onValueChange = { jumpText = it.filter { char -> char.isDigit() } },
                                            label = { Text("Page Number (1 - $pageCount)") },
                                            singleLine = true
                                        )
                                    },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                val page = jumpText.toIntOrNull()
                                                if (page != null && page in 1..pageCount) {
                                                    viewModel.jumpToPage(page - 1, viewWidth)
                                                }
                                                showJumpDialog = false
                                            }
                                        ) {
                                            Text("Go")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showJumpDialog = false }) {
                                            Text("Cancel")
                                        }
                                    }
                                )
                            }

                            IconButton(
                                onClick = { viewModel.jumpToPage(currentPage + 1, viewWidth) },
                                enabled = currentPage < pageCount - 1,
                                modifier = Modifier.background(
                                    color = if (currentPage < pageCount - 1) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                                    shape = CircleShape
                                )
                            ) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Next Page")
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(if (isTrueDarkMode) Color.Black else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .onSizeChanged { size ->
                        if (size.width > 0 && viewWidth != size.width) {
                            viewWidth = size.width
                            viewModel.renderCurrentPage(size.width)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isPdfLoading) {
                    CircularProgressIndicator()
                } else if (pageBitmap != null) {
                    val bitmap = pageBitmap!!
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(isDrawingMode) {
                                if (isDrawingMode) {
                                    detectDragGestures(
                                        onDragStart = { startOffset ->
                                            val centerX = size.width / 2f
                                            val centerY = size.height / 2f
                                            val unscaledX = (startOffset.x - offset.x - centerX) / scale + centerX
                                            val unscaledY = (startOffset.y - offset.y - centerY) / scale + centerY
                                            currentStroke = DrawingStroke(
                                                points = listOf(Offset(unscaledX, unscaledY)),
                                                color = selectedColor,
                                                width = strokeWidth / scale,
                                                isEraser = isEraserMode
                                            )
                                        },
                                        onDrag = { change, _ ->
                                            val centerX = size.width / 2f
                                            val centerY = size.height / 2f
                                            val unscaledX = (change.position.x - offset.x - centerX) / scale + centerX
                                            val unscaledY = (change.position.y - offset.y - centerY) / scale + centerY
                                            currentStroke = currentStroke?.copy(
                                                points = currentStroke!!.points + Offset(unscaledX, unscaledY)
                                            )
                                        },
                                        onDragEnd = {
                                            currentStroke?.let { stroke ->
                                                if (stroke.points.size > 1) {
                                                    currentPdf?.uriString?.let { uri ->
                                                        viewModel.addStroke(uri, currentPage, stroke)
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
                                        scale = (scale * zoom).coerceIn(1f, 4f)
                                        if (scale > 1f) {
                                            offset = Offset(
                                                x = offset.x + pan.x,
                                                y = offset.y + pan.y
                                            )
                                        } else {
                                            offset = Offset.Zero
                                        }
                                    }
                                }
                            }
                    ) {
                        val layerModifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offset.x
                                translationY = offset.y
                                // Need compositing strategy offscreen for BlendMode.Clear to work on this layer ONLY
                                compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen
                            }

                        Box(modifier = layerModifier) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "PDF Page",
                                colorFilter = if (isTrueDarkMode) ColorFilter.colorMatrix(invertColorMatrix) else null,
                                modifier = Modifier.fillMaxSize()
                            )
                            
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val pdfUri = currentPdf?.uriString ?: return@Canvas
                                val strokes = pageDrawings[pdfUri]?.get(currentPage) ?: emptyList()
                                
                                val drawStroke: androidx.compose.ui.graphics.drawscope.DrawScope.(DrawingStroke) -> Unit = { stroke ->
                                    if (stroke.points.size > 1) {
                                        val path = androidx.compose.ui.graphics.Path().apply {
                                            moveTo(stroke.points.first().x, stroke.points.first().y)
                                            for (i in 1 until stroke.points.size) {
                                                lineTo(stroke.points[i].x, stroke.points[i].y)
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

                                strokes.forEach { it.drawStroke() }
                                currentStroke?.let { it.drawStroke() }
                            }
                        }
                    }
                } else if (pageCount > 0) {
                    Text("Failed to render page", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
INNER_EOF
