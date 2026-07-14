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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()

    // Dialog state management
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var folderNameInput by remember { mutableStateOf("") }
    var pdfToMoveToFolder by remember { mutableStateOf<PdfEntity?>(null) }
    var pdfToDelete by remember { mutableStateOf<PdfEntity?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    val isPinConfigured by viewModel.isPinConfigured.collectAsState()
    var showSecurityDialog by remember { mutableStateOf(false) }

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

    // Retrieve recent files (top 5 sorted by access time)
    val recentPdfs = remember(pdfsList) {
        pdfsList.filter { it.lastAccessTime > 0 }
            .sortedByDescending { it.lastAccessTime }
            .take(5)
    }

    if (showSecurityDialog) {
        SecurityDialog(
            isPinConfigured = isPinConfigured,
            onSetPin = { pin -> viewModel.setAppPin(pin) },
            onDisablePin = { viewModel.disableAppPin() },
            verifyPin = { pin -> viewModel.verifyPin(pin) },
            onDismiss = { showSecurityDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Silent PDF", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
                actions = {
                    IconButton(onClick = { viewModel.toggleGridView() }) {
                        Icon(
                            imageVector = if (isGridView) Icons.AutoMirrored.Outlined.ViewList else Icons.Outlined.GridView,
                            contentDescription = "Toggle View",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { showSecurityDialog = true }) {
                        Icon(
                            imageVector = if (isPinConfigured) Icons.Default.Lock else Icons.Outlined.Lock,
                            contentDescription = "Security Settings",
                            tint = if (isPinConfigured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Sort,
                            contentDescription = "Sort Options",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { filePickerLauncher.launch(arrayOf("application/pdf")) },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add PDF", tint = MaterialTheme.colorScheme.onPrimary)
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
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                placeholder = { Text("Search book name...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Search")
                        }
                    }
                },
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                )
            )

            // Dynamic Progress Scan
            if (isScanning) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Tabs Layout
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.padding(bottom = 8.dp),
                divider = { Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { viewModel.setSelectedTab(0) },
                    text = { Text("All", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { viewModel.setSelectedTab(1) },
                    text = { Text("Recent", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { viewModel.setSelectedTab(2) },
                    text = { Text("Favorites", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) }
                )
            }

            // main PDF items UI container
            if (pdfsList.isEmpty() && !isScanning) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No book found with this name" else "No PDF books",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Please try searching another word." else "Click the button below to import a local book.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    // 1. RECENT FILES CAROUSEL SECTION (Only on All/All Tab & when Search Query is empty)
                    if (selectedTab == 0 && searchQuery.isEmpty() && recentPdfs.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Recently viewed",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "${recentPdfs.size} books",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    items(recentPdfs, key = { "recent_${it.uriString}" }) { pdf ->
                                        RecentPdfCard(
                                            pdf = pdf,
                                            onClick = {
                                                viewModel.openPdf(pdf)
                                                onNavigateToReader()
                                            }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }

                    // 2. FOLDERS / CATEGORIES FILTER SECTION (Only on All/All Tab & when Search is empty)
                    if (selectedTab == 0 && searchQuery.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Folders / Categories",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    // "All" Folder Chip
                                    item {
                                        FilterChip(
                                            selected = selectedCategory == null,
                                            onClick = { viewModel.setSelectedCategory(null) },
                                            label = { Text("All") },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Outlined.FolderSpecial,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                    }

                                    // Dynamic User Folders Chips
                                    items(allCategories) { categoryName ->
                                        FilterChip(
                                            selected = selectedCategory == categoryName,
                                            onClick = { viewModel.setSelectedCategory(categoryName) },
                                            label = { Text(categoryName) },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Filled.Folder,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                    }

                                    // Create Folder Button Chip
                                    item {
                                        AssistChip(
                                            onClick = { showCreateFolderDialog = true },
                                            label = { Text("New Folder") },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            shape = RoundedCornerShape(16.dp),
                                            colors = AssistChipDefaults.assistChipColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                                labelColor = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }

                    // Header for the Document List
                    item {
                        Text(
                            text = if (selectedCategory != null) "Books in: $selectedCategory" else "All Books",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    // 3. MAIN PDF LIST OR GRID
                    if (isGridView) {
                        item {
                            // Render a Grid layout nested inside the LazyColumn to allow single scroll viewport
                            val chunkedPdfs = pdfsList.chunked(2)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                chunkedPdfs.forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        rowItems.forEach { pdf ->
                                            Box(modifier = Modifier.weight(1f)) {
                                                GridPdfItem(
                                                    pdf = pdf,
                                                    onClick = {
                                                        viewModel.openPdf(pdf)
                                                        onNavigateToReader()
                                                    },
                                                    onFavoriteToggle = { viewModel.toggleFavorite(pdf) },
                                                    onShare = { sharePdf(context, pdf) },
                                                    onDelete = { pdfToDelete = pdf },
                                                    onMoveToFolder = { pdfToMoveToFolder = pdf }
                                                )
                                            }
                                        }
                                        if (rowItems.size < 2) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        items(pdfsList, key = { it.uriString }) { pdf ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                ListPdfItem(
                                    pdf = pdf,
                                    onClick = {
                                        viewModel.openPdf(pdf)
                                        onNavigateToReader()
                                    },
                                    onFavoriteToggle = { viewModel.toggleFavorite(pdf) },
                                    onShare = { sharePdf(context, pdf) },
                                    onDelete = { pdfToDelete = pdf },
                                    onMoveToFolder = { pdfToMoveToFolder = pdf }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // CREATE FOLDER DIALOG
    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreateFolderDialog = false
                folderNameInput = ""
            },
            title = { Text("Create New Folder", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter the name of the folder you want to create to organize your books.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = folderNameInput,
                        onValueChange = { folderNameInput = it },
                        placeholder = { Text("Example: History Books") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (folderNameInput.isNotBlank()) {
                            viewModel.createCategory(folderNameInput)
                            showCreateFolderDialog = false
                            folderNameInput = ""
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreateFolderDialog = false
                    folderNameInput = ""
                }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // MOVE PDF TO FOLDER DIALOG
    if (pdfToMoveToFolder != null) {
        val pdf = pdfToMoveToFolder!!
        var customCategoryInput by remember { mutableStateOf("") }
        var selectedCategoryToMove by remember { mutableStateOf(pdf.category ?: "") }

        AlertDialog(
            onDismissRequest = { pdfToMoveToFolder = null },
            title = { Text("Move to Folder", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Select the folder you want to move '${pdf.fileName}' to:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Radio Button selection for existing categories
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // "Uncategorized/No folder" option
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedCategoryToMove = "" }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedCategoryToMove == "",
                                onClick = { selectedCategoryToMove = "" }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Default Folder", fontSize = 14.sp)
                        }

                        // Users dynamic categories
                        allCategories.forEach { cat ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedCategoryToMove = cat }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = selectedCategoryToMove == cat,
                                    onClick = { selectedCategoryToMove = cat }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(cat, fontSize = 14.sp)
                            }
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    // Text Field to create on-the-fly and move
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Or create a new folder:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = customCategoryInput,
                            onValueChange = {
                                customCategoryInput = it
                                if (it.isNotBlank()) selectedCategoryToMove = it.trim()
                            },
                            placeholder = { Text("Enter new name...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetCategory = if (customCategoryInput.isNotBlank()) {
                            val newCat = customCategoryInput.trim()
                            viewModel.createCategory(newCat) // Save it persistently
                            newCat
                        } else if (selectedCategoryToMove.isNotBlank()) {
                            selectedCategoryToMove
                        } else {
                            null
                        }
                        viewModel.updatePdfCategory(pdf, targetCategory)
                        pdfToMoveToFolder = null
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { pdfToMoveToFolder = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // SORTING DIALOG

    // DELETE CONFIRMATION DIALOG
    if (pdfToDelete != null) {
        val pdf = pdfToDelete!!
        AlertDialog(
            onDismissRequest = { pdfToDelete = null },
            title = { Text("Delete PDF", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${pdf.fileName}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePdf(pdf)
                        pdfToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("OK", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { pdfToDelete = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
    if (showSortMenu) {
        AlertDialog(
            onDismissRequest = { showSortMenu = false },
            title = { Text("Sort Books", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val options = listOf(
                        Triple(0, "A-Z (Name)", Icons.AutoMirrored.Outlined.Sort),
                        Triple(1, "Recent (Date)", Icons.Outlined.CalendarToday),
                        Triple(2, "Book Size", Icons.Outlined.FilePresent)
                    )

                    options.forEach { (index, title, icon) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.setSortBy(index)
                                    showSortMenu = false
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (sortBy == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = title,
                                modifier = Modifier.weight(1f),
                                fontWeight = if (sortBy == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (sortBy == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            RadioButton(
                                selected = sortBy == index,
                                onClick = {
                                    viewModel.setSortBy(index)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSortMenu = false }) {
                    Text("Close")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

// HORIZONTAL CARD COMPONENT FOR RECENT FILES
@Composable
fun RecentPdfCard(
    pdf: PdfEntity,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(220.dp)
            .height(115.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        )
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp, 38.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "PDF",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pdf.fileName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formatFileSize(pdf.fileSize),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                val lastPage = pdf.lastPageRead
                val total = pdf.totalPages
                val progressText = if (total > 0) "Page ${lastPage + 1} of $total" else "Recent page"
                val progressVal = if (total > 0) (lastPage + 1).toFloat() / total.toFloat() else 0f

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = progressText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = formatRelativeTime(pdf.lastAccessTime),
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progressVal },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
            }
        }
    }
}

// DETAILED COMPONENT FOR LIST PDF ITEMS
@Composable
fun ListPdfItem(
    pdf: PdfEntity,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onMoveToFolder: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PDF",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
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
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${formatFileSize(pdf.fileSize)} • ${formatRelativeTime(pdf.lastAccessTime)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (pdf.category != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = pdf.category,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            IconButton(onClick = onFavoriteToggle) {
                Icon(
                    imageVector = if (pdf.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Favorite Toggle",
                    tint = if (pdf.isFavorite) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
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
                        text = { Text("Move to Folder") },
                        onClick = {
                            showMenu = false
                            onMoveToFolder()
                        },
                        leadingIcon = { Icon(Icons.Outlined.Folder, null, tint = MaterialTheme.colorScheme.primary) }
                    )
                    DropdownMenuItem(
                        text = { Text("Share") },
                        onClick = {
                            showMenu = false
                            onShare()
                        },
                        leadingIcon = { Icon(Icons.Outlined.Share, null, tint = MaterialTheme.colorScheme.primary) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        }
    }
}

// DETAILED COMPONENT FOR GRID PDF ITEMS
@Composable
fun GridPdfItem(
    pdf: PdfEntity,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onMoveToFolder: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
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
                        .size(36.dp, 44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "PDF",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onFavoriteToggle,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (pdf.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Favorite Toggle",
                            tint = if (pdf.isFavorite) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Move to Folder") },
                                onClick = {
                                    showMenu = false
                                    onMoveToFolder()
                                },
                                leadingIcon = { Icon(Icons.Outlined.Folder, null, tint = MaterialTheme.colorScheme.primary) }
                            )
                            DropdownMenuItem(
                                text = { Text("Share") },
                                onClick = {
                                    showMenu = false
                                    onShare()
                                },
                                leadingIcon = { Icon(Icons.Outlined.Share, null, tint = MaterialTheme.colorScheme.primary) }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                },
                                leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatFileSize(pdf.fileSize),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (pdf.category != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = pdf.category,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SecurityDialog(
    isPinConfigured: Boolean,
    onSetPin: (String) -> Unit,
    onDisablePin: () -> Unit,
    verifyPin: (String) -> Boolean,
    onDismiss: () -> Unit
) {
    var pinText by remember { mutableStateOf("") }
    var confirmPinText by remember { mutableStateOf("") }
    var currentPinInput by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(if (isPinConfigured) 0 else 1) } // 0: options/disable, 1: enter pin, 2: confirm pin
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when {
                    isPinConfigured && step == 0 -> "App Security"
                    step == 1 -> "Set PIN"
                    step == 2 -> "Confirm PIN"
                    else -> "Enter PIN"
                },
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isPinConfigured && step == 0) {
                    Text(
                        text = "App PIN is currently active. Your documents are safe.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (error != null) {
                        Text(
                            text = error ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    OutlinedTextField(
                        value = currentPinInput,
                        onValueChange = {
                            if (it.length <= 4) {
                                currentPinInput = it
                                error = null
                            }
                        },
                        label = { Text("Enter PIN to disable") },
                        placeholder = { Text("4 digits") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else if (step == 1) {
                    Text(
                        text = "Please enter a 4-digit PIN to lock the app.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = pinText,
                        onValueChange = {
                            if (it.length <= 4) {
                                pinText = it
                                error = null
                            }
                        },
                        label = { Text("New PIN") },
                        placeholder = { Text("4 digits") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else if (step == 2) {
                    Text(
                        text = "Repeat the PIN you just entered to confirm.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (error != null) {
                        Text(
                            text = error ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    OutlinedTextField(
                        value = confirmPinText,
                        onValueChange = {
                            if (it.length <= 4) {
                                confirmPinText = it
                                error = null
                            }
                        },
                        label = { Text("Confirm PIN") },
                        placeholder = { Text("4 digits") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isPinConfigured && step == 0) {
                        if (verifyPin(currentPinInput)) {
                            onDisablePin()
                            onDismiss()
                        } else {
                            error = "The PIN you entered is incorrect!"
                            currentPinInput = ""
                        }
                    } else if (step == 1) {
                        if (pinText.length == 4) {
                            step = 2
                        } else {
                            error = "Please enter 4 digits."
                        }
                    } else if (step == 2) {
                        if (confirmPinText == pinText) {
                            onSetPin(pinText)
                            onDismiss()
                        } else {
                            error = "The PINs you entered do not match!"
                            confirmPinText = ""
                        }
                    }
                },
                enabled = when {
                    isPinConfigured && step == 0 -> currentPinInput.length == 4
                    step == 1 -> pinText.length == 4
                    step == 2 -> confirmPinText.length == 4
                    else -> false
                }
            ) {
                Text(
                    text = when {
                        isPinConfigured && step == 0 -> "Dami"
                        step == 1 -> "Sii soco"
                        step == 2 -> "Kaydi"
                        else -> "Xaqiiji"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
