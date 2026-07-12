package com.example.data.repository

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

    // Cache of page bitmaps to avoid redundant rendering
    // Uses an LRU cache with max 3 active bitmaps to save RAM
    // Do NOT manually call recycle() when evicting because Compose UI might still be drawing them. Let GC handle it.
    private val bitmapCache = object : android.util.LruCache<Int, Bitmap>(3) {}

    suspend fun openDocument(uri: Uri): Int = withContext(Dispatchers.IO) {
        closeDocument()
        try {
            currentUri = uri
            val resolver = context.contentResolver
            fileDescriptor = resolver.openFileDescriptor(uri, "r")
            fileDescriptor?.let { fd ->
                pdfRenderer = PdfRenderer(fd)
                return@withContext pdfRenderer?.pageCount ?: 0
            }
        } catch (e: Exception) {
            Log.e("PdfRenderEngine", "Failed to open PDF document directly", e)
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
                        pdfRenderer = PdfRenderer(fd)
                        return@withContext pdfRenderer?.pageCount ?: 0
                    }
                }
            } catch (ex: Exception) {
                Log.e("PdfRenderEngine", "Fallback stream copying failed", ex)
            }
        }
        return@withContext 0
    }

    suspend fun renderPage(pageIndex: Int, targetWidth: Int): Bitmap? = withContext(Dispatchers.IO) {
        val renderer = pdfRenderer ?: return@withContext null
        val count = renderer.pageCount
        if (pageIndex < 0 || pageIndex >= count) return@withContext null

        val cached = bitmapCache.get(pageIndex)
        if (cached != null && !cached.isRecycled) {
            return@withContext cached
        }

        try {

            val page = renderer.openPage(pageIndex)
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
                Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888)
            } catch (e: OutOfMemoryError) {
                Log.e("PdfRenderEngine", "OOM when creating bitmap for page $pageIndex", e)
                System.gc() // Hint GC
                // Fallback to smaller bitmap to save memory
                val smallerWidth = (renderWidth / 2).coerceAtLeast(100)
                val smallerHeight = (renderHeight / 2).coerceAtLeast(100)
                Bitmap.createBitmap(smallerWidth, smallerHeight, Bitmap.Config.ARGB_8888)
            }

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

            bitmapCache.put(pageIndex, bitmap)
            return@withContext bitmap
        } catch (e: OutOfMemoryError) {
            Log.e("PdfRenderEngine", "OOM during renderPage $pageIndex", e)
            return@withContext null
        } catch (e: Exception) {
            Log.e("PdfRenderEngine", "Failed to render page $pageIndex", e)
            return@withContext null
        }
    }

    fun closeDocument() {
        bitmapCache.evictAll()

        try {
            pdfRenderer?.close()
        } catch (e: Exception) {
            Log.e("PdfRenderEngine", "Error closing PdfRenderer", e)
        } finally {
            pdfRenderer = null
        }

        try {
            fileDescriptor?.close()
        } catch (e: Exception) {
            Log.e("PdfRenderEngine", "Error closing FileDescriptor", e)
        } finally {
            fileDescriptor = null
        }
        currentUri = null
    }
}
