package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object PdfThumbnailHelper {
    suspend fun getThumbnail(context: Context, uriString: String): File? = withContext(Dispatchers.IO) {
        try {
            val hash = uriString.hashCode().toString()
            val cacheDir = File(context.cacheDir, "pdf_thumbnails")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            val thumbnailFile = File(cacheDir, "$hash.jpg")
            
            if (thumbnailFile.exists() && thumbnailFile.length() > 0) {
                return@withContext thumbnailFile
            }
            
            val uri = Uri.parse(uriString)
            var pfd: ParcelFileDescriptor? = null
            var pdfRenderer: PdfRenderer? = null
            try {
                pfd = context.contentResolver.openFileDescriptor(uri, "r")
                if (pfd != null) {
                    pdfRenderer = PdfRenderer(pfd)
                    if (pdfRenderer.pageCount > 0) {
                        var page: PdfRenderer.Page? = null
                        try {
                            page = pdfRenderer.openPage(0)
                            
                            // Render to a reasonably sized bitmap for thumbnail
                            val width = 400
                            val height = (width.toFloat() / page.width * page.height).toInt().coerceAtLeast(1)
                            
                            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            try {
                                // White background
                                val canvas = android.graphics.Canvas(bitmap)
                                canvas.drawColor(Color.WHITE)
                                
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                
                                FileOutputStream(thumbnailFile).use { out ->
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                                    out.flush()
                                }
                                return@withContext thumbnailFile
                            } finally {
                                bitmap.recycle()
                            }
                        } finally {
                            try {
                                page?.close()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    pdfRenderer?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                try {
                    pfd?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }
}
