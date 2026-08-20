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

    private suspend fun replacePdfWithTemp(pdf: PdfEntity, tempFile: File): Boolean {
        return try {
            val originalUri = Uri.parse(pdf.uriString)
            if (originalUri.scheme == "file") {
                val originalFile = File(originalUri.path!!)
                tempFile.copyTo(originalFile, overwrite = true)
            } else {
                application.contentResolver.openOutputStream(originalUri)?.use { out ->
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
            false
        }
    }
}
