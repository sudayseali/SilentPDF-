package com.silentpdf.app.ui.screens

import android.content.Context

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.silentpdf.app.bionic.BionicConfig
import com.silentpdf.app.bionic.ProcessedBionicPage
import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast

import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import java.io.FileOutputStream
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.animation.core.*
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silentpdf.app.ui.viewmodel.DrawingStroke
import com.silentpdf.app.ui.viewmodel.SilentPdfViewModel
import kotlinx.coroutines.launch

fun printPdf(context: Context, pdf: com.silentpdf.app.data.db.PdfEntity) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
    try {
        val printAdapter = object : PrintDocumentAdapter() {
            override fun onWrite(
                pages: Array<out android.print.PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                try {
                    val input = context.contentResolver.openInputStream(android.net.Uri.parse(pdf.uriString))
                    val output = FileOutputStream(destination?.fileDescriptor)
                    input?.copyTo(output)
                    input?.close()
                    output.close()
                    callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                }
            }

            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder(pdf.fileName)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .build()
                callback?.onLayoutFinished(info, newAttributes != oldAttributes)
            }
        }
        printManager.print(pdf.fileName, printAdapter, PrintAttributes.Builder().build())
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    viewModel: SilentPdfViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val handleExit = {
        viewModel.closePdf()
        onNavigateBack()
    }

    BackHandler {
        handleExit()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.closePdf()
        }
    }

    val currentPdf by viewModel.currentPdf.collectAsState()
    val pageCount by viewModel.pageCount.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val pageBitmap by viewModel.currentPageBitmap.collectAsState()
    val isTrueDarkMode by viewModel.isTrueDarkMode.collectAsState()
    val isAppDarkMode by viewModel.isAppDarkMode.collectAsState()
    val isPdfLoading by viewModel.isPdfLoading.collectAsState()
    val bookmarks by viewModel.currentBookmarks.collectAsState()
    val pageDrawings by viewModel.pageDrawings.collectAsState()
    val openedPdfTextPages by viewModel.openedPdfTextPages.collectAsState()

    val readerBgColor = if (isAppDarkMode) Color.Black else MaterialTheme.colorScheme.background
    val readerSurfaceColor = if (isAppDarkMode) Color(0xFF111422) else MaterialTheme.colorScheme.surface
    val readerBorderColor = if (isAppDarkMode) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
    val readerOnSurfaceColor = if (isAppDarkMode) Color(0xFFF1F5F9) else MaterialTheme.colorScheme.onSurface
    val readerOnSurfaceVariantColor = if (isAppDarkMode) Color(0xFF94A3B8) else MaterialTheme.colorScheme.onSurfaceVariant
    val readerButtonBgColor = if (isAppDarkMode) Color(0xFF1E263D).copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    // Password decrypt flows
    val isPasswordProtected by viewModel.isPasswordProtected.collectAsState()
    val pdfOpeningError by viewModel.pdfOpeningError.collectAsState()

    // Text search flows
    val searchInPdfQuery by viewModel.pdfSearchQuery.collectAsState()
    val searchResults by viewModel.pdfSearchResults.collectAsState()
    val activeSearchMatchIndex by viewModel.activeSearchMatchIndex.collectAsState()
    val isOcrRequired by viewModel.isOcrRequired.collectAsState()
    val isSearchingInPdf by viewModel.isSearchingInPdf.collectAsState()
    val searchProgress by viewModel.searchProgress.collectAsState()

    // Outline flows
    val pdfOutline by viewModel.pdfOutline.collectAsState()
    val isOutlineLoading by viewModel.isOutlineLoading.collectAsState()

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var viewWidth by remember { mutableStateOf(1080) }

    var isDrawingMode by remember { mutableStateOf(false) }
    var selectedColor by remember { mutableStateOf(Color(0xFFF44336)) } // Default Red
    var strokeWidth by remember { mutableStateOf(8f) }
    var isEraserMode by remember { mutableStateOf(false) }
    var isHighlighterMode by remember { mutableStateOf(false) }
    var isUnderlineMode by remember { mutableStateOf(false) }
    var isTextMode by remember { mutableStateOf(false) }

    val drawingColors = listOf(
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

    val sepiaColorMatrix = remember {
        ColorMatrix(floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
    }

    var isFullScreen by remember { mutableStateOf(true) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showManagePages by remember { mutableStateOf(false) }

    var showMoreMenu by remember { mutableStateOf(false) }
    var showVoiceRecorderDialog by remember { mutableStateOf(false) }
    var showOcrDialog by remember { mutableStateOf(false) }
    var showBionicSettingsDialog by remember { mutableStateOf(false) }
    var showSignDialog by remember { mutableStateOf(false) }
    var showReadingModeMenu by remember { mutableStateOf(false) }
    var showSearchOverlay by remember { mutableStateOf(false) }
    
    var readingStyle by remember { mutableStateOf("Scroll") }
    var readingTheme by remember { mutableStateOf("Light") }
    var keepScreenOn by remember { mutableStateOf(false) }
    
    val activity = context as? android.app.Activity
    DisposableEffect(keepScreenOn) {
        if (keepScreenOn) {
            activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose { activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    var isOcrProcessing by remember { mutableStateOf(false) }
    var ocrExtractedText by remember { mutableStateOf("") }

    val bionicConfig by viewModel.bionicConfig.collectAsState()

    val isRecording by viewModel.isRecording.collectAsState()
    val recordingSeconds by viewModel.recordingSeconds.collectAsState()

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startVoiceRecording(context)
        } else {
            android.widget.Toast.makeText(context, "Please grant Microphone permission to record audio.", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    // Active Drawer Tab: 0 = Outline, 1 = Bookmarks, 2 = Search
    var selectedDrawerTab by remember { mutableStateOf(0) }

    val currentNotes by viewModel.currentNotes.collectAsState()
    var showNoteDialog by remember { mutableStateOf(false) }
    var noteInputText by remember { mutableStateOf("") }

    LaunchedEffect(currentPage) {
        scale = 1f
        offset = Offset.Zero
        currentStroke = null
    }

    // Handles the Encrypted Lock Screen separately
    if (isPasswordProtected) {
        var passwordInput by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Top Exit Nav Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(onClick = handleExit) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel")
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked PDF",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "This file is locked",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Please enter the correct password to open this document.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    placeholder = { Text("Enter password...") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle password visibility"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    isError = pdfOpeningError != null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )

                if (pdfOpeningError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = pdfOpeningError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = {
                        if (passwordInput.isNotBlank() && currentPdf != null) {
                            viewModel.openPdf(currentPdf!!, passwordInput)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.VpnKey, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Document", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    } else {
        // Standard high-fidelity PDF Reader Layout with Navigation Drawer
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
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Book Outline",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // 4 Navigation Tabs: TOC, Bookmarks, Search, and Notes
                        TabRow(
                            selectedTabIndex = selectedDrawerTab,
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Tab(
                                selected = selectedDrawerTab == 0,
                                onClick = { selectedDrawerTab = 0 },
                                text = { Text("TOC", fontSize = 11.sp) }
                            )
                            Tab(
                                selected = selectedDrawerTab == 1,
                                onClick = { selectedDrawerTab = 1 },
                                text = { Text("Bkmks", fontSize = 11.sp) }
                            )
                            Tab(
                                selected = selectedDrawerTab == 2,
                                onClick = { selectedDrawerTab = 2 },
                                text = { Text("Search", fontSize = 11.sp) }
                            )
                            Tab(
                                selected = selectedDrawerTab == 3,
                                onClick = { selectedDrawerTab = 3 },
                                text = { Text("Notes", fontSize = 11.sp) }
                            )
                        }

                        // Lazy viewport inside the drawer based on active tab
                        Box(modifier = Modifier.weight(1f)) {
                            if (selectedDrawerTab == 3) {
                                // Notes list
                                if (currentNotes.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Default.Edit, 
                                                contentDescription = null, 
                                                modifier = Modifier.size(48.dp), 
                                                tint = MaterialTheme.colorScheme.outline
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = "No notes added to this book.",
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(currentNotes) { note ->
                                            Card(
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                ),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        viewModel.jumpToPage(note.pageNumber, viewWidth)
                                                        coroutineScope.launch { drawerState.close() }
                                                    }
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(12.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "Page ${note.pageNumber + 1}",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                        IconButton(
                                                            onClick = {
                                                                viewModel.removeNote(note.id)
                                                            },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Outlined.Delete,
                                                                contentDescription = "Delete",
                                                                tint = MaterialTheme.colorScheme.error,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text(
                                                        text = note.noteText,
                                                        fontSize = 13.sp,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 3,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else when (selectedDrawerTab) {
                                0 -> { // Outlines / Chapters / TOC
                                    if (isOutlineLoading) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator(strokeWidth = 3.dp)
                                        }
                                    } else if (pdfOutline.isEmpty()) {
                                        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(Icons.Outlined.ImportContacts, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Text(
                                                    text = "No chapters found",
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    } else {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            items(pdfOutline) { item ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .clickable {
                                                            viewModel.jumpToPage(item.pageNumber, viewWidth)
                                                            coroutineScope.launch { drawerState.close() }
                                                        }
                                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.MenuBook,
                                                        contentDescription = null,
                                                        tint = if (currentPage == item.pageNumber) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Text(
                                                        text = item.title,
                                                        fontSize = 13.sp,
                                                        fontWeight = if (currentPage == item.pageNumber) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (currentPage == item.pageNumber) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "${item.pageNumber + 1}",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier
                                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                1 -> { // Bookmarks list
                                    if (bookmarks.isEmpty()) {
                                        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(Icons.Outlined.BookmarkBorder, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Text(
                                                    text = "No recently bookmarked pages.",
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    } else {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            items(bookmarks) { bookmark ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .clickable {
                                                            viewModel.jumpToPage(bookmark.pageNumber, viewWidth)
                                                            coroutineScope.launch { drawerState.close() }
                                                        }
                                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Bookmark,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Text(
                                                        text = "Page ${bookmark.pageNumber + 1}",
                                                        fontSize = 14.sp,
                                                        fontWeight = if (currentPage == bookmark.pageNumber) FontWeight.Bold else FontWeight.Normal,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    IconButton(onClick = { viewModel.toggleBookmarkCurrentPage() }) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                2 -> { // Text search inside PDF Content
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        OutlinedTextField(
                                            value = searchInPdfQuery,
                                            onValueChange = { viewModel.searchInPdf(it) },
                                            placeholder = { Text("Search word in book...", fontSize = 13.sp) },
                                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                            trailingIcon = {
                                                if (searchInPdfQuery.isNotEmpty()) {
                                                    IconButton(onClick = { viewModel.searchInPdf("") }) {
                                                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 8.dp),
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                            )
                                        )
                                        
                                        if (isOcrRequired) {
                                            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        text = "No searchable text available. OCR is required.",
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.error,
                                                        textAlign = TextAlign.Center
                                                    )
                                                    Spacer(modifier = Modifier.height(16.dp))
                                                    Button(onClick = { viewModel.scanCurrentPageOcr() }) {
                                                        Text("Scan Current Page")
                                                    }
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Button(onClick = { viewModel.scanEntireDocumentOcr() }) {
                                                        Text("Scan Entire Document")
                                                    }
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    TextButton(onClick = { viewModel.cancelOcrRequirement() }) {
                                                        Text("Cancel")
                                                    }
                                                }
                                            }
                                        } else if (isSearchingInPdf) {
                                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    CircularProgressIndicator(strokeWidth = 3.dp, modifier = Modifier.size(24.dp))
                                                    if (searchProgress > 0f && searchProgress < 1f) {
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        Text(
                                                            text = "Scanning: ${(searchProgress * 100).toInt()}%",
                                                            fontSize = 12.sp,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
                                            }
                                        } else if (searchInPdfQuery.isEmpty()) {
                                            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "Type a word above to search across all pages.",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        } else if (searchResults.isEmpty()) {
                                            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "No results found.",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.error,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = "Found in ${searchResults.size} places",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
                                            )
                                            LazyColumn(
                                                modifier = Modifier.fillMaxSize(),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                items(searchResults.size) { index ->
                                                    val result = searchResults[index]
                                                    Card(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable {
                                                                viewModel.setActiveSearchMatch(index)
                                                                viewModel.jumpToPage(result.page, viewWidth)
                                                                coroutineScope.launch { drawerState.close() }
                                                            },
                                                        colors = CardDefaults.cardColors(
                                                            containerColor = if (activeSearchMatchIndex == index) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                                        ),
                                                        shape = RoundedCornerShape(12.dp)
                                                    ) {
                                                        Column(modifier = Modifier.padding(12.dp)) {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Surface(
                                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                                    shape = RoundedCornerShape(4.dp)
                                                                ) {
                                                                    Text(
                                                                        text = "Page ${result.page + 1}",
                                                                        fontSize = 11.sp,
                                                                        fontWeight = FontWeight.ExtraBold,
                                                                        color = MaterialTheme.colorScheme.primary,
                                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                    )
                                                                }
                                                                Text(
                                                                    text = "Match",
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.Medium,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                            Spacer(modifier = Modifier.height(8.dp))
                                                            val matches = com.silentpdf.app.ui.components.findSearchMatches(result.matchText, searchInPdfQuery)
                                                            com.silentpdf.app.ui.components.HighlightedText(
                                                                text = result.matchText,
                                                                matches = matches,
                                                                currentMatchIndex = -1,
                                                                modifier = Modifier.fillMaxWidth(),
                                                                activeHighlightColor = Color(0xFFFF9800),
                                                                inactiveHighlightColor = Color(0xFFFFF176).copy(alpha = 0.6f),
                                                                textColor = MaterialTheme.colorScheme.onSurface
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) {
            Scaffold(
                topBar = {
                    AnimatedVisibility(
                        visible = !isFullScreen,
                        enter = slideInVertically(initialOffsetY = { -it }),
                        exit = slideOutVertically(targetOffsetY = { -it })
                    ) {
                        // Premium Floating Top Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            readerBgColor.copy(alpha = 0.95f),
                                            readerBgColor.copy(alpha = 0.8f),
                                            Color.Transparent
                                        )
                                    )
                                )
                                .padding(top = 40.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(readerSurfaceColor.copy(alpha = 0.85f))
                                    .border(1.dp, readerBorderColor, RoundedCornerShape(20.dp))
                                    .padding(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    IconButton(
                                        onClick = handleExit,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(readerButtonBgColor)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel", tint = readerOnSurfaceColor)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = currentPdf?.fileName ?: "Silent PDF",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = readerOnSurfaceColor,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            showSearchOverlay = true
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Search, contentDescription = "Search text", tint = Color(0xFF2F80ED), modifier = Modifier.size(20.dp))
                                    }
                                    
                                    val isBookmarked = bookmarks.any { it.pageNumber == currentPage }
                                    IconButton(
                                        onClick = { viewModel.toggleBookmarkCurrentPage() },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                                            contentDescription = "Bookmark",
                                            tint = if (isBookmarked) Color(0xFFFF9500) else readerOnSurfaceVariantColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { coroutineScope.launch { drawerState.open() } },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Outlined.FormatListNumbered, contentDescription = "TOC", tint = readerOnSurfaceVariantColor, modifier = Modifier.size(20.dp))
                                    }
                                    Box {
                                        IconButton(
                                            onClick = { showMoreMenu = true },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = readerOnSurfaceVariantColor, modifier = Modifier.size(20.dp))
                                        }
                                        DropdownMenu(
                                            expanded = showMoreMenu, 
                                            onDismissRequest = { showMoreMenu = false },
                                            modifier = Modifier.background(readerSurfaceColor)
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text(if (isDrawingMode) "Exit Drawing Mode" else "Draw / Annotate", color = readerOnSurfaceColor) },
                                                leadingIcon = { Icon(Icons.Default.Edit, null, tint = if (isDrawingMode) Color(0xFF2F80ED) else readerOnSurfaceVariantColor) },
                                                onClick = {
                                                    isDrawingMode = !isDrawingMode
                                                    if (isDrawingMode) isFullScreen = false
                                                    showMoreMenu = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("High Contrast Mode", color = readerOnSurfaceColor) },
                                                leadingIcon = { Icon(Icons.Outlined.Contrast, null, tint = readerOnSurfaceVariantColor) },
                                                onClick = {
                                                    viewModel.toggleTrueDarkMode()
                                                    showMoreMenu = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text("Bionic Reading", color = readerOnSurfaceColor)
                                                        Spacer(modifier = Modifier.width(16.dp))
                                                        Switch(
                                                            checked = bionicConfig.isEnabled,
                                                            onCheckedChange = { viewModel.updateBionicConfig(isEnabled = it) },
                                                            modifier = Modifier.scale(0.8f)
                                                        )
                                                    }
                                                },
                                                leadingIcon = { Icon(Icons.Default.Bolt, null, tint = if (bionicConfig.isEnabled) Color(0xFFFFB300) else readerOnSurfaceVariantColor) },
                                                onClick = {
                                                    viewModel.updateBionicConfig(isEnabled = !bionicConfig.isEnabled)
                                                }
                                            )
                                             DropdownMenuItem(
                                                 text = { Text("Add/Edit Note", color = readerOnSurfaceColor) },
                                                 leadingIcon = { Icon(Icons.Default.Edit, null, tint = Color(0xFF2F80ED)) },
                                                 onClick = {
                                                     val existing = currentNotes.find { it.pageNumber == currentPage }
                                                     noteInputText = existing?.noteText ?: ""
                                                     showNoteDialog = true
                                                     showMoreMenu = false
                                                 }
                                             )
                                             DropdownMenuItem(
                                                 text = { Text("AI Bionic Reading Settings", color = readerOnSurfaceColor) },
                                                 leadingIcon = { Icon(Icons.Default.Bolt, null, tint = Color(0xFFFFB300)) },
                                                 onClick = {
                                                     showBionicSettingsDialog = true
                                                     showMoreMenu = false
                                                 }
                                             )
                                            DropdownMenuItem(
                                                text = { Text("Fullscreen", color = readerOnSurfaceColor) },
                                                leadingIcon = { Icon(Icons.Default.Fullscreen, null, tint = readerOnSurfaceVariantColor) },
                                                onClick = {
                                                    isFullScreen = true
                                                    showMoreMenu = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Print", color = readerOnSurfaceColor) },
                                                leadingIcon = { Icon(Icons.Default.Print, null, tint = readerOnSurfaceVariantColor) },
                                                onClick = {
                                                    currentPdf?.let { printPdf(context, it) }
                                                    showMoreMenu = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Details", color = readerOnSurfaceColor) },
                                                leadingIcon = { Icon(Icons.Default.Info, null, tint = readerOnSurfaceVariantColor) },
                                                onClick = {
                                                    showInfoDialog = true
                                                    showMoreMenu = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Voice Note", color = readerOnSurfaceColor) },
                                                leadingIcon = { Icon(Icons.Default.Mic, null, tint = Color(0xFF2F80ED)) },
                                                onClick = {
                                                    showVoiceRecorderDialog = true
                                                    showMoreMenu = false
                                                }
                                            )

                                            DropdownMenuItem(
                                                text = { Text("Extract Text (OCR)", color = readerOnSurfaceColor) },
                                                leadingIcon = { Icon(Icons.Default.DocumentScanner, null, tint = Color(0xFF9C27B0)) },
                                                onClick = {
                                                    showMoreMenu = false
                                                    if (pageBitmap != null) {
                                                        isOcrProcessing = true
                                                        showOcrDialog = true
                                                        val image = InputImage.fromBitmap(pageBitmap!!, 0)
                                                        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                                                        recognizer.process(image)
                                                            .addOnSuccessListener { visionText ->
                                                                ocrExtractedText = visionText.text
                                                                isOcrProcessing = false
                                                            }
                                                            .addOnFailureListener { e ->
                                                                ocrExtractedText = "Failed to extract text: ${e.message}"
                                                                isOcrProcessing = false
                                                            }
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                                }
                                if (pageCount > 0) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        androidx.compose.material3.LinearProgressIndicator(
                                            progress = { (currentPage + 1).toFloat() / pageCount.toFloat() },
                                            modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)),
                                            color = Color(0xFF2F80ED),
                                            trackColor = readerOnSurfaceVariantColor.copy(alpha = 0.2f),
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "${currentPage + 1} / $pageCount",
                                            fontSize = 11.sp,
                                            color = readerOnSurfaceVariantColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                bottomBar = {
                    AnimatedVisibility(
                        visible = !isFullScreen,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it })
                    ) {
                        // Premium Floating Bottom Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            readerBgColor.copy(alpha = 0.8f),
                                            readerBgColor.copy(alpha = 0.95f)
                                        )
                                    )
                                )
                                .padding(bottom = 32.dp, top = 16.dp, start = 16.dp, end = 16.dp)
                                .windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(readerSurfaceColor.copy(alpha = 0.85f))
                                    .border(1.dp, readerBorderColor, RoundedCornerShape(24.dp))
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Dynamic Drawing markup toolbar if enabled
                                AnimatedVisibility(
                                    visible = isDrawingMode,
                                    enter = expandVertically(),
                                    exit = shrinkVertically()
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                IconButton(
                                                    onClick = { isEraserMode = false; isHighlighterMode = false },
                                                    modifier = Modifier.background(
                                                        color = if (!isEraserMode && !isHighlighterMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                                        shape = CircleShape
                                                    )
                                                ) {
                                                    Icon(
                                                        Icons.Default.Edit,
                                                        contentDescription = "Pen",
                                                        tint = if (!isEraserMode && !isHighlighterMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { isEraserMode = false; isHighlighterMode = true },
                                                    modifier = Modifier.background(
                                                        color = if (isHighlighterMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                                        shape = CircleShape
                                                    )
                                                ) {
                                                    Icon(
                                                        Icons.Default.BorderColor,
                                                        contentDescription = "Highlight",
                                                        tint = if (isHighlighterMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { isEraserMode = true; isHighlighterMode = false },
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

                                            // Drawing Color Circles selection
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 8.dp)
                                            ) {
                                                val pickerColors = drawingColors.take(6)
                                                pickerColors.forEach { color ->
                                                    Box(
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .clip(CircleShape)
                                                            .background(color)
                                                            .border(
                                                                width = if (selectedColor == color && !isEraserMode) 2.dp else 1.dp,
                                                                color = if (selectedColor == color && !isEraserMode) MaterialTheme.colorScheme.primary else Color.LightGray,
                                                                shape = CircleShape
                                                            )
                                                            .clickable(enabled = !isEraserMode) { selectedColor = color }
                                                    )
                                                }
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                IconButton(onClick = {
                                                    currentPdf?.uriString?.let { uri ->
                                                        viewModel.undoLastStroke(uri, currentPage)
                                                    }
                                                }) {
                                                    Icon(Icons.Default.Undo, contentDescription = "Undo")
                                                }
                                                IconButton(onClick = {
                                                    currentPdf?.uriString?.let { uri ->
                                                        viewModel.redoLastStroke(uri, currentPage)
                                                    }
                                                }) {
                                                    Icon(Icons.Default.Redo, contentDescription = "Redo")
                                                }
                                            }
                                        }

                                        // Slider control for brush width
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.LineWeight, contentDescription = "Size", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Slider(
                                                value = strokeWidth,
                                                onValueChange = { strokeWidth = it },
                                                valueRange = 2f..40f,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))
                                    }
                                }



                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.clickable {
                                            showReadingModeMenu = true
                                        }.padding(8.dp)
                                    ) {
                                        Icon(Icons.Default.MenuBook, contentDescription = "Reading mode", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Reading mode", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.clickable {
                                            showManagePages = true
                                        }.padding(8.dp)
                                    ) {
                                        Icon(Icons.Default.Settings, contentDescription = "Manage", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Manage", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    val isMarkupEnabled = !bionicConfig.isEnabled
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.clickable(enabled = isMarkupEnabled) {
                                            isDrawingMode = !isDrawingMode
                                            if (isDrawingMode) isFullScreen = false
                                        }.padding(8.dp).alpha(if (isMarkupEnabled) 1f else 0.5f)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Markup", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Markup", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.clickable(enabled = isMarkupEnabled) {
                                            showSignDialog = true
                                        }.padding(8.dp).alpha(if (isMarkupEnabled) 1f else 0.5f)
                                    ) {
                                        Icon(Icons.Default.Gesture, contentDescription = "Sign", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Sign", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    val context = androidx.compose.ui.platform.LocalContext.current
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.clickable {
                                            currentPdf?.uriString?.let { uriString ->
                                                val uri = android.net.Uri.parse(uriString)
                                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                    type = "application/pdf"
                                                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(android.content.Intent.createChooser(intent, "Share PDF"))
                                            }
                                        }.padding(8.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Share", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                },

            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(if (isAppDarkMode) Color.Black else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .onSizeChanged { size ->
                            if (size.width > 0 && viewWidth != size.width) {
                                viewWidth = size.width
                                viewModel.renderCurrentPage(size.width)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (showManagePages && currentPdf != null) {
                        androidx.compose.ui.window.Dialog(
                            onDismissRequest = { showManagePages = false },
                            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
                        ) {
                            ManagePagesScreen(
                                pdf = currentPdf!!,
                                viewModel = viewModel,
                                onClose = { showManagePages = false },
                                onPagesChanged = { 
                                    viewModel.openPdf(currentPdf!!)
                                }
                            )
                        }
                    }


                    if (showInfoDialog) {
                        AlertDialog(
                            onDismissRequest = { showInfoDialog = false },
                            title = { Text("Book Details", fontWeight = FontWeight.Bold) },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    currentPdf?.let {
                                        Text("Name: ${it.fileName}", fontWeight = FontWeight.Bold)
                                        Text("Size: ${formatFileSize(it.fileSize)}")
                                        Text("Pages: ${it.totalPages}")
                                        if (it.category != null) {
                                            Text("Category: ${it.category}")
                                        }
                                    } ?: Text("Loading...")
                                }
                            },
                            confirmButton = {
                                Button(onClick = { showInfoDialog = false }, shape = RoundedCornerShape(12.dp)) {
                                    Text("Close")
                                }
                            },
                            shape = RoundedCornerShape(24.dp)
                        )
                    }

                    if (showOcrDialog) {
                        AlertDialog(
                            onDismissRequest = { showOcrDialog = false },
                            title = { Text("Extracted Text", fontWeight = FontWeight.Bold) },
                            text = {
                                if (isOcrProcessing) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        CircularProgressIndicator()
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("Extracting text...", style = MaterialTheme.typography.bodyMedium)
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)
                                    ) {
                                        item {
                                            OutlinedTextField(
                                                value = ocrExtractedText,
                                                onValueChange = { ocrExtractedText = it },
                                                modifier = Modifier.fillMaxWidth(),
                                                placeholder = { Text("No text found on this page.") },
                                                textStyle = MaterialTheme.typography.bodyMedium,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    unfocusedBorderColor = Color.Transparent,
                                                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                                )
                                            )
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        if (ocrExtractedText.isNotEmpty()) {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("OCR Text", ocrExtractedText)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    enabled = !isOcrProcessing && ocrExtractedText.isNotEmpty(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Copy")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showOcrDialog = false }) {
                                    Text("Close")
                                }
                            }
                        )
                    }

                    if (showBionicSettingsDialog) {
                        BionicSettingsDialog(
                            config = bionicConfig,
                            onDismissRequest = { showBionicSettingsDialog = false },
                            onConfigChanged = { newConfig ->
                                viewModel.updateBionicConfig(
                                    isEnabled = newConfig.isEnabled,
                                    intensity = newConfig.intensity,
                                    customPercentage = newConfig.customIntensityPercentage,
                                    language = newConfig.language,
                                    performanceMode = newConfig.performanceMode,
                                    autoOcrForScanned = newConfig.autoOcrForScanned
                                )
                            }
                        )
                    }

                    if (isPdfLoading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    } else if (pageCount > 0) {
                        val pageBackgroundColor = when (readingTheme) {
                            "Dark" -> Color.DarkGray
                            "AMOLED" -> Color.Black
                            "Sepia" -> Color(0xFFF4ECD8)
                            else -> Color.White
                        }

                        val pageColorFilter = when (readingTheme) {
                            "Dark", "AMOLED" -> ColorFilter.colorMatrix(invertColorMatrix)
                            "Sepia" -> ColorFilter.colorMatrix(sepiaColorMatrix)
                            else -> null
                        }

                        if (readingStyle == "Swipe") {
                            val pagerState = androidx.compose.foundation.pager.rememberPagerState(
                                initialPage = currentPage,
                                pageCount = { pageCount }
                            )

                            LaunchedEffect(pagerState) {
                                snapshotFlow { pagerState.currentPage }
                                    .collect { index ->
                                        if (currentPage != index && !isPdfLoading) {
                                            viewModel.updateCurrentPage(index)
                                        }
                                    }
                            }

                            LaunchedEffect(currentPage, activeSearchMatchIndex) {
                                if (pagerState.currentPage != currentPage && currentPage in 0 until pageCount) {
                                    pagerState.animateScrollToPage(currentPage)
                                }
                            }
                            
                            androidx.compose.foundation.pager.HorizontalPager(
                                state = pagerState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(pageBackgroundColor)
                                    .pointerInput(isDrawingMode) {
                                        detectTapGestures(onTap = {
                                            if (!isDrawingMode) {
                                                isFullScreen = !isFullScreen
                                            }
                                        })
                                    }
                            ) { index ->
                                PdfPageItem(
                                    pageIndex = index,
                                    targetWidth = viewWidth,
                                    viewModel = viewModel,
                                    bionicConfig = bionicConfig,
                                    pageBackgroundColor = pageBackgroundColor,
                                    pageColorFilter = pageColorFilter,
                                    pageDrawings = pageDrawings,
                                    currentNotes = currentNotes,
                                    isDrawingMode = isDrawingMode,
                                    isHighlighterMode = isHighlighterMode,
                                    isEraserMode = isEraserMode,
                                    selectedColor = selectedColor,
                                    strokeWidth = strokeWidth,
                                    currentPdf = currentPdf,
                                    readerOnSurfaceColor = readerOnSurfaceColor,
                                    onOpenBionicSettings = { showBionicSettingsDialog = true },
                                    onNoteClick = { text ->
                                        noteInputText = text
                                        showNoteDialog = true
                                    },
                                    onNoteDelete = { pageIdx, text ->
                                        val note = currentNotes.find { it.pageNumber == pageIdx }
                                        if (note != null) {
                                            if (text.isNotEmpty()) {
                                                viewModel.addOrUpdateNote(pageIdx, text)
                                            } else {
                                                viewModel.removeNote(note.id)
                                            }
                                        }
                                    },
                                    scale = scale,
                                    offset = offset
                                )
                            }
                        } else {
                            val listState = androidx.compose.foundation.lazy.rememberLazyListState(initialFirstVisibleItemIndex = currentPage)
                            
                            LaunchedEffect(listState) {
                                snapshotFlow { listState.firstVisibleItemIndex }
                                    .collect { index ->
                                        if (currentPage != index && !isPdfLoading) {
                                            viewModel.updateCurrentPage(index)
                                        }
                                    }
                            }

                            LaunchedEffect(activeSearchMatchIndex, searchResults) {
                                if (searchResults.isNotEmpty() && activeSearchMatchIndex in searchResults.indices) {
                                    val match = searchResults[activeSearchMatchIndex]
                                    val targetPage = match.page
                                    val rectTop = match.rects.firstOrNull()?.top ?: 0f
                                    val estimatedPageHeight = viewWidth * 1.414f
                                    val targetOffset = ((rectTop * estimatedPageHeight) - 150f).coerceAtLeast(0f).toInt()
                                    listState.animateScrollToItem(
                                        index = targetPage,
                                        scrollOffset = targetOffset
                                    )
                                }
                            }

                            LaunchedEffect(currentPage) {
                                // If the user selected a page (e.g. from outline) and it's not currently visible
                                if (listState.firstVisibleItemIndex != currentPage && currentPage in 0 until pageCount) {
                                    // Only scroll if we are not actively tracking a search match that just changed the page
                                    if (searchResults.isEmpty() || searchResults.getOrNull(activeSearchMatchIndex)?.page != currentPage) {
                                        listState.animateScrollToItem(currentPage)
                                    }
                                }
                            }
    
                            androidx.compose.foundation.lazy.LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(pageBackgroundColor)
                                    .pointerInput(isDrawingMode) {
                                        detectTapGestures(onTap = {
                                            if (!isDrawingMode) {
                                                isFullScreen = !isFullScreen
                                            }
                                        })
                                    }
                            ) {
                                items(pageCount) { index ->
                                    Column {
                                        PdfPageItem(
                                            pageIndex = index,
                                            targetWidth = viewWidth,
                                            viewModel = viewModel,
                                            bionicConfig = bionicConfig,
                                            pageBackgroundColor = pageBackgroundColor,
                                            pageColorFilter = pageColorFilter,
                                            pageDrawings = pageDrawings,
                                            currentNotes = currentNotes,
                                            isDrawingMode = isDrawingMode,
                                            isHighlighterMode = isHighlighterMode,
                                            isEraserMode = isEraserMode,
                                            selectedColor = selectedColor,
                                            strokeWidth = strokeWidth,
                                            currentPdf = currentPdf,
                                            readerOnSurfaceColor = readerOnSurfaceColor,
                                            onOpenBionicSettings = { showBionicSettingsDialog = true },
                                            onNoteClick = { text ->
                                                noteInputText = text
                                                showNoteDialog = true
                                            },
                                            onNoteDelete = { pageIdx, text ->
                                                val note = currentNotes.find { it.pageNumber == pageIdx }
                                                if (note != null) {
                                                    if (text.isNotEmpty()) {
                                                        viewModel.addOrUpdateNote(pageIdx, text)
                                                    } else {
                                                        viewModel.removeNote(note.id)
                                                    }
                                                }
                                            },
                                            scale = scale,
                                            offset = offset
                                        )
                                        if (index < pageCount - 1) {
                                            androidx.compose.material3.HorizontalDivider(
                                                thickness = 2.dp,
                                                color = if (readingTheme == "Dark" || readingTheme == "AMOLED") Color.DarkGray else Color.Black
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showReadingModeMenu) {
        @OptIn(ExperimentalMaterial3Api::class)
        ModalBottomSheet(
            onDismissRequest = { showReadingModeMenu = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text("Reading Mode", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

                // 1. Reading Style
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Reading Style", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = readingStyle == "Scroll",
                            onClick = { readingStyle = "Scroll" },
                            label = { Text("Scroll") },
                            leadingIcon = { Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        FilterChip(
                            selected = readingStyle == "Swipe",
                            onClick = { readingStyle = "Swipe" },
                            label = { Text("Swipe") },
                            leadingIcon = { Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }

                // 2. Theme
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Theme", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Light", "Sepia", "Dark", "AMOLED").forEach { theme ->
                            FilterChip(
                                selected = readingTheme == theme,
                                onClick = { readingTheme = theme },
                                label = { Text(theme) }
                            )
                        }
                    }
                }

                // 3. Smart Reading
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Smart Reading", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = bionicConfig.isEnabled,
                            onClick = { showBionicSettingsDialog = true },
                            label = { Text("Reflow (AI text)") },
                            leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        var autoBrightness by remember { mutableStateOf(true) }
                        FilterChip(
                            selected = autoBrightness,
                            onClick = { 
                                autoBrightness = !autoBrightness 
                                if (autoBrightness) {
                                    activity?.window?.attributes = activity?.window?.attributes?.apply {
                                        screenBrightness = -1f // System default (auto)
                                    }
                                } else {
                                    activity?.window?.attributes = activity?.window?.attributes?.apply {
                                        screenBrightness = 0.5f // Fixed manual fallback
                                    }
                                }
                            },
                            label = { Text("Auto Brightness") },
                            leadingIcon = { Icon(Icons.Default.BrightnessAuto, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }

                // 4. Focus Mode
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Focus Mode", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = isFullScreen,
                            onClick = {
                                isFullScreen = !isFullScreen
                            },
                            label = { Text("Hide UI") },
                            leadingIcon = { Icon(Icons.Default.Fullscreen, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        FilterChip(
                            selected = keepScreenOn,
                            onClick = { keepScreenOn = !keepScreenOn },
                            label = { Text("Keep Screen On") },
                            leadingIcon = { Icon(if (keepScreenOn) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    var savedSignatures by remember { mutableStateOf(listOf<DrawingStroke>()) }
    var tempSignature by remember { mutableStateOf<DrawingStroke?>(null) }

    if (showSignDialog) {
        AlertDialog(
            onDismissRequest = { showSignDialog = false; tempSignature = null },
            title = { Text("Sign Document", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Draw your signature below:", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { startOffset ->
                                        tempSignature = DrawingStroke(
                                            points = listOf(startOffset),
                                            color = Color.Black,
                                            width = 6f,
                                            isEraser = false
                                        )
                                    },
                                    onDrag = { change, _ ->
                                        tempSignature = tempSignature?.copy(
                                            points = tempSignature!!.points + change.position
                                        )
                                    },
                                    onDragEnd = { /* Done */ },
                                    onDragCancel = { tempSignature = null }
                                )
                            }
                    ) {
                        Canvas(modifier = Modifier.matchParentSize()) {
                            tempSignature?.let { stroke ->
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
                                        color = stroke.color,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                                            width = stroke.width,
                                            cap = StrokeCap.Round,
                                            join = StrokeJoin.Round
                                        )
                                    )
                                }
                            }
                        }
                    }
                    if (savedSignatures.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Saved Signatures:", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        LazyColumn(modifier = Modifier.heightIn(max = 100.dp)) {
                            items(savedSignatures) { savedSig ->
                                Text(
                                    text = "Signature",
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            currentPdf?.uriString?.let { uri ->
                                                viewModel.addStroke(uri, currentPage, savedSig)
                                                showSignDialog = false
                                            }
                                        }
                                        .padding(8.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        tempSignature?.let { sig ->
                            savedSignatures = savedSignatures + sig
                            currentPdf?.uriString?.let { uri ->
                                viewModel.addStroke(uri, currentPage, sig)
                            }
                        }
                        showSignDialog = false
                        tempSignature = null
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save & Insert")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignDialog = false; tempSignature = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSearchOverlay) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
        ) {
            com.silentpdf.app.ui.components.SearchBarWithNavigation(
                query = searchInPdfQuery,
                onQueryChange = { viewModel.searchInPdf(it) },
                currentMatchIndex = activeSearchMatchIndex,
                totalMatches = searchResults.size,
                onPrevious = { viewModel.previousSearchMatch() },
                onNext = { viewModel.nextSearchMatch() },
                onClose = {
                    showSearchOverlay = false
                    viewModel.searchInPdf("")
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
            )
        }
    
    if (showNoteDialog) {
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            title = {
                Text(
                    text = "Note for Page ${currentPage + 1}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = noteInputText,
                        onValueChange = { noteInputText = it },
                        label = { Text("Your Note") },
                        placeholder = { Text("Write a specific note here...") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val existing = currentNotes.find { it.pageNumber == currentPage }
                        val audioPrefix = if (existing != null && existing.noteText.startsWith("[audio:")) {
                            "[audio:" + existing.noteText.substringAfter("[audio:").substringBefore("]") + "] "
                        } else ""

                        if (noteInputText.isNotBlank()) {
                            viewModel.addOrUpdateNote(currentPage, audioPrefix + noteInputText.trim())
                        } else {
                            if (audioPrefix.isNotEmpty()) {
                                viewModel.addOrUpdateNote(currentPage, audioPrefix.trim())
                            } else {
                                viewModel.removeNoteForPage(currentPage)
                            }
                        }
                        showNoteDialog = false
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val currentPageNote = currentNotes.find { it.pageNumber == currentPage }
                    if (currentPageNote != null) {
                        TextButton(
                            onClick = {
                                viewModel.removeNoteForPage(currentPage)
                                showNoteDialog = false
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete")
                        }
                    }
                    TextButton(onClick = { showNoteDialog = false }) {
                        Text("Cancel")
                    }
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showVoiceRecorderDialog) {
        val currentPageNote = currentNotes.find { it.pageNumber == currentPage }
        val audioPath = if (currentPageNote != null && currentPageNote.noteText.startsWith("[audio:")) {
            currentPageNote.noteText.substringAfter("[audio:").substringBefore("]")
        } else null

        AlertDialog(
            onDismissRequest = { 
                if (isRecording) {
                    viewModel.stopVoiceRecording()
                }
                showVoiceRecorderDialog = false 
            },
            title = {
                Text(
                    text = "Voice Note - Page ${currentPage + 1}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (isRecording) {
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val scaleAnim by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 1.3f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulse_scale"
                        )

                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .graphicsLayer {
                                    scaleX = scaleAnim
                                    scaleY = scaleAnim
                                }
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .background(MaterialTheme.colorScheme.error, shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                            }
                        }

                        val minutes = recordingSeconds / 60
                        val seconds = recordingSeconds % 60
                        Text(
                            text = String.format("%02d:%02d", minutes, seconds),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.error
                        )

                        Text(
                            text = "Recording your voice now...",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Button(
                            onClick = { viewModel.stopVoiceRecording() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Listen & Save")
                        }
                    } else {
                        if (audioPath != null) {
                            Text(
                                text = "This page already has a voice note:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            
                            VoiceNotePlayer(
                                filePath = audioPath,
                                onDelete = {
                                    if (currentPageNote != null) {
                                        val cleanText = currentPageNote.noteText.substringAfter("]").trim()
                                        if (cleanText.isNotEmpty()) {
                                            viewModel.addOrUpdateNote(currentPage, cleanText)
                                        } else {
                                            viewModel.removeNote(currentPageNote.id)
                                        }
                                    }
                                }
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 4.dp))
                            
                            Text(
                                text = "Do you want to record again?",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Text(
                                text = "Press the button below to start recording voice.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }

                        Button(
                            onClick = {
                                val permission = android.Manifest.permission.RECORD_AUDIO
                                val isGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                                    context,
                                    permission
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                
                                if (isGranted) {
                                    viewModel.startVoiceRecording(context)
                                } else {
                                    micPermissionLauncher.launch(permission)
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (audioPath != null) "Record New Voice Note" else "Start Recording")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { 
                        if (isRecording) {
                            viewModel.stopVoiceRecording()
                        }
                        showVoiceRecorderDialog = false 
                    }
                ) {
                    Text("Close")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }


}


}

@Composable
fun VoiceNotePlayer(filePath: String, onDelete: () -> Unit) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var position by remember { mutableStateOf(0f) }
    var duration by remember { mutableStateOf(1f) } // prevent division by zero
    var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }

    DisposableEffect(filePath) {
        onDispose {
            mediaPlayer?.release()
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying && mediaPlayer?.isPlaying == true) {
                position = mediaPlayer?.currentPosition?.toFloat() ?: 0f
                kotlinx.coroutines.delay(200)
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = {
                if (isPlaying) {
                    mediaPlayer?.pause()
                    isPlaying = false
                } else {
                    try {
                        if (mediaPlayer == null) {
                            mediaPlayer = android.media.MediaPlayer().apply {
                                setDataSource(filePath)
                                prepare()
                                setOnCompletionListener {
                                    isPlaying = false
                                    position = 0f
                                }
                            }
                        }
                        mediaPlayer?.start()
                        duration = mediaPlayer?.duration?.toFloat() ?: 1f
                        isPlaying = true
                    } catch (e: Exception) {
                        android.util.Log.e("VoiceNotePlayer", "Playback failed", e)
                    }
                }
            }) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play / Pause",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text("Page Voice Note", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Slider(
                    value = position,
                    onValueChange = {
                        mediaPlayer?.seekTo(it.toInt())
                        position = it
                    },
                    valueRange = 0f..duration,
                    modifier = Modifier.height(24.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val currentMin = (position / 1000).toInt() / 60
                    val currentSec = (position / 1000).toInt() % 60
                    val totalMin = (duration / 1000).toInt() / 60
                    val totalSec = (duration / 1000).toInt() % 60
                    Text(
                        text = String.format("%02d:%02d", currentMin, currentSec),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format("%02d:%02d", totalMin, totalSec),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
                isPlaying = false
                onDelete()
            }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

