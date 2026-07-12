package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import com.example.data.db.BookmarkEntity
import com.example.data.db.PdfEntity
import com.example.data.db.SilentPdfDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class PdfRepository(
    private val context: Context,
    private val pdfDao: SilentPdfDao
) {
    // Reactive streams from Database
    val allPdfsByName: Flow<List<PdfEntity>> = pdfDao.getAllPdfsByNameAsc()
    val allPdfsByRecent: Flow<List<PdfEntity>> = pdfDao.getAllPdfsByRecent()
    val favoritePdfs: Flow<List<PdfEntity>> = pdfDao.getFavoritePdfs()

    suspend fun getPdfByUri(uriString: String): PdfEntity? = withContext(Dispatchers.IO) {
        pdfDao.getPdfByUri(uriString)
    }

    suspend fun insertOrUpdatePdf(pdf: PdfEntity) = withContext(Dispatchers.IO) {
        pdfDao.insertOrUpdatePdf(pdf)
    }

    suspend fun updateFavorite(uriString: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        pdfDao.updateFavorite(uriString, isFavorite)
    }

    suspend fun updateProgress(uriString: String, page: Int, totalPages: Int) = withContext(Dispatchers.IO) {
        val existing = pdfDao.getPdfByUri(uriString)
        val finalTotalPages = if (totalPages > 0) totalPages else (existing?.totalPages ?: 0)
        
        if (existing != null) {
            pdfDao.insertOrUpdatePdf(existing.copy(
                lastPageRead = page,
                totalPages = finalTotalPages,
                lastAccessTime = System.currentTimeMillis()
            ))
        } else {
            // Safe fallback if it's not yet scanned
            pdfDao.insertOrUpdatePdf(PdfEntity(
                uriString = uriString,
                fileName = uriString.substringAfterLast("/"),
                fileSize = 0L,
                lastAccessTime = System.currentTimeMillis(),
                lastPageRead = page,
                totalPages = finalTotalPages
            ))
        }
    }

    suspend fun deletePdf(uriString: String) = withContext(Dispatchers.IO) {
        pdfDao.deletePdf(uriString)
    }

    // Bookmarks operations
    fun getBookmarksForPdf(pdfUriString: String): Flow<List<BookmarkEntity>> {
        return pdfDao.getBookmarksForPdf(pdfUriString)
    }

    suspend fun addBookmark(pdfUriString: String, pageNumber: Int, label: String) = withContext(Dispatchers.IO) {
        pdfDao.insertBookmark(BookmarkEntity(
            pdfUriString = pdfUriString,
            pageNumber = pageNumber,
            label = label
        ))
    }

    suspend fun removeBookmark(id: Long) = withContext(Dispatchers.IO) {
        pdfDao.deleteBookmark(id)
    }

    suspend fun removeBookmarkForPage(pdfUriString: String, pageNumber: Int) = withContext(Dispatchers.IO) {
        pdfDao.deleteBookmarkForPage(pdfUriString, pageNumber)
    }

    // MediaStore scan to discover local PDF documents offline
    suspend fun scanLocalPdfs(): List<PdfEntity> = withContext(Dispatchers.IO) {
        val pdfList = mutableListOf<PdfEntity>()
        val contentResolver = context.contentResolver
        val collection = MediaStore.Files.getContentUri("external")
        
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.DATA
        )
        
        val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} = ? OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("application/pdf", "%.pdf")
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

        try {
            contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Unnamed PDF"
                    val size = cursor.getLong(sizeColumn)
                    val date = cursor.getLong(dateColumn) * 1000 // To milliseconds
                    val path = cursor.getString(dataColumn)
                    val contentUri = ContentUris.withAppendedId(collection, id).toString()

                    val pdf = PdfEntity(
                        uriString = contentUri,
                        fileName = name,
                        fileSize = size,
                        lastAccessTime = date,
                        isFavorite = false,
                        lastPageRead = 0,
                        totalPages = 0,
                        filePath = path
                    )
                    pdfList.add(pdf)
                }
            }
        } catch (e: Exception) {
            Log.e("PdfRepository", "Error scanning MediaStore PDFs", e)
        }

        // Merge newly scanned items with database
        pdfList.forEach { scannedPdf ->
            val existing = pdfDao.getPdfByUri(scannedPdf.uriString)
            if (existing == null) {
                pdfDao.insertOrUpdatePdf(scannedPdf)
            } else {
                pdfDao.insertOrUpdatePdf(existing.copy(
                    fileName = scannedPdf.fileName,
                    fileSize = scannedPdf.fileSize,
                    filePath = scannedPdf.filePath
                ))
            }
        }
        return@withContext pdfList
    }
}
