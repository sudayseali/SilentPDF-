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
            val pfd: ParcelFileDescriptor? = context.contentResolver.openFileDescriptor(uri, "r")
            
            if (pfd != null) {
                val pdfRenderer = PdfRenderer(pfd)
                if (pdfRenderer.pageCount > 0) {
                    val page = pdfRenderer.openPage(0)
                    
                    // Render to a reasonably sized bitmap for thumbnail
                    val width = 400
                    val height = (width.toFloat() / page.width * page.height).toInt()
                    
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    
                    // White background
                    val canvas = android.graphics.Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)
                    
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    
                    val out = FileOutputStream(thumbnailFile)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                    out.flush()
                    out.close()
                    bitmap.recycle()
                    
                    pdfRenderer.close()
                    pfd.close()
                    
                    return@withContext thumbnailFile
                }
                pdfRenderer.close()
                pfd.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }
}
