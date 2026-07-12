package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SilentPdfDao {
    @Query("SELECT * FROM pdfs ORDER BY fileName ASC")
    fun getAllPdfsByNameAsc(): Flow<List<PdfEntity>>

    @Query("SELECT * FROM pdfs ORDER BY lastAccessTime DESC")
    fun getAllPdfsByRecent(): Flow<List<PdfEntity>>

    @Query("SELECT * FROM pdfs WHERE isFavorite = 1 ORDER BY lastAccessTime DESC")
    fun getFavoritePdfs(): Flow<List<PdfEntity>>

    @Query("SELECT * FROM pdfs WHERE uriString = :uriString LIMIT 1")
    suspend fun getPdfByUri(uriString: String): PdfEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePdf(pdf: PdfEntity)

    @Query("UPDATE pdfs SET lastPageRead = :page, lastAccessTime = :time WHERE uriString = :uriString")
    suspend fun updateProgress(uriString: String, page: Int, time: Long)

    @Query("UPDATE pdfs SET isFavorite = :isFavorite WHERE uriString = :uriString")
    suspend fun updateFavorite(uriString: String, isFavorite: Boolean)

    @Query("DELETE FROM pdfs WHERE uriString = :uriString")
    suspend fun deletePdf(uriString: String)

    // Bookmarks queries
    @Query("SELECT * FROM bookmarks WHERE pdfUriString = :pdfUriString ORDER BY pageNumber ASC")
    fun getBookmarksForPdf(pdfUriString: String): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmark(id: Long)

    @Query("DELETE FROM bookmarks WHERE pdfUriString = :pdfUriString AND pageNumber = :pageNumber")
    suspend fun deleteBookmarkForPage(pdfUriString: String, pageNumber: Int)
}
