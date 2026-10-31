package com.silentpdf.app.ui.viewmodel.controllers

import android.app.Application
import android.net.Uri
import com.silentpdf.app.data.db.PdfEntity
import com.silentpdf.app.data.repository.PdfRenderEngine
import com.silentpdf.app.data.repository.PdfRepository
import java.io.File
import java.io.FileInputStream

class PdfEditController(
    private val application: Application,
    private val repository: PdfRepository,
    private val renderEngine: PdfRenderEngine
) {
    suspend fun rotatePages(pdf: PdfEntity, pageIndices: List<Int>): Boolean {
        val destFile = File(application.cacheDir, "temp_rotated.pdf")
        val success = com.silentpdf.app.util.PdfPageManager.rotatePages(application, Uri.parse(pdf.uriString), pageIndices, destFile)
        if (success) {
            return replacePdfWithTemp(pdf, destFile)
        }
        return false
    }

    suspend fun deletePages(pdf: PdfEntity, pageIndices: List<Int>): Boolean {
        val destFile = File(application.cacheDir, "temp_deleted.pdf")
        val success = com.silentpdf.app.util.PdfPageManager.deletePages(application, Uri.parse(pdf.uriString), pageIndices, destFile)
        if (success) {
            return replacePdfWithTemp(pdf, destFile)
        }
        return false
    }

    suspend fun insertBlankPage(pdf: PdfEntity, afterPageIndex: Int): Boolean {
        val destFile = File(application.cacheDir, "temp_inserted.pdf")
        val success = com.silentpdf.app.util.PdfPageManager.insertBlankPage(application, Uri.parse(pdf.uriString), afterPageIndex, destFile)
        if (success) {
            return replacePdfWithTemp(pdf, destFile)
        }
        return false
    }

    suspend fun extractPages(pdf: PdfEntity, pageIndices: List<Int>): Boolean {
        val destFile = File(application.filesDir, "extracted_${System.currentTimeMillis()}.pdf")
        val success = com.silentpdf.app.util.PdfPageManager.extractPages(application, Uri.parse(pdf.uriString), pageIndices, destFile)
        if (success) {
            val extractedPdf = PdfEntity(
                fileName = "${pdf.fileName.substringBeforeLast(".")}_extracted.pdf",
                uriString = Uri.fromFile(destFile).toString(),
                fileSize = destFile.length(),
                lastAccessTime = System.currentTimeMillis()
            )
            repository.insertOrUpdatePdf(extractedPdf)
            return true
        }
        return false
    }

    suspend fun burnDrawingsIntoPdf(pdf: PdfEntity, drawings: Map<Int, List<com.silentpdf.app.ui.viewmodel.DrawingStroke>>, destinationFile: File): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val originalUri = Uri.parse(pdf.uriString)
            application.contentResolver.openFileDescriptor(originalUri, "r")?.use { pfd ->
                com.tom_roush.pdfbox.pdmodel.PDDocument.load(FileInputStream(pfd.fileDescriptor), com.tom_roush.pdfbox.io.MemoryUsageSetting.setupTempFileOnly()).use { document ->
                    drawings.forEach { (pageIdx, strokes) ->
                        if (pageIdx in 0 until document.numberOfPages && strokes.isNotEmpty()) {
                            val page = document.getPage(pageIdx)
                            val cropBox = page.cropBox ?: page.mediaBox
                            val pdfWidth = cropBox.width
                            val pdfHeight = cropBox.height

                            val contentStream = com.tom_roush.pdfbox.pdmodel.PDPageContentStream(
                                document, page, com.tom_roush.pdfbox.pdmodel.PDPageContentStream.AppendMode.APPEND, true, true
                            )
                            strokes.forEach { stroke ->
                                if (stroke.points.size > 1 && !stroke.isEraser) {
                                    contentStream.setStrokingColor(stroke.color.red, stroke.color.green, stroke.color.blue)
                                    contentStream.setLineWidth(stroke.width.coerceIn(1f, 20f))
                                    val first = stroke.points.first()
                                    // Scale to PDF coordinate system (points bottom-left origin)
                                    val startX = first.x.coerceIn(0f, pdfWidth)
                                    val startY = (pdfHeight - first.y).coerceIn(0f, pdfHeight)
                                    contentStream.moveTo(startX, startY)
                                    for (i in 1 until stroke.points.size) {
                                        val pt = stroke.points[i]
                                        val x = pt.x.coerceIn(0f, pdfWidth)
                                        val y = (pdfHeight - pt.y).coerceIn(0f, pdfHeight)
                                        contentStream.lineTo(x, y)
                                    }
                                    contentStream.stroke()
                                }
                            }
                            contentStream.close()
                        }
                    }
                    java.io.FileOutputStream(destinationFile).use { out ->
                        document.save(out)
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private suspend fun replacePdfWithTemp(pdf: PdfEntity, tempFile: File): Boolean {
        return try {
            val originalUri = Uri.parse(pdf.uriString)
            
            // 1. Release active file descriptor lock in render engine
            renderEngine.closeDocument()

            // 2. Atomic copy with backup
            if (originalUri.scheme == "file") {
                val originalFile = File(originalUri.path!!)
                val backupFile = File(application.cacheDir, "backup_${System.currentTimeMillis()}.pdf")
                if (originalFile.exists()) {
                    originalFile.copyTo(backupFile, overwrite = true)
                }
                try {
                    tempFile.copyTo(originalFile, overwrite = true)
                    backupFile.delete()
                } catch (e: Exception) {
                    if (backupFile.exists()) {
                        backupFile.copyTo(originalFile, overwrite = true)
                        backupFile.delete()
                    }
                    throw e
                }
            } else {
                application.contentResolver.openOutputStream(originalUri, "rwt")?.use { out ->
                    FileInputStream(tempFile).use { input ->
                        input.copyTo(out)
                    }
                }
            }
            // Update file size in DB
            val updatedSize = if (originalUri.scheme == "file") {
                File(originalUri.path!!).length()
            } else {
                application.contentResolver.openFileDescriptor(originalUri, "r")?.statSize ?: pdf.fileSize
            }
            repository.insertOrUpdatePdf(pdf.copy(fileSize = updatedSize))
            
            // Reload the PDF engine
            renderEngine.openDocument(originalUri)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                renderEngine.openDocument(Uri.parse(pdf.uriString))
            } catch (ex: Exception) {
                // ignore
            }
            false
        }
    }
}
