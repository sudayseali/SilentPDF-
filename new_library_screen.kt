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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    if (diff < 0) return "Today"
    val seconds = diff / 1000
    if (seconds < 60) return "Just now"
    val minutes = seconds / 60
    if (minutes < 60) return "Today"
    val hours = minutes / 60
    if (hours < 24) return "Today"
    val days = hours / 24
    if (days < 2) return "Yesterday"
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

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var folderNameInput by remember { mutableStateOf("") }
    var pdfToMoveToFolder by remember { mutableStateOf<PdfEntity?>(null) }
    var pdfToDelete by remember { mutableStateOf<PdfEntity?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    val isPinConfigured by viewModel.isPinConfigured.collectAsState()
    var showSecurityDialog by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }

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

    val recentPdfs = remember(pdfsList) {
        pdfsList.filter { it.lastAccessTime > 0 }
            .sortedByDescending { it.lastAccessTime }
            .take(6)
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
        containerColor = Color(0xFFF5F7FA), // Light grey background
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { viewModel.setSelectedTab(0) },
                    icon = { Icon(Icons.Filled.Description, contentDescription = null) },
                    label = { Text("Library", fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF1E88E5),
                        selectedTextColor = Color(0xFF1E88E5),
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { viewModel.setSelectedTab(1) },
                    icon = { Icon(Icons.Outlined.Schedule, contentDescription = null) },
                    label = { Text("Recents", fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF1E88E5),
                        selectedTextColor = Color(0xFF1E88E5),
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { viewModel.setSelectedTab(2) },
                    icon = { Icon(Icons.Outlined.StarBorder, contentDescription = null) },
                    label = { Text("Favorites", fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF1E88E5),
                        selectedTextColor = Color(0xFF1E88E5),
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { showSettingsSheet = true },
                    icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                    label = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF1E88E5),
                        selectedTextColor = Color(0xFF1E88E5),
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { filePickerLauncher.launch(arrayOf("application/pdf")) },
                containerColor = Color(0xFF1E88E5),
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add PDF", tint = Color.White)
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Top Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.Black)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E88E5)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Description, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Silent", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color.Black)
                        Text("PDF", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color(0xFF1E88E5))
                    }
                    IconButton(onClick = { /* Premium feature */ }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Outlined.WorkspacePremium, contentDescription = "Premium", tint = Color.Black)
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search PDF files...", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Outlined.Search, null, tint = Color.Gray) },
                    trailingIcon = {
                        Icon(Icons.Outlined.FilterList, null, tint = Color.Gray)
                    },
                    shape = CircleShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent
                    ),
                    singleLine = true
                )
                if (isScanning) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        color = Color(0xFF1E88E5)
                    )
                }
            }

            // 4 Stat Cards
            if (searchQuery.isEmpty() && selectedTab == 0) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatCard(
                            icon = Icons.Filled.Description,
                            title = "All Files",
                            count = pdfsList.size.toString(),
                            color = Color(0xFF1E88E5),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        StatCard(
                            icon = Icons.Filled.Schedule,
                            title = "Recent",
                            count = recentPdfs.size.toString(),
                            color = Color(0xFF1E88E5),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        StatCard(
                            icon = Icons.Filled.Star,
                            title = "Favorites",
                            count = pdfsList.count { it.isFavorite }.toString(),
                            color = Color(0xFFFFB300),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        StatCard(
                            icon = Icons.Filled.Folder,
                            title = "Folders",
                            count = allCategories.size.toString(),
                            color = Color(0xFF1E88E5),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Main PDF Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (selectedTab) {
                            1 -> "Recent Documents"
                            2 -> "Favorite Documents"
                            else -> if (searchQuery.isNotEmpty()) "Search Results" else "Recent Documents"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.Black
                    )
                    Text("View all", color = Color(0xFF1E88E5), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // PDF Grid (3 columns)
            item {
                val displayList = if (selectedTab == 0 && searchQuery.isEmpty()) recentPdfs else pdfsList
                
                if (displayList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No PDFs found.", color = Color.Gray)
                    }
                } else {
                    val chunkedPdfs = displayList.chunked(3)
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
                                // Fill empty spaces if row < 3 items
                                for (i in rowItems.size until 3) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // Folders Section
            if (searchQuery.isEmpty() && selectedTab == 0) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Folders", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                        Text("View all", color = Color(0xFF1E88E5), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // User folders
                        items(allCategories) { category ->
                            val count = pdfsList.count { it.category == category }
                            FolderCard(
                                name = category,
                                count = count,
                                onClick = { viewModel.setSelectedCategory(category); viewModel.setSelectedTab(0) },
                                onMoreClick = {}
                            )
                        }
                        // Default / All folder
                        item {
                            FolderCard(
                                name = "All Books",
                                count = pdfsList.size,
                                onClick = { viewModel.setSelectedCategory(null); viewModel.setSelectedTab(0) },
                                onMoreClick = {}
                            )
                        }
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                modifier = Modifier.width(120.dp).clickable { showCreateFolderDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Add, null, tint = Color(0xFF1E88E5))
                                    Spacer(Modifier.width(8.dp))
                                    Text("New Folder", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // BOTTOM SHEET FOR SETTINGS
    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            containerColor = Color.White
        ) {
            Column(modifier = Modifier.padding(16.dp).padding(bottom = 32.dp)) {
                Text("Settings", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(bottom = 16.dp))
                
                ListItem(
                    headlineContent = { Text("App Security (PIN)") },
                    leadingContent = { Icon(Icons.Outlined.Lock, null) },
                    modifier = Modifier.clickable { showSettingsSheet = false; showSecurityDialog = true }
                )
                ListItem(
                    headlineContent = { Text("Help / Support") },
                    leadingContent = { Icon(Icons.Outlined.SupportAgent, null) },
                    modifier = Modifier.clickable { showSettingsSheet = false; showSupportDialog = true }
                )
                ListItem(
                    headlineContent = { Text("Sort Options") },
                    leadingContent = { Icon(Icons.Outlined.Sort, null) },
                    modifier = Modifier.clickable { showSettingsSheet = false; showSortMenu = true }
                )
                ListItem(
                    headlineContent = { Text("Toggle View Mode") },
                    leadingContent = { Icon(if (isGridView) Icons.Outlined.ViewList else Icons.Outlined.GridView, null) },
                    modifier = Modifier.clickable { showSettingsSheet = false; viewModel.toggleGridView() }
                )
            }
        }
    }

    // ALL THE DIALOGS
    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false; folderNameInput = "" },
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
                TextButton(onClick = { showCreateFolderDialog = false; folderNameInput = "" }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

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
                    Text("Select the folder you want to move '${pdf.fileName}' to:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp).verticalScroll(rememberScrollState())
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { selectedCategoryToMove = "" }.padding(vertical = 4.dp)
                        ) {
                            RadioButton(selected = selectedCategoryToMove == "", onClick = { selectedCategoryToMove = "" })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Default Folder", fontSize = 14.sp)
                        }
                        allCategories.forEach { cat ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable { selectedCategoryToMove = cat }.padding(vertical = 4.dp)
                            ) {
                                RadioButton(selected = selectedCategoryToMove == cat, onClick = { selectedCategoryToMove = cat })
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(cat, fontSize = 14.sp)
                            }
                        }
                    }
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
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
                            viewModel.createCategory(newCat)
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
                TextButton(onClick = { pdfToMoveToFolder = null }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showSupportDialog) {
        AlertDialog(
            onDismissRequest = { showSupportDialog = false },
            title = { Text("Help / Support", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("If you encounter any issues, please contact us via WhatsApp.")
                    Text(
                        "Note: Only chat messages are allowed. Voice and video calls are not permitted.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSupportDialog = false
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.data = Uri.parse("https://wa.me/252657864155?text=Hello,%20I%20need%20help%20with%20the%20SilentPDF%20app.")
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "WhatsApp is not installed.", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("WhatsApp Chat")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSupportDialog = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (pdfToDelete != null) {
        val pdf = pdfToDelete!!
        AlertDialog(
            onDismissRequest = { pdfToDelete = null },
            title = { Text("Delete PDF", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${pdf.fileName}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.deletePdf(pdf); pdfToDelete = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("OK", color = MaterialTheme.colorScheme.onError) }
            },
            dismissButton = {
                TextButton(onClick = { pdfToDelete = null }) { Text("Cancel") }
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
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { viewModel.setSortBy(index); showSortMenu = false }.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = icon, contentDescription = null, tint = if (sortBy == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = title, modifier = Modifier.weight(1f), fontWeight = if (sortBy == index) FontWeight.Bold else FontWeight.Normal, color = if (sortBy == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                            RadioButton(selected = sortBy == index, onClick = { viewModel.setSortBy(index); showSortMenu = false })
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showSortMenu = false }) { Text("Close") } },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun StatCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, count: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.height(90.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
            Text(count, fontSize = 11.sp, color = Color.Gray)
        }
    }
}

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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column {
            // Simulated Book Cover
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.85f)
                    .background(Brush.verticalGradient(listOf(Color(0xFF2C3E50), Color(0xFF1A252F))))
            ) {
                // PDF Badge
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .background(Color(0xFFD32F2F), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text("PDF", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
                
                // Menu Button
                IconButton(
                    onClick = { showMenu = true }, 
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp)
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }

                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Move to Folder") },
                        onClick = { showMenu = false; onMoveToFolder() },
                        leadingIcon = { Icon(Icons.Outlined.Folder, null, tint = MaterialTheme.colorScheme.primary) }
                    )
                    DropdownMenuItem(
                        text = { Text("Share") },
                        onClick = { showMenu = false; onShare() },
                        leadingIcon = { Icon(Icons.Outlined.Share, null, tint = MaterialTheme.colorScheme.primary) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = { showMenu = false; onDelete() },
                        leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) }
                    )
                }

                // Title snippet on cover
                Text(
                    text = pdf.fileName.removeSuffix(".pdf").take(20),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.align(Alignment.Center).padding(horizontal = 8.dp),
                    textAlign = TextAlign.Center
                )
            }
            
            // Bottom Info
            Column(modifier = Modifier.padding(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = pdf.fileName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (pdf.isFavorite) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFF1E88E5), modifier = Modifier.size(14.dp))
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${formatFileSize(pdf.fileSize)} • ${formatRelativeTime(pdf.lastAccessTime)}",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun FolderCard(name: String, count: Int, onClick: () -> Unit, onMoreClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.width(130.dp).clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Folder, contentDescription = null, tint = Color(0xFF1E88E5), modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$count files", fontSize = 10.sp, color = Color.Gray)
            }
            Icon(Icons.Default.MoreVert, contentDescription = null, modifier = Modifier.size(16.dp).clickable { onMoreClick() }, tint = Color.Gray)
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
    var step by remember { mutableStateOf(if (isPinConfigured) 0 else 1) } 
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
                    Text("App PIN is currently active. Your documents are safe.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (error != null) Text(error ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    OutlinedTextField(
                        value = currentPinInput,
                        onValueChange = { if (it.length <= 4) { currentPinInput = it; error = null } },
                        label = { Text("Enter PIN to disable") },
                        placeholder = { Text("4 digits") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else if (step == 1) {
                    Text("Please enter a 4-digit PIN to lock the app.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (error != null) Text(error ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    OutlinedTextField(
                        value = pinText,
                        onValueChange = { if (it.length <= 4) { pinText = it; error = null } },
                        label = { Text("New PIN") },
                        placeholder = { Text("4 digits") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else if (step == 2) {
                    Text("Repeat the PIN you just entered to confirm.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (error != null) Text(error ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    OutlinedTextField(
                        value = confirmPinText,
                        onValueChange = { if (it.length <= 4) { confirmPinText = it; error = null } },
                        label = { Text("Confirm PIN") },
                        placeholder = { Text("4 digits") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
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
                        if (verifyPin(currentPinInput)) { onDisablePin(); onDismiss() } else { error = "The PIN you entered is incorrect!"; currentPinInput = "" }
                    } else if (step == 1) {
                        if (pinText.length == 4) step = 2 else error = "Please enter 4 digits."
                    } else if (step == 2) {
                        if (confirmPinText == pinText) { onSetPin(pinText); onDismiss() } else { error = "The PINs you entered do not match!"; confirmPinText = "" }
                    }
                },
                enabled = when {
                    isPinConfigured && step == 0 -> currentPinInput.length == 4
                    step == 1 -> pinText.length == 4
                    step == 2 -> confirmPinText.length == 4
                    else -> false
                }
            ) {
                Text(when { isPinConfigured && step == 0 -> "Disable"; step == 1 -> "Continue"; step == 2 -> "Save"; else -> "Confirm" })
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        shape = RoundedCornerShape(24.dp)
    )
}
