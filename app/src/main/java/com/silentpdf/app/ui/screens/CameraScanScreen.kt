package com.silentpdf.app.ui.screens

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.silentpdf.app.ui.viewmodel.SilentPdfViewModel
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScanScreen(
    viewModel: SilentPdfViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var showImagesToPdfDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    if (showImagesToPdfDialog) {
        ImagesToPdfDialog(
            imageUris = imageUris,
            onDismiss = { showImagesToPdfDialog = false },
            onPdfCreated = { uri ->
                val file = java.io.File(uri.path!!)
                viewModel.importPdf(uri, file.name, file.length())
                onNavigateBack()
            }
        )
    }

    if (cameraPermissionState.status.isGranted) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            CameraPreviewView(
                onImageCaptured = { uri ->
                    imageUris = imageUris + uri
                }
            )

            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                if (imageUris.isNotEmpty()) {
                    Button(
                        onClick = { showImagesToPdfDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2F80ED))
                    ) {
                        Text("Finish (${imageUris.size})", fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Check, contentDescription = "Finish", modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Bottom Gallery
            if (imageUris.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = 120.dp, start = 16.dp, end = 16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(imageUris) { uri ->
                        Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp))) {
                            Image(
                                painter = rememberAsyncImagePainter(uri),
                                contentDescription = "Scanned Image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = { imageUris = imageUris - uri },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(20.dp)
                                    .background(Color.Red.copy(alpha = 0.8f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Camera permission is required to scan documents.")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                    Text("Grant Permission")
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onNavigateBack) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
fun CameraPreviewView(
    onImageCaptured: (Uri) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    val executor = remember { Executors.newSingleThreadExecutor() }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )

                        // Enable tap-to-focus
                        previewView.setOnTouchListener { _, event ->
                            if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                                val factory = previewView.meteringPointFactory
                                val point = factory.createPoint(event.x, event.y)
                                val action = FocusMeteringAction.Builder(point).build()
                                camera.cameraControl.startFocusAndMetering(action)
                            }
                            true
                        }
                    } catch (e: Exception) {
                        Log.e("CameraPreviewView", "Binding failed", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )

        // Document Scan Overlay
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            
            // Define the document capture area (e.g., 80% width, aspect ratio 3:4 approx A4)
            val rectWidth = canvasWidth * 0.85f
            val rectHeight = rectWidth * 1.3f
            val left = (canvasWidth - rectWidth) / 2f
            val top = (canvasHeight - rectHeight) / 2f
            
            // Draw dark overlay around the rectangle
            val path = androidx.compose.ui.graphics.Path().apply {
                addRect(androidx.compose.ui.geometry.Rect(0f, 0f, canvasWidth, canvasHeight))
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        left = left,
                        top = top,
                        right = left + rectWidth,
                        bottom = top + rectHeight,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(32f, 32f)
                    )
                )
                fillType = androidx.compose.ui.graphics.PathFillType.EvenOdd
            }
            drawPath(path, color = Color.Black.copy(alpha = 0.5f))
            
            // Draw corner brackets
            val bracketLength = 80f
            val strokeW = 10f
            val bracketColor = Color(0xFF2F80ED)
            
            // Top Left
            drawLine(bracketColor, androidx.compose.ui.geometry.Offset(left, top + bracketLength), androidx.compose.ui.geometry.Offset(left, top), strokeW, androidx.compose.ui.graphics.StrokeCap.Round)
            drawLine(bracketColor, androidx.compose.ui.geometry.Offset(left, top), androidx.compose.ui.geometry.Offset(left + bracketLength, top), strokeW, androidx.compose.ui.graphics.StrokeCap.Round)
            
            // Top Right
            drawLine(bracketColor, androidx.compose.ui.geometry.Offset(left + rectWidth - bracketLength, top), androidx.compose.ui.geometry.Offset(left + rectWidth, top), strokeW, androidx.compose.ui.graphics.StrokeCap.Round)
            drawLine(bracketColor, androidx.compose.ui.geometry.Offset(left + rectWidth, top), androidx.compose.ui.geometry.Offset(left + rectWidth, top + bracketLength), strokeW, androidx.compose.ui.graphics.StrokeCap.Round)
            
            // Bottom Left
            drawLine(bracketColor, androidx.compose.ui.geometry.Offset(left, top + rectHeight - bracketLength), androidx.compose.ui.geometry.Offset(left, top + rectHeight), strokeW, androidx.compose.ui.graphics.StrokeCap.Round)
            drawLine(bracketColor, androidx.compose.ui.geometry.Offset(left, top + rectHeight), androidx.compose.ui.geometry.Offset(left + bracketLength, top + rectHeight), strokeW, androidx.compose.ui.graphics.StrokeCap.Round)
            
            // Bottom Right
            drawLine(bracketColor, androidx.compose.ui.geometry.Offset(left + rectWidth, top + rectHeight - bracketLength), androidx.compose.ui.geometry.Offset(left + rectWidth, top + rectHeight), strokeW, androidx.compose.ui.graphics.StrokeCap.Round)
            drawLine(bracketColor, androidx.compose.ui.geometry.Offset(left + rectWidth - bracketLength, top + rectHeight), androidx.compose.ui.geometry.Offset(left + rectWidth, top + rectHeight), strokeW, androidx.compose.ui.graphics.StrokeCap.Round)
        }

        // Capture Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .size(72.dp)
                .background(Color.White, CircleShape)
                .clickable {
                    val file = File(context.cacheDir, "scan_${System.currentTimeMillis()}.jpg")
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

                    imageCapture?.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                onImageCaptured(Uri.fromFile(file))
                            }

                            override fun onError(exception: ImageCaptureException) {
                                Log.e("CameraPreviewView", "Photo capture failed", exception)
                                Toast.makeText(context, "Failed to capture photo", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(Color.LightGray, CircleShape)
            )
        }
    }
}
