package com.silentpdf.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PdfRenderEngine(private val context: Context) {

    private var fileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null
    private var currentUri: Uri? = null
    private var documentGeneration: Long = 0L
    
    private val renderLock = Any()

    // Dynamically calculate cache memory limit: 1/8th of JVM max heap, bounded between 16MB and 64MB
    private val maxCacheMemoryKb: Int = run {
        val maxMemKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        (maxMemKb / 8).coerceIn(16 * 1024, 64 * 1024)
    }

    // Memory-aware LRU cache using composite document-aware keys (Uri + PageIndex + TargetWidth)
    private val bitmapCache = object : android.util.LruCache<String, Bitmap>(maxCacheMemoryKb) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            val byteCount = try {
                bitmap.byteCount
            } catch (e: Exception) {
                bitmap.width * bitmap.height * 4
            }
            return (byteCount / 1024).coerceAtLeast(1)
        }
    }

    private fun buildCacheKey(uri: Uri?, pageIndex: Int, width: Int): String {
        return "${uri?.toString().orEmpty()}#$pageIndex#$width"
    }

    suspend fun openDocument(uri: Uri, password: String? = null): Int = withContext(Dispatchers.IO) {
        closeDocument()
        synchronized(renderLock) {
            try {
                currentUri = uri
                documentGeneration++
                val resolver = context.contentResolver
                fileDescriptor = resolver.openFileDescriptor(uri, "r")
                fileDescriptor?.let { fd ->
                    pdfRenderer = createPdfRendererWithPassword(fd, password)
                    return@withContext pdfRenderer?.pageCount ?: 0
                }
            } catch (e: SecurityException) {
                Log.e("PdfRenderEngine", "SecurityException opening PDF document directly", e)
                throw e
            } catch (e: Exception) {
                Log.e("PdfRenderEngine", "Failed to open PDF document directly, trying fallback", e)
                // Fallback: Copy PDF stream to local cache directory and open it
                try {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val tempFile = File(context.cacheDir, "temp_render_file.pdf")
                        if (tempFile.exists()) tempFile.delete()
                        FileOutputStream(tempFile).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                        fileDescriptor = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
                        fileDescriptor?.let { fd ->
                            pdfRenderer = createPdfRendererWithPassword(fd, password)
                            return@withContext pdfRenderer?.pageCount ?: 0
                        }
                    }
                } catch (ex: SecurityException) {
                    Log.e("PdfRenderEngine", "SecurityException during fallback", ex)
                    throw ex
                } catch (ex: Exception) {
                    Log.e("PdfRenderEngine", "Fallback stream copying failed", ex)
                }
            }
            return@withContext 0
        }
    }

    /**
     * Utilizes reflection to invoke Android 15 (API 35+) password-protected PdfRenderer APIs.
     * Keeps code 100% compile-safe on older SDK classpaths while being fully operational on real devices.
     */
    private fun createPdfRendererWithPassword(fd: ParcelFileDescriptor, password: String?): PdfRenderer {
        if (password != null && android.os.Build.VERSION.SDK_INT >= 35) {
            try {
                val builderClass = Class.forName("android.graphics.pdf.PdfRenderer\$LoadParams\$Builder")
                val builder = builderClass.getDeclaredConstructor().newInstance()
                val setPasswordMethod = builderClass.getMethod("setPassword", String::class.java)
                setPasswordMethod.invoke(builder, password)
                val loadParamsClass = Class.forName("android.graphics.pdf.PdfRenderer\$LoadParams")
                val buildMethod = builderClass.getMethod("build")
                val loadParams = buildMethod.invoke(builder)
                
                val pdfRendererClass = PdfRenderer::class.java
                val constructor = pdfRendererClass.getConstructor(ParcelFileDescriptor::class.java, loadParamsClass)
                return constructor.newInstance(fd, loadParams) as PdfRenderer
            } catch (e: Exception) {
                Log.e("PdfRenderEngine", "Failed to load via reflection, falling back to standard constructor", e)
            }
        }
        return PdfRenderer(fd)
    }

    suspend fun renderPage(pageIndex: Int, targetWidth: Int): Bitmap? = withContext(Dispatchers.IO) {
        ensureActive()

        // Quick lock-free cache check
        val activeUri = currentUri ?: return@withContext null
        val cacheKey = buildCacheKey(activeUri, pageIndex, targetWidth)
        val cached = bitmapCache.get(cacheKey)
        if (cached != null && !cached.isRecycled) {
            return@withContext cached
        }

        synchronized(renderLock) {
            if (!isActive) return@withContext null

            val renderer = pdfRenderer ?: return@withContext null
            val currentGen = documentGeneration
            val uri = currentUri ?: return@withContext null
            val key = buildCacheKey(uri, pageIndex, targetWidth)

            // Double check cache after acquiring lock
            val lockedCached = bitmapCache.get(key)
            if (lockedCached != null && !lockedCached.isRecycled) {
                return@withContext lockedCached
            }

            val count = renderer.pageCount
            if (pageIndex < 0 || pageIndex >= count) return@withContext null
    
            try {
                val page = renderer.openPage(pageIndex)
                try {
                    if (!isActive) return@withContext null

                    val originalWidth = page.width.coerceAtLeast(1)
                    val originalHeight = page.height.coerceAtLeast(1)
    
                    // Scale factor to downscale large high-dpi page bitmaps for mobile widths
                    val scale = if (targetWidth > 0) {
                        targetWidth.toFloat() / originalWidth.toFloat()
                    } else {
                        1.0f
                    }
    
                    // Cap maximum width of rendered pages to prevent massive native memory allocations
                    val finalScale = if (originalWidth * scale > 1800f) {
                        1800f / originalWidth.toFloat()
                    } else {
                        scale
                    }
    
                    val renderWidth = (originalWidth * finalScale).toInt().coerceAtLeast(100)
                    val renderHeight = (originalHeight * finalScale).toInt().coerceAtLeast(100)
    
                    val bitmap = try {
                        Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888).apply {
                            eraseColor(android.graphics.Color.WHITE)
                        }
                    } catch (e: OutOfMemoryError) {
                        Log.e("PdfRenderEngine", "OOM when creating bitmap for page $pageIndex", e)
                        System.gc()
                        val smallerWidth = (renderWidth / 2).coerceAtLeast(100)
                        val smallerHeight = (renderHeight / 2).coerceAtLeast(100)
                        Bitmap.createBitmap(smallerWidth, smallerHeight, Bitmap.Config.ARGB_8888).apply {
                            eraseColor(android.graphics.Color.WHITE)
                        }
                    }

                    if (!isActive) return@withContext null
    
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
    
                    // Only cache and return if document is still active and unchanged
                    if (documentGeneration == currentGen && currentUri == uri) {
                        bitmapCache.put(key, bitmap)
                        return@withContext bitmap
                    } else {
                        return@withContext null
                    }
                } finally {
                    try {
                        page.close()
                    } catch (e: Exception) {
                        Log.e("PdfRenderEngine", "Error closing page $pageIndex", e)
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: OutOfMemoryError) {
                Log.e("PdfRenderEngine", "OOM during renderPage $pageIndex", e)
                return@withContext null
            } catch (e: Exception) {
                Log.e("PdfRenderEngine", "Failed to render page $pageIndex", e)
                return@withContext null
            }
        }
    }

    fun getPageCount(): Int {
        return pdfRenderer?.pageCount ?: 0
    }

    fun closeDocument() {
        synchronized(renderLock) {
            documentGeneration++
            // Evict cache to drop references; GC will reclaim native memory safely
            bitmapCache.evictAll()
    
            try {
                pdfRenderer?.close()
            } catch (e: Exception) {
                Log.e("PdfRenderEngine", "Failed to close PdfRenderer", e)
            }
            try {
                fileDescriptor?.close()
            } catch (e: Exception) {
                Log.e("PdfRenderEngine", "Failed to close fileDescriptor", e)
            }
            pdfRenderer = null
            fileDescriptor = null
            currentUri = null
        }
    }
}
