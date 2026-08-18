package com.silentpdf.app.search.engine

import android.content.Context
import android.graphics.RectF
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.IOException
import kotlin.math.max
import kotlin.math.min

class TextExtractionEngine(private val context: Context) {

    data class PageTextResult(val hasText: Boolean, val words: List<Pair<String, RectF>>)

    suspend fun extractPageText(uri: Uri, pageIndex: Int): PageTextResult = withContext(Dispatchers.IO) {
        var result = PageTextResult(false, emptyList())
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                val document = PDDocument.load(FileInputStream(pfd.fileDescriptor), com.tom_roush.pdfbox.io.MemoryUsageSetting.setupTempFileOnly())
                val page = document.getPage(pageIndex)
                val stripper = BoundingBoxStripper()
                stripper.startPage = pageIndex + 1
                stripper.endPage = pageIndex + 1
                
                val cropBox = page.cropBox ?: page.mediaBox
                val rotation = page.rotation
                if (rotation == 90 || rotation == 270) {
                    stripper.pageWidth = cropBox.height
                    stripper.pageHeight = cropBox.width
                } else {
                    stripper.pageWidth = cropBox.width
                    stripper.pageHeight = cropBox.height
                }
                
                stripper.getText(document)
                val words = stripper.wordBounds
                result = PageTextResult(words.isNotEmpty(), words)
                document.close()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext result
    }

    private class BoundingBoxStripper : PDFTextStripper() {
        val wordBounds = mutableListOf<Pair<String, RectF>>()
        var pageWidth = 1f
        var pageHeight = 1f

        @Throws(IOException::class)
        override fun writeString(text: String?, textPositions: MutableList<TextPosition>?) {
            super.writeString(text, textPositions)
            if (!text.isNullOrBlank() && !textPositions.isNullOrEmpty()) {
                var minX = Float.MAX_VALUE
                var minY = Float.MAX_VALUE
                var maxX = Float.MIN_VALUE
                var maxY = Float.MIN_VALUE

                for (pos in textPositions) {
                    minX = min(minX, pos.xDirAdj)
                    minY = min(minY, pos.yDirAdj - pos.heightDir)
                    maxX = max(maxX, pos.xDirAdj + pos.widthDirAdj)
                    maxY = max(maxY, pos.yDirAdj)
                }

                if (pageWidth > 0 && pageHeight > 0) {
                    val normRect = RectF(
                        max(0f, minX / pageWidth),
                        max(0f, minY / pageHeight),
                        min(1f, maxX / pageWidth),
                        min(1f, maxY / pageHeight)
                    )
                    wordBounds.add(Pair(text.trim(), normRect))
                }
            }
        }
    }
}
