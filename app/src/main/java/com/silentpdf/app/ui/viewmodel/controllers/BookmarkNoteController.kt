package com.silentpdf.app.ui.viewmodel.controllers

import android.util.Log
import com.silentpdf.app.data.db.BookmarkEntity
import com.silentpdf.app.data.db.NoteEntity
import com.silentpdf.app.data.db.PdfEntity
import com.silentpdf.app.data.repository.PdfRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class BookmarkNoteController(
    private val repository: PdfRepository,
    private val coroutineScope: CoroutineScope,
    private val currentPdf: StateFlow<PdfEntity?>,
    private val getCurrentPage: () -> Int
) {
    val currentBookmarks: StateFlow<List<BookmarkEntity>> = currentPdf
        .flatMapLatest { pdf ->
            pdf?.let { repository.getBookmarksForPdf(it.uriString) } ?: flowOf(emptyList())
        }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentNotes: StateFlow<List<NoteEntity>> = currentPdf
        .flatMapLatest { pdf ->
            pdf?.let { repository.getNotesForPdf(it.uriString) } ?: flowOf(emptyList())
        }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleBookmarkCurrentPage() {
        val pdf = currentPdf.value ?: return
        val page = getCurrentPage()
        coroutineScope.launch {
            val bookmarks = currentBookmarks.value
            val existing = bookmarks.firstOrNull { it.pageNumber == page }
            if (existing != null) {
                repository.removeBookmark(existing.id)
            } else {
                repository.addBookmark(pdf.uriString, page, "Page ${page + 1}")
            }
        }
    }

    fun addOrUpdateNote(page: Int, text: String) {
        val pdf = currentPdf.value ?: return
        coroutineScope.launch {
            val existingNote = currentNotes.value.find { it.pageNumber == page }
            if (existingNote != null && existingNote.noteText.startsWith("[audio:")) {
                val oldPath = existingNote.noteText.substringAfter("[audio:").substringBefore("]")
                val newPath = if (text.startsWith("[audio:")) text.substringAfter("[audio:").substringBefore("]") else null
                if (oldPath != newPath) {
                    try {
                        val file = File(oldPath)
                        if (file.exists()) file.delete()
                    } catch (e: Exception) {
                        Log.e("BookmarkNoteController", "Error deleting orphaned voice note file", e)
                    }
                }
            }
            repository.addOrUpdateNote(pdf.uriString, page, text)
        }
    }

    fun removeNote(noteId: Long) {
        coroutineScope.launch {
            val note = currentNotes.value.find { it.id == noteId }
            if (note != null && note.noteText.startsWith("[audio:")) {
                try {
                    val filePath = note.noteText.substringAfter("[audio:").substringBefore("]")
                    val file = File(filePath)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    Log.e("BookmarkNoteController", "Error deleting voice note file", e)
                }
            }
            repository.removeNote(noteId)
        }
    }

    fun removeNoteForPage(page: Int) {
        val pdf = currentPdf.value ?: return
        coroutineScope.launch {
            val note = currentNotes.value.find { it.pageNumber == page }
            if (note != null && note.noteText.startsWith("[audio:")) {
                try {
                    val filePath = note.noteText.substringAfter("[audio:").substringBefore("]")
                    val file = File(filePath)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    Log.e("BookmarkNoteController", "Error deleting voice note file for page", e)
                }
            }
            repository.removeNoteForPage(pdf.uriString, page)
        }
    }
}
