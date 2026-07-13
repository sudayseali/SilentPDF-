package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var viewWidth by remember { mutableStateOf(1080) }

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
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
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
                } else {
                    pageBitmap?.let { bitmap ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
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
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "PDF Page",
                                colorFilter = if (isTrueDarkMode) ColorFilter.colorMatrix(invertColorMatrix) else null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                        translationX = offset.x
                                        translationY = offset.y
                                    }
                            )
                        }
                    } ?: run {
                        if (pageCount > 0) {
                            Text("Failed to render page", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}
