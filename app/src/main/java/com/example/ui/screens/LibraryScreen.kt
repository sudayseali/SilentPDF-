package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
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

@Composable
fun BookCover(
    title: String,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    progress: Float = 0.0f,
    uriString: String? = null
) {
    val titleHash = title.hashCode()
    val gradientColors = remember(titleHash) {
        val colorsList = listOf(
            listOf(Color(0xFF1E3A8A), Color(0xFF0F172A)), // Deep Sapphire
            listOf(Color(0xFF3B0764), Color(0xFF18012A)), // Royal Amethyst
            listOf(Color(0xFF064E3B), Color(0xFF021E14)), // Deep Forest Emerald
            listOf(Color(0xFF451A03), Color(0xFF1A0A02)), // Rich Amber
            listOf(Color(0xFF1E1B4B), Color(0xFF0A071E)), // Midnight Indigo
            listOf(Color(0xFF27272A), Color(0xFF09090B))  // Sleek Obsidian
        )
        colorsList[kotlin.math.abs(titleHash) % colorsList.size]
    }

    val context = LocalContext.current
    var thumbnailFile by remember(uriString) { mutableStateOf<java.io.File?>(null) }
    
    LaunchedEffect(uriString) {
        if (uriString != null) {
            thumbnailFile = com.example.util.PdfThumbnailHelper.getThumbnail(context, uriString)
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.verticalGradient(gradientColors))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
    ) {
        if (thumbnailFile != null) {
            AsyncImage(
                model = thumbnailFile,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Overlay gradient to make text visible if thumbnail is too bright
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )
        } else {
            // Decorative Abstract Lines inside cover
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawCircle(
                    color = Color.White.copy(alpha = 0.02f),
                    radius = w * 0.55f,
                    center = androidx.compose.ui.geometry.Offset(w * 0.8f, h * 0.2f)
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.02f),
                    start = androidx.compose.ui.geometry.Offset(0f, h * 0.72f),
                    end = androidx.compose.ui.geometry.Offset(w, h * 0.35f),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.02f),
                    start = androidx.compose.ui.geometry.Offset(0f, h * 0.77f),
                    end = androidx.compose.ui.geometry.Offset(w, h * 0.40f),
                    strokeWidth = 2f
                )
            }
        }

        // Left 3D Book Spine shadow overlay
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(8.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.45f),
                            Color.Black.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                )
                .align(Alignment.CenterStart)
        )

        // PDF Emblem Tag
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .background(Color(0xFFEF4444), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = "PDF",
                color = Color.White,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
        }

        // Bookmark indicator
        if (isFavorite) {
            Icon(
                imageVector = Icons.Filled.Bookmark,
                contentDescription = null,
                tint = Color(0xFFFF9500),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 8.dp)
                    .size(16.dp)
            )
        }

        if (thumbnailFile == null) {
            // Elegant Book Typography (Centered)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.MenuBook,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = title.removeSuffix(".pdf").take(30),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Mini Reading status pill at bottom
        if (progress > 0f) {
            val statusText = if (progress >= 1.0f) "Completed" else "${(progress * 100).toInt()}% Read"
            val statusColor = if (progress >= 1.0f) Color(0xFF10B981) else Color(0xFF2F80ED)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = statusText,
                    color = statusColor,
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun QuickActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    count: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF1B2C4E) else Color(0xFF111422)
        ),
        border = BorderStroke(
            1.dp,
            if (isSelected) Color(0xFF2F80ED).copy(alpha = 0.4f) else Color(0xFF1E263D)
        ),
        modifier = modifier
            .height(84.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color(0xFF2F80ED).copy(alpha = 0.2f) else Color(0xFF1E263D)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) Color(0xFF2F80ED) else Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = count,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color(0xFF2F80ED) else Color(0xFFF1F5F9)
                )
            }
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) Color(0xFFF1F5F9) else Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
fun ContinueReadingCard(
    pdf: PdfEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (pdf.totalPages > 0) pdf.lastPageRead.toFloat() / pdf.totalPages.toFloat() else 0.05f
    
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111422)),
        border = BorderStroke(1.dp, Color(0xFF1E263D)),
        modifier = modifier
            .width(230.dp)
            .height(104.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BookCover(
                title = pdf.fileName,
                isFavorite = pdf.isFavorite,
                progress = progress,
                uriString = pdf.uriString,
                modifier = Modifier
                    .size(56.dp, 78.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = pdf.fileName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF1F5F9),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatFileSize(pdf.fileSize),
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
                
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (pdf.totalPages > 0) "P. ${pdf.lastPageRead}/${pdf.totalPages}" else "Opened",
                            fontSize = 8.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            fontSize = 8.sp,
                            color = Color(0xFF2F80ED),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    LinearProgressIndicator(
                        progress = progress,
                        color = Color(0xFF2F80ED),
                        trackColor = Color(0xFF1E263D),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
fun FolderCard(
    name: String,
    count: Int,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111422)),
        border = BorderStroke(1.dp, Color(0xFF1E263D)),
        modifier = modifier
            .width(135.dp)
            .height(105.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF2F80ED).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Folder,
                        contentDescription = null,
                        tint = Color(0xFF2F80ED),
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Column {
                Text(
                    text = name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF1F5F9),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$count files",
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

@Composable
fun GridPdfCard(
    pdf: PdfEntity,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onMoveToFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val progress = if (pdf.totalPages > 0) pdf.lastPageRead.toFloat() / pdf.totalPages.toFloat() else 0.0f

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111422)),
        border = BorderStroke(1.dp, Color(0xFF1E263D)),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.85f)
            ) {
                BookCover(
                    title = pdf.fileName,
                    isFavorite = pdf.isFavorite,
                    progress = progress,
                    uriString = pdf.uriString,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { showMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(Color(0xFF191D31))
                ) {
                    DropdownMenuItem(
                        text = { Text("Move to Folder", color = Color(0xFFF1F5F9)) },
                        onClick = { showMenu = false; onMoveToFolder() },
                        leadingIcon = { Icon(Icons.Outlined.Folder, null, tint = Color(0xFF2F80ED)) }
                    )
                    DropdownMenuItem(
                        text = { Text("Share", color = Color(0xFFF1F5F9)) },
                        onClick = { showMenu = false; onShare() },
                        leadingIcon = { Icon(Icons.Outlined.Share, null, tint = Color(0xFF2F80ED)) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = Color(0xFFEF4444)) },
                        onClick = { showMenu = false; onDelete() },
                        leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = Color(0xFFEF4444)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = pdf.fileName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF1F5F9),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(2.dp))
            
            Text(
                text = "${formatFileSize(pdf.fileSize)} • ${formatRelativeTime(pdf.lastAccessTime)}",
                fontSize = 9.sp,
                color = Color(0xFF94A3B8)
            )

            if (progress > 0.0f) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = progress,
                    color = Color(0xFF10B981),
                    trackColor = Color(0xFF1E263D),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(CircleShape)
                )
            }
        }
    }
}

