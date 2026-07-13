package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pdfs")
data class PdfEntity(
    @PrimaryKey val uriString: String,
    val fileName: String,
    val fileSize: Long,
    val lastAccessTime: Long,
    val isFavorite: Boolean = false,
    val lastPageRead: Int = 0,
    val totalPages: Int = 0,
    val filePath: String? = null,
    val category: String? = null
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pdfUriString: String,
    val pageNumber: Int,
    val label: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pdfUriString: String,
    val pageNumber: Int,
    val noteText: String,
    val timestamp: Long = System.currentTimeMillis()
)
