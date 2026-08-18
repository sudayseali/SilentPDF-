package com.silentpdf.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PdfRenderEngine(private val context: Context) {

    private var fileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null
    private var currentUri: Uri? = null
    
    private val renderLock = Any()

    // Cache of page bitmaps to avoid redundant rendering
    // Uses an LRU cache with max 3 active bitmaps to save RAM
    private val bitmapCache = object : android.util.LruCache<Int, Bitmap>(3) {}

    suspend fun openDocument(uri: Uri, password: String? = null): Int = withContext(Dispatchers.IO) {
        closeDocument()
        synchronized(renderLock) {
            try {
                currentUri = uri
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
        synchronized(renderLock) {
            val renderer = pdfRenderer ?: return@withContext null
            val count = renderer.pageCount
            if (pageIndex < 0 || pageIndex >= count) return@withContext null
    
            val cached = bitmapCache.get(pageIndex)
            if (cached != null && !cached.isRecycled) {
                return@withContext cached
            }
    
            try {
                val page = renderer.openPage(pageIndex)
                try {
                    val originalWidth = page.width
                    val originalHeight = page.height
    
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
                        System.gc() // Hint GC
                        // Fallback to smaller bitmap to save memory
                        val smallerWidth = (renderWidth / 2).coerceAtLeast(100)
                        val smallerHeight = (renderHeight / 2).coerceAtLeast(100)
                        Bitmap.createBitmap(smallerWidth, smallerHeight, Bitmap.Config.ARGB_8888).apply {
                            eraseColor(android.graphics.Color.WHITE)
                        }
                    }
    
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
    
                    bitmapCache.put(pageIndex, bitmap)
                    return@withContext bitmap
                } finally {
                    try {
                        page.close()
                    } catch (e: Exception) {
                        Log.e("PdfRenderEngine", "Error closing page $pageIndex", e)
                    }
                }
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
            // Let the GC handle native memory. Recycling here causes "Canvas: trying to use a recycled bitmap"
            // if Compose or ML Kit are currently holding references during a PDF close.
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
