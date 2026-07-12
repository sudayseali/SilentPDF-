package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.BookmarkEntity
import com.example.data.db.PdfEntity
import com.example.data.db.SilentPdfDatabase
import com.example.data.repository.PdfRenderEngine
import com.example.data.repository.PdfRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SilentPdfViewModel(application: Application) : AndroidViewModel(application) {

    private val database = SilentPdfDatabase.getDatabase(application)
    private val repository = PdfRepository(application, database.pdfDao())
    private val renderEngine = PdfRenderEngine(application)

    // State: User Search Query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // State: Selected Tab (0 = All, 1 = Recents, 2 = Bookmarks/Favorites)
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab

    // State: Sorting (0 = Name, 1 = Date, 2 = Size)
    private val _sortBy = MutableStateFlow(0)
    val sortBy: StateFlow<Int> = _sortBy

    // View Settings (True Dark Mode, Grid/List view, Horizontal vs Vertical scrolling)
    private val _isTrueDarkMode = MutableStateFlow(false)
    val isTrueDarkMode: StateFlow<Boolean> = _isTrueDarkMode

    private val _isGridView = MutableStateFlow(false)
    val isGridView: StateFlow<Boolean> = _isGridView

    private val _isHorizontalScroll = MutableStateFlow(false)
    val isHorizontalScroll: StateFlow<Boolean> = _isHorizontalScroll

    // Combines DB flows, search, sort, and tab filters into a single StateFlow of UI list
    val libraryPdfs: StateFlow<List<PdfEntity>> = combine(
        _selectedTab,
        _searchQuery,
        _sortBy,
        repository.allPdfsByName
    ) { tab, query, sort, allPdfs ->
        val baseList = when (tab) {
            1 -> allPdfs.filter { it.lastPageRead > 0 || it.lastAccessTime > 0 }
            2 -> allPdfs.filter { it.isFavorite }
            else -> allPdfs
        }

        val filteredList = if (query.isBlank()) {
            baseList
        } else {
            baseList.filter { it.fileName.contains(query, ignoreCase = true) }
        }

        when (sort) {
            0 -> filteredList.sortedBy { it.fileName.lowercase() }
            1 -> filteredList.sortedByDescending { it.lastAccessTime }
            2 -> filteredList.sortedByDescending { it.fileSize }
            else -> filteredList
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Reader UI States
    private val _currentPdf = MutableStateFlow<PdfEntity?>(null)
    val currentPdf: StateFlow<PdfEntity?> = _currentPdf

    private val _pageCount = MutableStateFlow(0)
    val pageCount: StateFlow<Int> = _pageCount

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage

    private val _currentPageBitmap = MutableStateFlow<Bitmap?>(null)
    val currentPageBitmap: StateFlow<Bitmap?> = _currentPageBitmap

    val currentBookmarks: StateFlow<List<BookmarkEntity>> = _currentPdf
        .flatMapLatest { pdf ->
            pdf?.let { repository.getBookmarksForPdf(it.uriString) } ?: flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _isPdfLoading = MutableStateFlow(false)
    val isPdfLoading: StateFlow<Boolean> = _isPdfLoading

    init {
        // Scan files on startup to populate library items
        triggerScan()
    }

    fun triggerScan() {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                repository.scanLocalPdfs()
            } catch (e: Exception) {
                Log.e("SilentPdfViewModel", "Failed to scan local pdf documents", e)
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setSelectedTab(tab: Int) { _selectedTab.value = tab }
    fun setSortBy(sort: Int) { _sortBy.value = sort }
    fun toggleTrueDarkMode() { _isTrueDarkMode.value = !_isTrueDarkMode.value }
    fun toggleGridView() { _isGridView.value = !_isGridView.value }
    fun toggleHorizontalScroll() { _isHorizontalScroll.value = !_isHorizontalScroll.value }

    fun toggleFavorite(pdf: PdfEntity) {
        viewModelScope.launch {
            repository.updateFavorite(pdf.uriString, !pdf.isFavorite)
        }
    }

    fun importPdf(uri: Uri, displayName: String, size: Long) {
        viewModelScope.launch {
            val newPdf = PdfEntity(
                uriString = uri.toString(),
                fileName = displayName,
                fileSize = size,
                lastAccessTime = System.currentTimeMillis(),
                isFavorite = false,
                lastPageRead = 0,
                totalPages = 0
            )
            repository.insertOrUpdatePdf(newPdf)
            openPdf(newPdf)
        }
    }

    fun openPdf(pdf: PdfEntity) {
        viewModelScope.launch {
            _isPdfLoading.value = true
            _currentPdf.value = pdf
            try {
                val pages = renderEngine.openDocument(Uri.parse(pdf.uriString))
                _pageCount.value = pages
                
                val lastPage = pdf.lastPageRead.coerceIn(0, (pages - 1).coerceAtLeast(0))
                _currentPage.value = lastPage
                
                renderCurrentPage(720)
                
                repository.insertOrUpdatePdf(pdf.copy(
                    lastAccessTime = System.currentTimeMillis(),
                    totalPages = pages
                ))
            } catch (e: Exception) {
                Log.e("SilentPdfViewModel", "Failed to load PDF doc in engine", e)
            } finally {
                _isPdfLoading.value = false
            }
        }
    }

    fun closePdf() {
        renderEngine.closeDocument()
        _currentPdf.value = null
        _currentPageBitmap.value = null
        _pageCount.value = 0
        _currentPage.value = 0
    }

    fun jumpToPage(pageIndex: Int, targetWidth: Int = 1080) {
        val total = _pageCount.value
        if (pageIndex in 0 until total) {
            _currentPage.value = pageIndex
            renderCurrentPage(targetWidth)
            _currentPdf.value?.let { pdf ->
                viewModelScope.launch {
                    repository.updateProgress(pdf.uriString, pageIndex, total)
                }
            }
        }
    }

    fun renderCurrentPage(targetWidth: Int) {
        viewModelScope.launch {
            val page = _currentPage.value
            val bitmap = renderEngine.renderPage(page, targetWidth)
            _currentPageBitmap.value = bitmap
        }
    }

    fun toggleBookmarkCurrentPage() {
        val pdf = _currentPdf.value ?: return
        val page = _currentPage.value
        viewModelScope.launch {
            val bookmarks = currentBookmarks.value
            val existing = bookmarks.firstOrNull { it.pageNumber == page }
            if (existing != null) {
                repository.removeBookmark(existing.id)
            } else {
                repository.addBookmark(pdf.uriString, page, "Page ${page + 1}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        renderEngine.closeDocument()
    }
}
