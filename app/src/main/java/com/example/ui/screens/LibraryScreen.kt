package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.PdfEntity
import com.example.ui.viewmodel.SilentPdfViewModel

fun sharePdf(context: Context, pdf: PdfEntity) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, Uri.parse(pdf.uriString))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share PDF"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: SilentPdfViewModel,
    onNavigateToReader: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val sortBy by viewModel.sortBy.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val pdfsList by viewModel.libraryPdfs.collectAsState()

    // Activity launcher for selecting PDF file locally
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                val contentResolver = context.contentResolver
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    // Ignore
                }

                var displayName = "Local_Document.pdf"
                var size = 0L

                try {
                    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                            if (nameIdx != -1) {
                                displayName = cursor.getString(nameIdx) ?: displayName
                            }
                            if (sizeIdx != -1) {
                                size = cursor.getLong(sizeIdx)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Fallback
                }

                viewModel.importPdf(uri, displayName, size)
                onNavigateToReader()
            }
        }
    )

    val recentlyReadPdf = remember(pdfsList) {
        pdfsList.filter { it.lastAccessTime > 0 && it.totalPages > 0 }
            .maxByOrNull { it.lastAccessTime }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Silent PDF", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.toggleGridView() }) {
                        Icon(
                            imageVector = if (isGridView) Icons.AutoMirrored.Outlined.ViewList else Icons.Outlined.GridView,
                            contentDescription = "Toggle View"
                        )
                    }
                    var showSortMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Outlined.Sort, contentDescription = "Sort")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("By Name") },
                                onClick = { viewModel.setSortBy(0); showSortMenu = false },
                                trailingIcon = { if (sortBy == 0) Icon(Icons.Default.Check, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("By Date") },
                                onClick = { viewModel.setSortBy(1); showSortMenu = false },
                                trailingIcon = { if (sortBy == 1) Icon(Icons.Default.Check, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("By Size") },
                                onClick = { viewModel.setSortBy(2); showSortMenu = false },
                                trailingIcon = { if (sortBy == 2) Icon(Icons.Default.Check, null) }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { filePickerLauncher.launch(arrayOf("application/pdf")) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add PDF")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search PDFs...") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Search")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { viewModel.setSelectedTab(0) },
                    text = { Text("All") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { viewModel.setSelectedTab(1) },
                    text = { Text("Recent") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { viewModel.setSelectedTab(2) },
                    text = { Text("Favorites") }
                )
            }

            if (isScanning) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // List Content
            if (pdfsList.isEmpty() && !isScanning) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No matches found" else "No PDFs found",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                if (isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(pdfsList, key = { it.uriString }) { pdf ->
                            GridPdfItem(
                                pdf = pdf,
                                onClick = {
                                    viewModel.openPdf(pdf)
                                    onNavigateToReader()
                                },
                                onFavoriteToggle = { viewModel.toggleFavorite(pdf) },
                                onShare = { sharePdf(context, pdf) },
                                onDelete = { viewModel.deletePdf(pdf) }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (recentlyReadPdf != null && searchQuery.isEmpty() && selectedTab == 0) {
                            item {
                                Text(
                                    "Continue Reading",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Card(
                                    onClick = {
                                        viewModel.openPdf(recentlyReadPdf)
                                        onNavigateToReader()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("recent_pdf_card")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(16.dp)
                                            .fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp, 64.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                        Column(
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = recentlyReadPdf.fileName,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Page ${recentlyReadPdf.lastPageRead + 1} of ${recentlyReadPdf.totalPages}",
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                            
                                            // Progress bar
                                            val progress = if (recentlyReadPdf.totalPages > 0) {
                                                (recentlyReadPdf.lastPageRead + 1).toFloat() / recentlyReadPdf.totalPages.toFloat()
                                            } else {
                                                0f
                                            }
                                            LinearProgressIndicator(
                                                progress = { progress },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 8.dp)
                                                    .height(4.dp),
                                                strokeCap = StrokeCap.Round
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "All Documents",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }

                        items(pdfsList, key = { it.uriString }) { pdf ->
                            ListPdfItem(
                                pdf = pdf,
                                onClick = {
                                    viewModel.openPdf(pdf)
                                    onNavigateToReader()
                                },
                                onFavoriteToggle = { viewModel.toggleFavorite(pdf) },
                                onShare = { sharePdf(context, pdf) },
                                onDelete = { viewModel.deletePdf(pdf) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ListPdfItem(
    pdf: PdfEntity,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "PDF",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = pdf.fileName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${formatFileSize(pdf.fileSize)} • ${formatRelativeTime(pdf.lastAccessTime)}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        IconButton(onClick = onFavoriteToggle) {
            Icon(
                imageVector = if (pdf.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = "Favorite Toggle",
                tint = if (pdf.isFavorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More Options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Share") },
                    onClick = {
                        showMenu = false
                        onShare()
                    },
                    leadingIcon = { Icon(Icons.Outlined.Share, null) }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        showMenu = false
                        onDelete()
                    },
                    leadingIcon = { Icon(Icons.Outlined.Delete, null) }
                )
            }
        }
    }
}

@Composable
fun GridPdfItem(
    pdf: PdfEntity,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp, 48.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "PDF",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row {
                    IconButton(
                        onClick = onFavoriteToggle,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (pdf.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Favorite Toggle",
                            tint = if (pdf.isFavorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Share") },
                                onClick = {
                                    showMenu = false
                                    onShare()
                                },
                                leadingIcon = { Icon(Icons.Outlined.Share, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                },
                                leadingIcon = { Icon(Icons.Outlined.Delete, null) }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = pdf.fileName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatFileSize(pdf.fileSize),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024f
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024f
    return String.format("%.1f MB", mb)
}

fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    if (diff < 0) return "Just now"
    val seconds = diff / 1000
    if (seconds < 60) return "Just now"
    val minutes = seconds / 60
    if (minutes < 60) return "$minutes min ago"
    val hours = minutes / 60
    if (hours < 24) return "$hours hours ago"
    val days = hours / 24
    if (days < 7) return "$days days ago"
    return java.text.DateFormat.getDateInstance().format(java.util.Date(timestamp))
}
