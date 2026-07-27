package com.silentpdf.app.util

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object PdfPageManager {
    suspend fun rotatePages(context: Context, sourceUri: Uri, pageIndices: List<Int>, destinationFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openFileDescriptor(sourceUri, "r")?.use { pfd ->
                PDDocument.load(FileInputStream(pfd.fileDescriptor), com.tom_roush.pdfbox.io.MemoryUsageSetting.setupTempFileOnly()).use { document ->
                    pageIndices.forEach { index ->
                        if (index in 0 until document.numberOfPages) {
                            val page = document.getPage(index)
                            val currentRotation = page.rotation
                            page.rotation = (currentRotation + 90) % 360
                        }
                    }
                    FileOutputStream(destinationFile).use { out ->
                        document.save(out)
                    }
                }
            }
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    suspend fun deletePages(context: Context, sourceUri: Uri, pageIndices: List<Int>, destinationFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openFileDescriptor(sourceUri, "r")?.use { pfd ->
                PDDocument.load(FileInputStream(pfd.fileDescriptor), com.tom_roush.pdfbox.io.MemoryUsageSetting.setupTempFileOnly()).use { document ->
                    // Sort indices in descending order so removing doesn't shift indices of remaining pages to be removed
                    val sortedIndices = pageIndices.sortedDescending()
                    sortedIndices.forEach { index ->
                        if (index in 0 until document.numberOfPages) {
                            document.removePage(index)
                        }
                    }
                    FileOutputStream(destinationFile).use { out ->
                        document.save(out)
                    }
                }
            }
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    suspend fun insertBlankPage(context: Context, sourceUri: Uri, afterPageIndex: Int, destinationFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openFileDescriptor(sourceUri, "r")?.use { pfd ->
                PDDocument.load(FileInputStream(pfd.fileDescriptor), com.tom_roush.pdfbox.io.MemoryUsageSetting.setupTempFileOnly()).use { document ->
                    val newPage = PDPage(PDRectangle.A4)
                    val indexToInsert = (afterPageIndex + 1).coerceIn(0, document.numberOfPages)
                    document.pages.insertBefore(newPage, if (indexToInsert < document.numberOfPages) document.getPage(indexToInsert) else null)
                    
                    FileOutputStream(destinationFile).use { out ->
                        document.save(out)
                    }
                }
            }
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    suspend fun extractPages(context: Context, sourceUri: Uri, pageIndices: List<Int>, destinationFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openFileDescriptor(sourceUri, "r")?.use { pfd ->
                PDDocument.load(FileInputStream(pfd.fileDescriptor), com.tom_roush.pdfbox.io.MemoryUsageSetting.setupTempFileOnly()).use { document ->
                    PDDocument().use { newDocument ->
                        val sortedIndices = pageIndices.sorted()
                        sortedIndices.forEach { index ->
                            if (index in 0 until document.numberOfPages) {
                                val page = document.getPage(index)
                                // create a shallow clone or just import
                                newDocument.addPage(newDocument.importPage(page))
                            }
                        }
                        FileOutputStream(destinationFile).use { out ->
                            newDocument.save(out)
                        }
                    }
                }
            }
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }
}