@Composable
fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String,
    onActionClick: () -> Unit,
    actionText: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Color(0xFF2F80ED).copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF2F80ED),
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = message,
            color = Color(0xFF94A3B8),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(14.dp))
        Button(
            onClick = onActionClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2F80ED)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(actionText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
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
        containerColor = Color(0xFF08090E), // Ultra luxury Space Dark background
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0C0F1A),
                tonalElevation = 8.dp,
                modifier = Modifier.border(
                    BorderStroke(1.dp, Color(0xFF1E263D).copy(alpha = 0.5f)),
                    RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                )
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0 && selectedCategory == null,
                    onClick = { viewModel.setSelectedTab(0); viewModel.setSelectedCategory(null) },
                    icon = { Icon(Icons.Filled.Description, contentDescription = null) },
                    label = { Text("Library", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF2F80ED),
                        selectedTextColor = Color(0xFF2F80ED),
                        indicatorColor = Color(0xFF1B2C4E),
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { viewModel.setSelectedTab(1); viewModel.setSelectedCategory(null) },
                    icon = { Icon(Icons.Outlined.Schedule, contentDescription = null) },
                    label = { Text("Recents", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF2F80ED),
                        selectedTextColor = Color(0xFF2F80ED),
                        indicatorColor = Color(0xFF1B2C4E),
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { viewModel.setSelectedTab(2); viewModel.setSelectedCategory(null) },
                    icon = { Icon(Icons.Outlined.StarBorder, contentDescription = null) },
                    label = { Text("Favorites", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF2F80ED),
                        selectedTextColor = Color(0xFF2F80ED),
                        indicatorColor = Color(0xFF1B2C4E),
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    )
                )
                NavigationBarItem(
                    selected = showSettingsSheet,
                    onClick = { showSettingsSheet = true },
                    icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                    label = { Text("Settings", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF2F80ED),
                        selectedTextColor = Color(0xFF2F80ED),
                        indicatorColor = Color(0xFF1B2C4E),
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    )
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { filePickerLauncher.launch(arrayOf("application/pdf")) },
                containerColor = Color(0xFF2F80ED),
                contentColor = Color.White,
                shape = CircleShape,
                icon = { Icon(Icons.Default.Add, "Import PDF", modifier = Modifier.size(24.dp)) },
                text = { Text("Import PDF", fontWeight = FontWeight.Black) }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF08090E))) {
            // Premium subtle glowing background orbs
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF2F80ED).copy(alpha = 0.2f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.8f, size.height * 0.1f),
                        radius = size.width * 0.8f
                    ),
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.8f, size.height * 0.1f),
                    radius = size.width * 0.8f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF9C27B0).copy(alpha = 0.1f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.6f),
                        radius = size.width * 0.7f
                    ),
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.6f),
                    radius = size.width * 0.7f
                )
            }
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
            // 1. Premium Top Bar Branded
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF2F80ED), Color(0xFF1E3A8A)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Description, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Row {
                            Text("Silent", fontWeight = FontWeight.Black, fontSize = 24.sp, color = Color(0xFFF1F5F9))
                            Text("PDF", fontWeight = FontWeight.Black, fontSize = 24.sp, color = Color(0xFF2F80ED))
                        }
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showSecurityDialog = true },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = if (isPinConfigured) Icons.Default.Lock else Icons.Outlined.Lock,
                                contentDescription = "Security Settings",
                                tint = if (isPinConfigured) Color(0xFFFF9500) else Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = { showSettingsSheet = true },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E263D))
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.WorkspacePremium,
                                contentDescription = "Premium Settings",
                                tint = Color(0xFFFF9500),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // 2. Smart Search Bar Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        placeholder = { Text("Search PDF files...", color = Color(0xFF64748B), fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Outlined.Search, null, tint = Color(0xFF94A3B8)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, null, tint = Color(0xFF94A3B8))
                                }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color(0xFF111422),
                            focusedContainerColor = Color(0xFF111422),
                            unfocusedBorderColor = Color(0xFF1E263D),
                            focusedBorderColor = Color(0xFF2F80ED),
                            unfocusedTextColor = Color(0xFFF1F5F9),
                            focusedTextColor = Color(0xFFF1F5F9)
                        ),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.width(10.dp))
                    
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF111422))
                            .border(1.dp, Color(0xFF1E263D), RoundedCornerShape(16.dp))
                            .clickable { showSortMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.FilterList, contentDescription = "Sort Options", tint = Color(0xFF2F80ED), modifier = Modifier.size(22.dp))
                    }
                }
                if (isScanning) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        color = Color(0xFF2F80ED)
                    )
                }
            }

            // 3. Quick Actions Rows / Stats Cards
            if (searchQuery.isEmpty() && selectedCategory == null) {
                item {
                    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                    val screenWidth = configuration.screenWidthDp
                    if (screenWidth < 480) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                QuickActionCard(
                                    icon = Icons.Filled.Description,
                                    title = "All Files",
                                    count = pdfsList.size.toString(),
                                    isSelected = selectedTab == 0,
                                    onClick = { viewModel.setSelectedTab(0) },
                                    modifier = Modifier.weight(1f)
                                )
                                QuickActionCard(
                                    icon = Icons.Outlined.Schedule,
                                    title = "Recent",
                                    count = recentPdfs.size.toString(),
                                    isSelected = selectedTab == 1,
                                    onClick = { viewModel.setSelectedTab(1) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                QuickActionCard(
                                    icon = Icons.Outlined.StarBorder,
                                    title = "Favorites",
                                    count = pdfsList.count { it.isFavorite }.toString(),
                                    isSelected = selectedTab == 2,
                                    onClick = { viewModel.setSelectedTab(2) },
                                    modifier = Modifier.weight(1f)
                                )
                                QuickActionCard(
                                    icon = Icons.Filled.Folder,
                                    title = "Folders",
                                    count = allCategories.size.toString(),
                                    isSelected = false,
                                    onClick = { /* Scroll down / focus folders */ },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QuickActionCard(
                                icon = Icons.Filled.Description,
                                title = "All Files",
                                count = pdfsList.size.toString(),
                                isSelected = selectedTab == 0,
                                onClick = { viewModel.setSelectedTab(0) },
                                modifier = Modifier.weight(1f)
                            )
                            QuickActionCard(
                                icon = Icons.Outlined.Schedule,
                                title = "Recent",
                                count = recentPdfs.size.toString(),
                                isSelected = selectedTab == 1,
                                onClick = { viewModel.setSelectedTab(1) },
                                modifier = Modifier.weight(1f)
                            )
                            QuickActionCard(
                                icon = Icons.Outlined.StarBorder,
                                title = "Favorites",
                                count = pdfsList.count { it.isFavorite }.toString(),
                                isSelected = selectedTab == 2,
                                onClick = { viewModel.setSelectedTab(2) },
                                modifier = Modifier.weight(1f)
                            )
                            QuickActionCard(
                                icon = Icons.Filled.Folder,
                                title = "Folders",
                                count = allCategories.size.toString(),
                                isSelected = false,
                                onClick = { /* Scroll down / focus folders */ },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 4. Continue Reading Horizontal Row
            if (searchQuery.isEmpty() && selectedTab == 0 && selectedCategory == null && recentPdfs.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Continue Reading",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = Color(0xFFF1F5F9)
                            )
                            Text(
                                text = "View all",
                                color = Color(0xFF2F80ED),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { viewModel.setSelectedTab(1) }
                            )
                        }
                        
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(recentPdfs) { pdf ->
                                ContinueReadingCard(
                                    pdf = pdf,
                                    onClick = {
                                        viewModel.openPdf(pdf)
                                        onNavigateToReader()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 5. Folders Horizontal List
            if (searchQuery.isEmpty() && selectedTab == 0 && selectedCategory == null) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Folders",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = Color(0xFFF1F5F9)
                            )
                            Text(
                                text = "+ New Folder",
                                color = Color(0xFF2F80ED),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { showCreateFolderDialog = true }
                            )
                        }
                        
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Default folder / All Books
                            item {
                                FolderCard(
                                    name = "All Books",
                                    count = pdfsList.size,
                                    onClick = { viewModel.setSelectedCategory(null); viewModel.setSelectedTab(0) },
                                    onMenuClick = {}
                                )
                            }
                            // Custom categories
                            items(allCategories) { category ->
                                val count = pdfsList.count { it.category == category }
                                FolderCard(
                                    name = category,
                                    count = count,
                                    onClick = { viewModel.setSelectedCategory(category); viewModel.setSelectedTab(0) },
                                    onMenuClick = {}
                                )
                            }
                            // CTA Add Folder Card
                            item {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111422).copy(alpha = 0.5f)),
                                    border = BorderStroke(1.dp, Color(0xFF1E263D).copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .width(125.dp)
                                        .height(105.dp)
                                        .clickable { showCreateFolderDialog = true }
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.Add, null, tint = Color(0xFF2F80ED), modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Add Folder", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF94A3B8))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. Main Library Section Header
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when {
                            selectedCategory != null -> "Folder: $selectedCategory"
                            searchQuery.isNotEmpty() -> "Search Results"
                            selectedTab == 1 -> "All Recents"
                            selectedTab == 2 -> "Favorite Documents"
                            else -> "All Documents"
                        },
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = Color(0xFFF1F5F9)
                    )
                    
                    if (selectedCategory != null) {
                        Text(
                            text = "Exit Folder",
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { viewModel.setSelectedCategory(null) }
                        )
                    }
                }
            }

            // 7. Grid Document Layout (3 items per row / 3-column elegant layout)
            item {
                val baseList = when {
                    selectedCategory != null -> pdfsList.filter { it.category == selectedCategory }
                    selectedTab == 1 -> pdfsList.filter { it.lastAccessTime > 0 }.sortedByDescending { it.lastAccessTime }
                    selectedTab == 2 -> pdfsList.filter { it.isFavorite }
                    else -> pdfsList
                }

                val filteredList = if (searchQuery.isNotEmpty()) {
                    baseList.filter { it.fileName.contains(searchQuery, ignoreCase = true) }
                } else {
                    baseList
                }

                if (filteredList.isEmpty()) {
                    val promptText = if (searchQuery.isNotEmpty()) {
                        "No results found for '$searchQuery'"
                    } else if (selectedCategory != null) {
                        "This folder is empty."
                    } else {
                        "No PDF files imported yet."
                    }
                    
                    EmptyState(
                        icon = Icons.Outlined.FilePresent,
                        message = promptText,
                        actionText = "Import PDF",
                        onActionClick = { filePickerLauncher.launch(arrayOf("application/pdf")) }
                    )
                } else {
                    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                    val screenWidth = configuration.screenWidthDp
                    val columns = when {
                        screenWidth >= 1200 -> 6
                        screenWidth >= 900 -> 5
                        screenWidth >= 600 -> 4
                        screenWidth >= 440 -> 3
                        else -> 2
                    }
                    val chunkedPdfs = filteredList.chunked(columns)
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
                                        GridPdfCard(
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
                                for (i in rowItems.size until columns) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
        } // Close the Box wrapping LazyColumn
    }

    // Settings Modal Bottom Sheet
    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            containerColor = Color(0xFF111422)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Settings",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                ListItem(
                    headlineContent = { Text("App Security (PIN)", color = Color(0xFFF1F5F9)) },
                    leadingContent = { Icon(Icons.Outlined.Lock, null, tint = Color(0xFF2F80ED)) },
                    modifier = Modifier.clickable { showSettingsSheet = false; showSecurityDialog = true },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                ListItem(
                    headlineContent = { Text("Help / Support", color = Color(0xFFF1F5F9)) },
                    leadingContent = { Icon(Icons.Outlined.SupportAgent, null, tint = Color(0xFF2F80ED)) },
                    modifier = Modifier.clickable { showSettingsSheet = false; showSupportDialog = true },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                ListItem(
                    headlineContent = { Text("Sort Options", color = Color(0xFFF1F5F9)) },
                    leadingContent = { Icon(Icons.Outlined.Sort, null, tint = Color(0xFF2F80ED)) },
                    modifier = Modifier.clickable { showSettingsSheet = false; showSortMenu = true },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                ListItem(
                    headlineContent = { Text("Toggle View Mode", color = Color(0xFFF1F5F9)) },
                    leadingContent = { Icon(if (isGridView) Icons.Outlined.ViewList else Icons.Outlined.GridView, null, tint = Color(0xFF2F80ED)) },
                    modifier = Modifier.clickable { showSettingsSheet = false; viewModel.toggleGridView() },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }
    }

    // Interactive Dialogs
    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false; folderNameInput = "" },
            title = { Text("Create New Folder", fontWeight = FontWeight.Bold, color = Color(0xFFF1F5F9)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter the name of the folder you want to create to organize your books.", fontSize = 13.sp, color = Color(0xFF94A3B8))
                    OutlinedTextField(
                        value = folderNameInput,
                        onValueChange = { folderNameInput = it },
                        placeholder = { Text("Example: Islamic Lectures") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color(0xFF08090E),
                            focusedContainerColor = Color(0xFF08090E),
                            unfocusedBorderColor = Color(0xFF1E263D),
                            focusedBorderColor = Color(0xFF2F80ED),
                            unfocusedTextColor = Color(0xFFF1F5F9),
                            focusedTextColor = Color(0xFFF1F5F9)
                        )
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
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2F80ED))
                ) {
                    Text("Create", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false; folderNameInput = "" }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF111422),
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (pdfToMoveToFolder != null) {
        val pdf = pdfToMoveToFolder!!
        var customCategoryInput by remember { mutableStateOf("") }
        var selectedCategoryToMove by remember { mutableStateOf(pdf.category ?: "") }

        AlertDialog(
            onDismissRequest = { pdfToMoveToFolder = null },
            title = { Text("Move to Folder", fontWeight = FontWeight.Bold, color = Color(0xFFF1F5F9)) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Select the folder you want to move '${pdf.fileName}' to:", fontSize = 13.sp, color = Color(0xFF94A3B8))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedCategoryToMove = "" }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedCategoryToMove == "",
                                onClick = { selectedCategoryToMove = "" },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF2F80ED))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Default Folder", fontSize = 14.sp, color = Color(0xFFF1F5F9))
                        }
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
                                    onClick = { selectedCategoryToMove = cat },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF2F80ED))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(cat, fontSize = 14.sp, color = Color(0xFFF1F5F9))
                            }
                        }
                    }
                    HorizontalDivider(color = Color(0xFF1E263D))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Or create a new folder:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF1F5F9))
                        OutlinedTextField(
                            value = customCategoryInput,
                            onValueChange = {
                                customCategoryInput = it
                                if (it.isNotBlank()) selectedCategoryToMove = it.trim()
                            },
                            placeholder = { Text("Enter new name...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = Color(0xFF08090E),
                                focusedContainerColor = Color(0xFF08090E),
                                unfocusedBorderColor = Color(0xFF1E263D),
                                focusedBorderColor = Color(0xFF2F80ED),
                                unfocusedTextColor = Color(0xFFF1F5F9),
                                focusedTextColor = Color(0xFFF1F5F9)
                            )
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
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2F80ED))
                ) {
                    Text("Confirm", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { pdfToMoveToFolder = null }) { Text("Cancel", color = Color(0xFF94A3B8)) }
            },
            containerColor = Color(0xFF111422),
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showSupportDialog) {
        AlertDialog(
            onDismissRequest = { showSupportDialog = false },
            title = { Text("Help / Support", fontWeight = FontWeight.Bold, color = Color(0xFFF1F5F9)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("If you encounter any issues, please contact us via WhatsApp.", color = Color(0xFF94A3B8))
                    Text(
                        "Note: Only chat messages are allowed. Voice and video calls are not permitted.",
                        color = Color(0xFFEF4444),
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
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2F80ED))
                ) {
                    Text("WhatsApp Chat", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSupportDialog = false }) { Text("Cancel", color = Color(0xFF94A3B8)) }
            },
            containerColor = Color(0xFF111422),
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (pdfToDelete != null) {
        val pdf = pdfToDelete!!
        AlertDialog(
            onDismissRequest = { pdfToDelete = null },
            title = { Text("Delete PDF", fontWeight = FontWeight.Bold, color = Color(0xFFF1F5F9)) },
            text = { Text("Are you sure you want to delete '${pdf.fileName}'? This action cannot be undone.", color = Color(0xFF94A3B8)) },
            confirmButton = {
                Button(
                    onClick = { viewModel.deletePdf(pdf); pdfToDelete = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) { Text("OK", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { pdfToDelete = null }) { Text("Cancel", color = Color(0xFF94A3B8)) }
            },
            containerColor = Color(0xFF111422),
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showSortMenu) {
        AlertDialog(
            onDismissRequest = { showSortMenu = false },
            title = { Text("Sort Books", fontWeight = FontWeight.Bold, color = Color(0xFFF1F5F9)) },
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
                                .clickable { viewModel.setSortBy(index); showSortMenu = false }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (sortBy == index) Color(0xFF2F80ED) else Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = title,
                                modifier = Modifier.weight(1f),
                                fontWeight = if (sortBy == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (sortBy == index) Color(0xFF2F80ED) else Color(0xFFF1F5F9)
                            )
                            RadioButton(
                                selected = sortBy == index,
                                onClick = { viewModel.setSortBy(index); showSortMenu = false },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF2F80ED))
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showSortMenu = false }) { Text("Close", color = Color(0xFF94A3B8)) } },
            containerColor = Color(0xFF111422),
            shape = RoundedCornerShape(24.dp)
        )
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
                color = Color(0xFF2F80ED)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isPinConfigured && step == 0) {
                    Text("App PIN is currently active. Your documents are safe.", fontSize = 14.sp, color = Color(0xFF94A3B8))
                    if (error != null) Text(error ?: "", color = Color(0xFFEF4444), fontSize = 12.sp)
                    OutlinedTextField(
                        value = currentPinInput,
                        onValueChange = { if (it.length <= 4) { currentPinInput = it; error = null } },
                        label = { Text("Enter PIN to disable", color = Color(0xFF64748B)) },
                        placeholder = { Text("4 digits", color = Color(0xFF64748B)) },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color(0xFF08090E),
                            focusedContainerColor = Color(0xFF08090E),
                            unfocusedBorderColor = Color(0xFF1E263D),
                            focusedBorderColor = Color(0xFF2F80ED),
                            unfocusedTextColor = Color(0xFFF1F5F9),
                            focusedTextColor = Color(0xFFF1F5F9)
                        )
                    )
                } else if (step == 1) {
                    Text("Please enter a 4-digit PIN to lock the app.", fontSize = 14.sp, color = Color(0xFF94A3B8))
                    if (error != null) Text(error ?: "", color = Color(0xFFEF4444), fontSize = 12.sp)
                    OutlinedTextField(
                        value = pinText,
                        onValueChange = { if (it.length <= 4) { pinText = it; error = null } },
                        label = { Text("New PIN", color = Color(0xFF64748B)) },
                        placeholder = { Text("4 digits", color = Color(0xFF64748B)) },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color(0xFF08090E),
                            focusedContainerColor = Color(0xFF08090E),
                            unfocusedBorderColor = Color(0xFF1E263D),
                            focusedBorderColor = Color(0xFF2F80ED),
                            unfocusedTextColor = Color(0xFFF1F5F9),
                            focusedTextColor = Color(0xFFF1F5F9)
                        )
                    )
                } else if (step == 2) {
                    Text("Repeat the PIN you just entered to confirm.", fontSize = 14.sp, color = Color(0xFF94A3B8))
                    if (error != null) Text(error ?: "", color = Color(0xFFEF4444), fontSize = 12.sp)
                    OutlinedTextField(
                        value = confirmPinText,
                        onValueChange = { if (it.length <= 4) { confirmPinText = it; error = null } },
                        label = { Text("Confirm PIN", color = Color(0xFF64748B)) },
                        placeholder = { Text("4 digits", color = Color(0xFF64748B)) },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color(0xFF08090E),
                            focusedContainerColor = Color(0xFF08090E),
                            unfocusedBorderColor = Color(0xFF1E263D),
                            focusedBorderColor = Color(0xFF2F80ED),
                            unfocusedTextColor = Color(0xFFF1F5F9),
                            focusedTextColor = Color(0xFFF1F5F9)
                        )
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
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2F80ED))
            ) {
                Text(
                    text = when { isPinConfigured && step == 0 -> "Disable"; step == 1 -> "Continue"; step == 2 -> "Save"; else -> "Confirm" },
                    color = Color.White
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Color(0xFF94A3B8)) } },
        containerColor = Color(0xFF111422),
        shape = RoundedCornerShape(24.dp)
    )
}
