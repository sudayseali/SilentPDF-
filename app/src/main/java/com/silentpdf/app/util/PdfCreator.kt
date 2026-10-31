package com.silentpdf.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.text.StaticLayout
import android.text.TextPaint
import android.text.Layout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object PdfCreator {

    suspend fun createTextPdf(context: Context, text: String, title: String): Uri? = withContext(Dispatchers.IO) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        val titlePaint = TextPaint().apply {
            color = android.graphics.Color.BLACK
            textSize = 24f
            isFakeBoldText = true
        }

        val textPaint = TextPaint().apply {
            color = android.graphics.Color.BLACK
            textSize = 14f
        }

        var currentY = 50f
        val margin = 50f
        val maxTextWidth = pageInfo.pageWidth - (2 * margin)

        canvas.drawText(title, margin, currentY, titlePaint)
        currentY += 40f

        val staticLayout = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, maxTextWidth.toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(1f, 1f)
            .setIncludePad(false)
            .build()

        var line = 0
        while (line < staticLayout.lineCount) {
            val lineBottom = staticLayout.getLineBottom(line)
            if (currentY + (lineBottom - staticLayout.getLineTop(line)) > pageInfo.pageHeight - margin) {
                document.finishPage(page)
                page = document.startPage(pageInfo)
                canvas = page.canvas
                currentY = margin
            }
            
            val textToDraw = text.substring(staticLayout.getLineStart(line), staticLayout.getLineEnd(line))
            canvas.drawText(textToDraw, margin, currentY, textPaint)
            
            currentY += textPaint.descent() - textPaint.ascent()
            line++
        }

        document.finishPage(page)

        try {
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "${title.replace(" ", "_")}.pdf")
            document.writeTo(FileOutputStream(file))
            document.close()
            return@withContext Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            document.close()
            return@withContext null
        }
    }

    suspend fun createImagesPdf(context: Context, imageUris: List<Uri>, title: String): Uri? = withContext(Dispatchers.IO) {
        val document = PdfDocument()
        val a4Width = 595
        val a4Height = 842
        val margin = 20f

        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

        for ((index, uri) in imageUris.withIndex()) {
            try {
                // Decode bounds first to prevent huge memory spikes
                var inSample = 1
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeStream(stream, null, options)
                    val maxDim = maxOf(options.outWidth, options.outHeight)
                    while (maxDim / inSample > 2400) {
                        inSample *= 2
                    }
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = inSample
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }

                val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOptions)
                }

                if (bitmap != null) {
                    val pageInfo = PdfDocument.PageInfo.Builder(a4Width, a4Height, index + 1).create()
                    val page = document.startPage(pageInfo)
                    val canvas = page.canvas

                    // Calculate aspect-fit within A4 margins
                    val maxW = a4Width - (2 * margin)
                    val maxH = a4Height - (2 * margin)
                    val scale = minOf(maxW / bitmap.width.toFloat(), maxH / bitmap.height.toFloat())
                    val destW = bitmap.width * scale
                    val destH = bitmap.height * scale
                    val destX = margin + (maxW - destW) / 2f
                    val destY = margin + (maxH - destH) / 2f

                    val destRect = android.graphics.RectF(destX, destY, destX + destW, destY + destH)
                    canvas.drawBitmap(bitmap, null, destRect, paint)

                    document.finishPage(page)
                    bitmap.recycle()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        try {
            val safeName = title.trim().replace(" ", "_").let {
                if (it.endsWith(".pdf", ignoreCase = true)) it else "$it.pdf"
            }
            val docsFolder = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            val file = File(docsFolder, safeName)
            FileOutputStream(file).use { out ->
                document.writeTo(out)
            }
            document.close()
            return@withContext Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            try { document.close() } catch (ignored: Exception) {}
            return@withContext null
        }
    }
}
