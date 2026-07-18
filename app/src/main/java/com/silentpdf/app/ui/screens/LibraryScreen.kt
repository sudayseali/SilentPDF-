package com.silentpdf.app.ui.screens

import android.content.Context
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
import androidx.compose.ui.draw.shadow
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
import com.silentpdf.app.data.db.PdfEntity
import com.silentpdf.app.ui.viewmodel.SilentPdfViewModel

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

enum class CoverTheme {
    QURAN, TECH, DESIGN, BUSINESS, GEOMETRIC, DEFAULT
}

@Composable
fun BookCover(
    title: String,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    progress: Float = 0.0f,
    uriString: String? = null
) {
    val lowercaseTitle = title.lowercase()
    val coverTheme = remember(title) {
        when {
            lowercaseTitle.contains("quran") || lowercaseTitle.contains("islam") || lowercaseTitle.contains("koran") || lowercaseTitle.contains("hadiis") || lowercaseTitle.contains("sunnah") -> CoverTheme.QURAN
            lowercaseTitle.contains("computer") || lowercaseTitle.contains("science") || lowercaseTitle.contains("code") || lowercaseTitle.contains("notes") || lowercaseTitle.contains("cs") || lowercaseTitle.contains("tech") || lowercaseTitle.contains("program") -> CoverTheme.TECH
            lowercaseTitle.contains("design") || lowercaseTitle.contains("art") || lowercaseTitle.contains("creative") || lowercaseTitle.contains("thinking") || lowercaseTitle.contains("graphic") -> CoverTheme.DESIGN
            lowercaseTitle.contains("business") || lowercaseTitle.contains("finance") || lowercaseTitle.contains("plan") || lowercaseTitle.contains("money") || lowercaseTitle.contains("economy") -> CoverTheme.BUSINESS
            lowercaseTitle.contains("lecture") || lowercaseTitle.contains("math") || lowercaseTitle.contains("physics") || lowercaseTitle.contains("school") || lowercaseTitle.contains("university") || lowercaseTitle.contains("class") -> CoverTheme.GEOMETRIC
            else -> CoverTheme.DEFAULT
        }
    }

    val defaultGradient = remember(title) {
        val hash = kotlin.math.abs(title.hashCode())
        val combinations = listOf(
            listOf(Color(0xFF1A365D), Color(0xFF2A4365)), // Sapphire
            listOf(Color(0xFF234E52), Color(0xFF2D3748)), // Teal Slate
            listOf(Color(0xFF4A154B), Color(0xFF2E082D)), // Slack Aubergine
            listOf(Color(0xFF5A2011), Color(0xFF2C0F08)), // Terracotta
            listOf(Color(0xFF2D3748), Color(0xFF1A202C)), // Deep Charcoal
            listOf(Color(0xFF3F3D56), Color(0xFF2F2E41))  // Classic Muted Blue-grey
        )
        combinations[hash % combinations.size]
    }

    val gradientColors = when (coverTheme) {
        CoverTheme.QURAN -> listOf(Color(0xFF0C1E3C), Color(0xFF1B365D)) // Royal Navy
        CoverTheme.TECH -> listOf(Color(0xFF0F2042), Color(0xFF1D53A0)) // Digital Cobalt
        CoverTheme.DESIGN -> listOf(Color(0xFF0F0E17), Color(0xFF2E1C4E)) // Creative Violet
        CoverTheme.BUSINESS -> listOf(Color(0xFF2C3E50), Color(0xFF34495E)) // Executive Slate
        CoverTheme.GEOMETRIC -> listOf(Color(0xFFEA580C), Color(0xFFD97706)) // Amber Orange
        CoverTheme.DEFAULT -> defaultGradient
    }

    val context = LocalContext.current
    var thumbnailFile by remember(uriString) { mutableStateOf<java.io.File?>(null) }
    
    LaunchedEffect(uriString) {
        if (uriString != null) {
            thumbnailFile = com.silentpdf.app.util.PdfThumbnailHelper.getThumbnail(context, uriString)
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.verticalGradient(gradientColors))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
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
            // Procedural Cover Art Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                
                when (coverTheme) {
                    CoverTheme.QURAN -> {
                        // Draw concentric gold rings
                        drawCircle(
                            color = Color(0xFFFCD34D).copy(alpha = 0.12f),
                            radius = w * 0.32f,
                            center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.45f)
                        )
                        drawCircle(
                            color = Color(0xFFFCD34D).copy(alpha = 0.06f),
                            radius = w * 0.45f,
                            center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.45f)
                        )
                        // Radiant lines
                        for (i in 0 until 12) {
                            val angle = (i * 30) * (Math.PI / 180f)
                            val startR = w * 0.32f
                            val endR = w * 0.42f
                            val startX = (w * 0.5f + startR * Math.cos(angle)).toFloat()
                            val startY = (h * 0.45f + startR * Math.sin(angle)).toFloat()
                            val endX = (w * 0.5f + endR * Math.cos(angle)).toFloat()
                            val endY = (h * 0.45f + endR * Math.sin(angle)).toFloat()
                            drawLine(
                                color = Color(0xFFFCD34D).copy(alpha = 0.08f),
                                start = androidx.compose.ui.geometry.Offset(startX, startY),
                                end = androidx.compose.ui.geometry.Offset(endX, endY),
                                strokeWidth = 2f
                            )
                        }
                    }
                    CoverTheme.TECH -> {
                        // Circuit nodes
                        drawLine(
                            color = Color.White.copy(alpha = 0.08f),
                            start = androidx.compose.ui.geometry.Offset(0f, h * 0.2f),
                            end = androidx.compose.ui.geometry.Offset(w * 0.4f, h * 0.2f),
                            strokeWidth = 2f
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.08f),
                            start = androidx.compose.ui.geometry.Offset(w * 0.4f, h * 0.2f),
                            end = androidx.compose.ui.geometry.Offset(w * 0.7f, h * 0.5f),
                            strokeWidth = 2f
                        )
                        drawCircle(
                            color = Color(0xFF2F80ED).copy(alpha = 0.3f),
                            radius = 6.dp.toPx(),
                            center = androidx.compose.ui.geometry.Offset(w * 0.7f, h * 0.5f)
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.08f),
                            start = androidx.compose.ui.geometry.Offset(w * 0.2f, h * 0.8f),
                            end = androidx.compose.ui.geometry.Offset(w * 0.6f, h * 0.8f),
                            strokeWidth = 2f
                        )
                        drawCircle(
                            color = Color(0xFF0D9488).copy(alpha = 0.3f),
                            radius = 5.dp.toPx(),
                            center = androidx.compose.ui.geometry.Offset(w * 0.6f, h * 0.8f)
                        )
                    }
                    CoverTheme.DESIGN -> {
                        // Modern fluid Bezier waves
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(0f, h * 0.6f)
                            cubicTo(
                                w * 0.25f, h * 0.45f,
                                w * 0.75f, h * 0.75f,
                                w, h * 0.55f
                            )
                            lineTo(w, h)
                            lineTo(0f, h)
                            close()
                        }
                        drawPath(
                            path = path,
                            brush = Brush.verticalGradient(listOf(Color(0xFF2F80ED).copy(alpha = 0.15f), Color.Transparent))
                        )
                        val linePath = androidx.compose.ui.graphics.Path().apply {
                            moveTo(0f, h * 0.55f)
                            cubicTo(
                                w * 0.25f, h * 0.4f,
                                w * 0.75f, h * 0.7f,
                                w, h * 0.5f
                            )
                        }
                        drawPath(
                            path = linePath,
                            color = Color.White.copy(alpha = 0.1f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                        )
                    }
                    CoverTheme.BUSINESS -> {
                        // Skyscraper silhouettes
                        drawRect(
                            color = Color.White.copy(alpha = 0.05f),
                            size = androidx.compose.ui.geometry.Size(w * 0.25f, h * 0.45f),
                            topLeft = androidx.compose.ui.geometry.Offset(w * 0.15f, h * 0.55f)
                        )
                        drawRect(
                            color = Color.White.copy(alpha = 0.07f),
                            size = androidx.compose.ui.geometry.Size(w * 0.3f, h * 0.55f),
                            topLeft = androidx.compose.ui.geometry.Offset(w * 0.38f, h * 0.45f)
                        )
                        drawRect(
                            color = Color.White.copy(alpha = 0.04f),
                            size = androidx.compose.ui.geometry.Size(w * 0.22f, h * 0.35f),
                            topLeft = androidx.compose.ui.geometry.Offset(w * 0.7f, h * 0.65f)
                        )
                    }
                    CoverTheme.GEOMETRIC -> {
                        // Overlapping triangles
                        val p1 = androidx.compose.ui.graphics.Path().apply {
                            moveTo(w * 0.1f, h * 0.9f)
                            lineTo(w * 0.5f, h * 0.4f)
                            lineTo(w * 0.9f, h * 0.9f)
                            close()
                        }
                        drawPath(p1, Color.White.copy(alpha = 0.06f))
                        val p2 = androidx.compose.ui.graphics.Path().apply {
                            moveTo(w * 0.3f, h * 0.85f)
                            lineTo(w * 0.7f, h * 0.35f)
                            lineTo(w * 0.95f, h * 0.85f)
                            close()
                        }
                        drawPath(p2, Color.White.copy(alpha = 0.04f))
                    }
                    CoverTheme.DEFAULT -> {
                        // Soft glowing bubbles in corners
                        drawCircle(
                            color = Color.White.copy(alpha = 0.05f),
                            radius = w * 0.5f,
                            center = androidx.compose.ui.geometry.Offset(w * 0.8f, h * 0.2f)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.03f),
                            radius = w * 0.4f,
                            center = androidx.compose.ui.geometry.Offset(w * 0.1f, h * 0.7f)
                        )
                    }
                }
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
                            Color.Black.copy(alpha = 0.4f),
                            Color.Black.copy(alpha = 0.08f),
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
                .padding(horizontal = 5.dp, vertical = 2.dp)
        ) {
            Text(
                text = "PDF",
                color = Color.White,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
        }

        if (thumbnailFile == null) {
            // Dynamic Book Typography (Centered & Structured based on title)
            val formattedTitle = remember(title) {
                val cleaned = title.removeSuffix(".pdf").removeSuffix(".PDF")
                    .replace("_", " ")
                    .replace("-", " ")
                    .trim()
                if (cleaned.length > 24) cleaned.take(22) + "..." else cleaned
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val iconColor = if (coverTheme == CoverTheme.QURAN) Color(0xFFFCD34D).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.15f)
                Icon(
                    imageVector = if (coverTheme == CoverTheme.QURAN) Icons.Filled.MenuBook else Icons.Filled.Description,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                
                Text(
                    text = formattedTitle,
                    color = if (coverTheme == CoverTheme.QURAN) Color(0xFFFCD34D) else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                val labelText = when (coverTheme) {
                    CoverTheme.QURAN -> "The Holy Quran"
                    CoverTheme.TECH -> "Tech Notes"
                    CoverTheme.DESIGN -> "Design Thinking"
                    CoverTheme.BUSINESS -> "Strategic Plan"
                    CoverTheme.GEOMETRIC -> "Class Lecture"
                    CoverTheme.DEFAULT -> "Document"
                }
                
                val labelColor = if (coverTheme == CoverTheme.QURAN) Color(0xFFFCD34D).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.4f)
                
                Text(
                    text = labelText.uppercase(),
                    color = labelColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 7.5.sp,
                    letterSpacing = 0.8.sp
                )
            }
        }

        // Mini Reading status progress bar inside the cover at bottom
        if (progress > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color.White.copy(alpha = 0.25f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(Color(0xFF2F80ED))
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
    val isTrueDarkMode = isSystemInDarkTheme()
    
    // Choose specific colors for icons as in screenshot
    val iconTint = when (title) {
        "Favorites" -> Color(0xFFFFA000) // Star Orange/Gold
        else -> Color(0xFF2F80ED) // Clean blue
    }
    
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                Color(0xFFEBF4FF)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            1.dp,
            if (isSelected) Color(0xFF2F80ED).copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
        ),
        modifier = modifier
            .height(115.dp)
            .shadow(
                elevation = if (isSelected) 3.dp else 1.dp,
                shape = RoundedCornerShape(16.dp),
                clip = false
            )
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = count,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
        modifier = modifier
            .width(230.dp)
            .height(104.dp)
            .shadow(1.dp, RoundedCornerShape(16.dp))
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
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatFileSize(pdf.fileSize),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
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
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
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
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
        modifier = modifier
            .width(185.dp) // Perfect width for horizontal scrolling folders row
            .height(68.dp)
            .shadow(1.dp, RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Filled.Folder,
                    contentDescription = null,
                    tint = Color(0xFF2F80ED), // Clean vibrant folder blue
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$count files",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f) // Beautiful, professional book cover ratio
        ) {
            BookCover(
                title = pdf.fileName,
                isFavorite = pdf.isFavorite,
                progress = progress,
                uriString = pdf.uriString,
                modifier = Modifier.fillMaxSize()
            )

            // More Options overlay button (Subtle translucent grey circle matching screenshot)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.85f))
                    .clickable { showMenu = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = Color(0xFF475569), // Sleek grey icon
                    modifier = Modifier.size(16.dp)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                DropdownMenuItem(
                    text = { Text("Move to Folder", color = MaterialTheme.colorScheme.onSurface) },
                    onClick = { showMenu = false; onMoveToFolder() },
                    leadingIcon = { Icon(Icons.Outlined.Folder, null, tint = Color(0xFF2F80ED)) }
                )
                DropdownMenuItem(
                    text = { Text("Favorite", color = MaterialTheme.colorScheme.onSurface) },
                    onClick = { showMenu = false; onFavoriteToggle() },
                    leadingIcon = { Icon(if (pdf.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline, null, tint = Color(0xFFFF9500)) }
                )
                DropdownMenuItem(
                    text = { Text("Share", color = MaterialTheme.colorScheme.onSurface) },
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

        // Title and Favorite star in a single line
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = pdf.fileName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (pdf.isFavorite) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "Starred",
                    tint = Color(0xFF2F80ED), // Beautiful filled blue star as in screenshot
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(2.dp))
        
        Text(
            text = "${formatFileSize(pdf.fileSize)} • ${formatRelativeTime(pdf.lastAccessTime)}",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
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
    onNavigateToCamera: () -> Unit = {},
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
    val isTrueDarkMode by viewModel.isTrueDarkMode.collectAsState()

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var folderNameInput by remember { mutableStateOf("") }
    var pdfToMoveToFolder by remember { mutableStateOf<PdfEntity?>(null) }
    var pdfToDelete by remember { mutableStateOf<PdfEntity?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    val isPinConfigured by viewModel.isPinConfigured.collectAsState()
    var showSecurityDialog by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }
        var showSettingsSheet by remember { mutableStateOf(false) }
    var showCreateSheet by remember { mutableStateOf(false) }
    var showTextToPdfDialog by remember { mutableStateOf(false) }
    var showImagesToPdfDialog by remember { mutableStateOf(false) }
    var selectedImagesForPdf by remember { mutableStateOf<List<android.net.Uri>>(emptyList()) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                selectedImagesForPdf = uris
                showImagesToPdfDialog = true
            }
        }
    )

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
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
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
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
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
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
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
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { viewModel.setSelectedTab(3); viewModel.setSelectedCategory(null) },
                    icon = { Icon(Icons.Outlined.Build, contentDescription = null) },
                    label = { Text("Tools", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF2F80ED),
                        selectedTextColor = Color(0xFF2F80ED),
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
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
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )
            }
        },
        floatingActionButton = {
            if (selectedTab != 3) {
                ExtendedFloatingActionButton(
                    onClick = { showCreateSheet = true },
                    containerColor = Color(0xFF2F80ED),
                    contentColor = Color.White,
                    shape = CircleShape,
                    icon = { Icon(Icons.Default.Add, "Create PDF", modifier = Modifier.size(24.dp)) },
                    text = { Text("Add", fontWeight = FontWeight.Black) }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            val isDark = isTrueDarkMode
            // Premium subtle glowing background orbs (Drawn in Dark Mode only)
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (isDark) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF2F80ED).copy(alpha = 0.12f), Color.Transparent),
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.8f, size.height * 0.1f),
                            radius = size.width * 0.8f
                        ),
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.8f, size.height * 0.1f),
                        radius = size.width * 0.8f
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF9C27B0).copy(alpha = 0.08f), Color.Transparent),
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.6f),
                            radius = size.width * 0.7f
                        ),
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.6f),
                        radius = size.width * 0.7f
                    )
                }
            }
            if (selectedTab == 3) {
                ToolsScreenContent(
                    paddingValues = paddingValues,
                    onNavigateToCamera = onNavigateToCamera,
                    imagePickerLauncher = imagePickerLauncher,
                    filePickerLauncher = filePickerLauncher,
                    showTextToPdfDialog = { showTextToPdfDialog = true },
                    showCreateFolderDialog = { showCreateFolderDialog = true },
                    viewModel = viewModel,
                    onNavigateToReader = onNavigateToReader
                )
            } else {
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
                            Text("Silent", fontWeight = FontWeight.Black, fontSize = 24.sp, color = MaterialTheme.colorScheme.onBackground)
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
                                tint = if (isPinConfigured) Color(0xFFFF9500) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = { showSettingsSheet = true },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
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
                        placeholder = { Text("Search PDF files...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Outlined.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                            focusedBorderColor = Color(0xFF2F80ED),
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.width(10.dp))
                    
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
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
                                color = MaterialTheme.colorScheme.onBackground
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
                                color = MaterialTheme.colorScheme.onBackground
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
                            // CTA Add Folder Card (Wide horizontal format matching FolderCard size)
                            item {
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                                    ),
                                    modifier = Modifier
                                        .width(185.dp)
                                        .height(68.dp)
                                        .shadow(0.5.dp, RoundedCornerShape(14.dp))
                                        .clickable { showCreateFolderDialog = true }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = Color(0xFF2F80ED),
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Add Folder",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
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
                        color = MaterialTheme.colorScheme.onBackground
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
        } // Close the else block
        } // Close the Box wrapping LazyColumn
    }

    // Settings Modal Bottom Sheet
    if (showCreateSheet) {
        CreateOptionsSheet(
            onDismiss = { showCreateSheet = false },
            onImportPdf = { filePickerLauncher.launch(arrayOf("application/pdf")) },
            onImagesToPdfClick = { imagePickerLauncher.launch("image/*") },
            onTextToPdfClick = { showTextToPdfDialog = true },
            onScanToPdfClick = { onNavigateToCamera() }
        )
    }

    if (showImagesToPdfDialog) {
        ImagesToPdfDialog(
            imageUris = selectedImagesForPdf,
            onDismiss = { showImagesToPdfDialog = false },
            onPdfCreated = { uri ->
                val file = java.io.File(uri.path!!)
                viewModel.importPdf(uri, file.name, file.length())
            }
        )
    }

    if (showTextToPdfDialog) {
        TextToPdfDialog(
            onDismiss = { showTextToPdfDialog = false },
            onPdfCreated = { uri ->
                val file = java.io.File(uri.path!!)
                viewModel.importPdf(uri, file.name, file.length())
            }
        )
    }

    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
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
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                ListItem(
                    headlineContent = { Text("App Security (PIN)", color = MaterialTheme.colorScheme.onSurface) },
                    leadingContent = { Icon(Icons.Outlined.Lock, null, tint = Color(0xFF2F80ED)) },
                    modifier = Modifier.clickable { showSettingsSheet = false; showSecurityDialog = true },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                ListItem(
                    headlineContent = { Text("Help / Support", color = MaterialTheme.colorScheme.onSurface) },
                    leadingContent = { Icon(Icons.Outlined.SupportAgent, null, tint = Color(0xFF2F80ED)) },
                    modifier = Modifier.clickable { showSettingsSheet = false; showSupportDialog = true },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                ListItem(
                    headlineContent = { Text("Sort Options", color = MaterialTheme.colorScheme.onSurface) },
                    leadingContent = { Icon(Icons.Outlined.Sort, null, tint = Color(0xFF2F80ED)) },
                    modifier = Modifier.clickable { showSettingsSheet = false; showSortMenu = true },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                ListItem(
                    headlineContent = { Text("Toggle View Mode", color = MaterialTheme.colorScheme.onSurface) },
                    leadingContent = { Icon(if (isGridView) Icons.Outlined.ViewList else Icons.Outlined.GridView, null, tint = Color(0xFF2F80ED)) },
                    modifier = Modifier.clickable { showSettingsSheet = false; viewModel.toggleGridView() },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                ListItem(
                    headlineContent = { Text("True Dark Mode", color = MaterialTheme.colorScheme.onSurface) },
                    supportingContent = { Text(if (isTrueDarkMode) "Active (OLED Black)" else "Inactive (Light Mode)", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
                    leadingContent = { Icon(Icons.Outlined.Contrast, null, tint = Color(0xFF2F80ED)) },
                    trailingContent = {
                        Switch(
                            checked = isTrueDarkMode,
                            onCheckedChange = { viewModel.toggleTrueDarkMode() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF2F80ED),
                                checkedTrackColor = Color(0xFF2F80ED).copy(alpha = 0.4f)
                            )
                        )
                    },
                    modifier = Modifier.clickable { viewModel.toggleTrueDarkMode() },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }
    }

    // Interactive Dialogs
    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false; folderNameInput = "" },
            title = { Text("Create New Folder", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter the name of the folder you want to create to organize your books.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = folderNameInput,
                        onValueChange = { folderNameInput = it },
                        placeholder = { Text("Example: Islamic Lectures", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                            focusedBorderColor = Color(0xFF2F80ED),
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface
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
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (pdfToMoveToFolder != null) {
        val pdf = pdfToMoveToFolder!!
        var customCategoryInput by remember { mutableStateOf("") }
        var selectedCategoryToMove by remember { mutableStateOf(pdf.category ?: "") }

        AlertDialog(
            onDismissRequest = { pdfToMoveToFolder = null },
            title = { Text("Move to Folder", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Select the folder you want to move '${pdf.fileName}' to:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            Text("Default Folder", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
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
                                Text(cat, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Or create a new folder:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        OutlinedTextField(
                            value = customCategoryInput,
                            onValueChange = {
                                customCategoryInput = it
                                if (it.isNotBlank()) selectedCategoryToMove = it.trim()
                            },
                            placeholder = { Text("Enter new name...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                                focusedBorderColor = Color(0xFF2F80ED),
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface
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
                TextButton(onClick = { pdfToMoveToFolder = null }) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)) }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showSupportDialog) {
        AlertDialog(
            onDismissRequest = { showSupportDialog = false },
            title = { Text("Help / Support", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("If you encounter any issues, please contact us via WhatsApp.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                TextButton(onClick = { showSupportDialog = false }) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)) }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (pdfToDelete != null) {
        val pdf = pdfToDelete!!
        AlertDialog(
            onDismissRequest = { pdfToDelete = null },
            title = { Text("Delete PDF", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("Are you sure you want to delete '${pdf.fileName}'? This action cannot be undone.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Button(
                    onClick = { viewModel.deletePdf(pdf); pdfToDelete = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) { Text("OK", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { pdfToDelete = null }) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)) }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showSortMenu) {
        AlertDialog(
            onDismissRequest = { showSortMenu = false },
            title = { Text("Sort Books", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
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
                                tint = if (sortBy == index) Color(0xFF2F80ED) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = title,
                                modifier = Modifier.weight(1f),
                                fontWeight = if (sortBy == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (sortBy == index) Color(0xFF2F80ED) else MaterialTheme.colorScheme.onSurface
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
            dismissButton = { TextButton(onClick = { showSortMenu = false }) { Text("Close", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)) } },
            containerColor = MaterialTheme.colorScheme.surface,
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
                    Text("App PIN is currently active. Your documents are safe.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (error != null) Text(error ?: "", color = Color(0xFFEF4444), fontSize = 12.sp)
                    OutlinedTextField(
                        value = currentPinInput,
                        onValueChange = { if (it.length <= 4) { currentPinInput = it; error = null } },
                        label = { Text("Enter PIN to disable", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
                        placeholder = { Text("4 digits", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                            focusedBorderColor = Color(0xFF2F80ED),
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                } else if (step == 1) {
                    Text("Please enter a 4-digit PIN to lock the app.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (error != null) Text(error ?: "", color = Color(0xFFEF4444), fontSize = 12.sp)
                    OutlinedTextField(
                        value = pinText,
                        onValueChange = { if (it.length <= 4) { pinText = it; error = null } },
                        label = { Text("New PIN", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
                        placeholder = { Text("4 digits", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                            focusedBorderColor = Color(0xFF2F80ED),
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                } else if (step == 2) {
                    Text("Repeat the PIN you just entered to confirm.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (error != null) Text(error ?: "", color = Color(0xFFEF4444), fontSize = 12.sp)
                    OutlinedTextField(
                        value = confirmPinText,
                        onValueChange = { if (it.length <= 4) { confirmPinText = it; error = null } },
                        label = { Text("Confirm PIN", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
                        placeholder = { Text("4 digits", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                            focusedBorderColor = Color(0xFF2F80ED),
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface
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
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)) } },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp)
    )
}
