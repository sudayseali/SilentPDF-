package com.silentpdf.app.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silentpdf.app.data.db.PdfEntity
import com.silentpdf.app.ui.viewmodel.SilentPdfViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagePagesScreen(
    pdf: PdfEntity,
    viewModel: SilentPdfViewModel,
    onClose: () -> Unit,
    onPagesChanged: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedPages by remember { mutableStateOf(setOf<Int>()) }
    var pageBitmaps by remember { mutableStateOf<Map<Int, Bitmap>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(false) }
    var totalPages by remember { mutableStateOf(0) }

    // Load thumbnails
    LaunchedEffect(pdf) {
        isLoading = true
        withContext(Dispatchers.IO) {
            val count = viewModel.getPdfPageCount()
            totalPages = count
            val bitmaps = mutableMapOf<Int, Bitmap>()
            for (i in 0 until count) {
                // Generate a small thumbnail (e.g. 400px wide)
                val bitmap = viewModel.getPageBitmap(i, 400)
                if (bitmap != null) {
                    bitmaps[i] = bitmap
                }
            }
            pageBitmaps = bitmaps
        }
        isLoading = false
    }

    val isAllSelected = selectedPages.size == totalPages && totalPages > 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage pages", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onClose) {
                        Text("Done")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ManageBottomAction(
                        icon = Icons.Default.AddBox,
                        label = "Insert",
                        onClick = {
                            if (selectedPages.isEmpty()) {
                                Toast.makeText(context, "Select a page to insert after", Toast.LENGTH_SHORT).show()
                                return@ManageBottomAction
                            }
                            val afterPage = selectedPages.maxOrNull() ?: 0
                            isLoading = true
                            scope.launch {
                                val success = viewModel.insertBlankPage(pdf, afterPage)
                                isLoading = false
                                if (success) {
                                    Toast.makeText(context, "Page inserted", Toast.LENGTH_SHORT).show()
                                    onPagesChanged()
                                } else {
                                    Toast.makeText(context, "Failed to insert page", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                    ManageBottomAction(
                        icon = Icons.Default.RotateRight,
                        label = "Rotate",
                        onClick = {
                            if (selectedPages.isEmpty()) return@ManageBottomAction
                            isLoading = true
                            scope.launch {
                                val success = viewModel.rotatePages(pdf, selectedPages.toList())
                                isLoading = false
                                if (success) {
                                    Toast.makeText(context, "Pages rotated", Toast.LENGTH_SHORT).show()
                                    selectedPages = emptySet()
                                    onPagesChanged()
                                } else {
                                    Toast.makeText(context, "Failed to rotate pages", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                    ManageBottomAction(
                        icon = Icons.Default.SaveAlt,
                        label = "Extract",
                        onClick = {
                            if (selectedPages.isEmpty()) return@ManageBottomAction
                            isLoading = true
                            scope.launch {
                                val success = viewModel.extractPages(pdf, selectedPages.toList())
                                isLoading = false
                                if (success) {
                                    Toast.makeText(context, "Pages extracted to new PDF", Toast.LENGTH_LONG).show()
                                    selectedPages = emptySet()
                                } else {
                                    Toast.makeText(context, "Failed to extract pages", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                    ManageBottomAction(
                        icon = Icons.Default.Delete,
                        label = "Delete",
                        onClick = {
                            if (selectedPages.isEmpty()) return@ManageBottomAction
                            if (selectedPages.size == totalPages) {
                                Toast.makeText(context, "Cannot delete all pages", Toast.LENGTH_SHORT).show()
                                return@ManageBottomAction
                            }
                            isLoading = true
                            scope.launch {
                                val success = viewModel.deletePages(pdf, selectedPages.toList())
                                isLoading = false
                                if (success) {
                                    Toast.makeText(context, "Pages deleted", Toast.LENGTH_SHORT).show()
                                    selectedPages = emptySet()
                                    onPagesChanged()
                                } else {
                                    Toast.makeText(context, "Failed to delete pages", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Info banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select pages to manage them", fontSize = 12.sp)
            }

            // Selection row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${selectedPages.size} Selected", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("All", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Checkbox(
                        checked = isAllSelected,
                        onCheckedChange = { checked ->
                            if (checked) {
                                selectedPages = (0 until totalPages).toSet()
                            } else {
                                selectedPages = emptySet()
                            }
                        }
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(totalPages) { index ->
                        val isSelected = selectedPages.contains(index)
                        Box(
                            modifier = Modifier
                                .padding(8.dp)
                                .aspectRatio(0.7f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    if (isSelected) {
                                        selectedPages = selectedPages - index
                                    } else {
                                        selectedPages = selectedPages + index
                                    }
                                }
                        ) {
                            val bitmap = pageBitmaps[index]
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Page ${index + 1}",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                            
                            // Checkbox top right
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .background(
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .size(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Page number bottom left
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .background(
                                        color = Color.Black.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(topEnd = 8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = (index + 1).toString(),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ManageBottomAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}
