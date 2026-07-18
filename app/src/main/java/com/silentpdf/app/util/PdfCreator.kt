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
        
        for ((index, uri) in imageUris.withIndex()) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap != null) {
                    val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
                    val page = document.startPage(pageInfo)
                    val canvas = page.canvas
                    canvas.drawBitmap(bitmap, 0f, 0f, Paint())
                    document.finishPage(page)
                    bitmap.recycle()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

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
}
