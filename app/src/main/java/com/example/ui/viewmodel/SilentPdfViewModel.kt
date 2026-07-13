package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.BookmarkEntity
import com.example.data.db.PdfEntity
import com.example.data.db.SilentPdfDatabase
import com.example.data.repository.PdfRenderEngine
import com.example.data.repository.PdfRepository
import com.example.data.repository.PdfTextSearcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DrawingStroke(
    val points: List<Offset>,
    val color: Color,
    val width: Float,
    val isEraser: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class SilentPdfViewModel(application: Application) : AndroidViewModel(application) {

    private val database = SilentPdfDatabase.getDatabase(application)
    private val repository = PdfRepository(application, database.pdfDao())
    private val securityPrefs = application.getSharedPreferences("app_security_prefs", android.content.Context.MODE_PRIVATE)
    private val renderEngine = PdfRenderEngine(application)
    private val textSearcher = PdfTextSearcher(application)

    // State: User Search Query for Library list
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // State: Selected Tab (0 = All, 1 = Recents, 2 = Bookmarks/Favorites)
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab

    // State: Sorting (0 = Name, 1 = Date, 2 = Size)
    private val _sortBy = MutableStateFlow(0)
    val sortBy: StateFlow<Int> = _sortBy

    // State: Selected Folder/Category (null means all folders/categories)
    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    // Get list of unique categories/folders created by user
    val allCategories: StateFlow<List<String>> = repository.allPdfsByName
        .map { pdfs -> pdfs.mapNotNull { it.category }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // View Settings (True Dark Mode, Grid/List view, Horizontal vs Vertical scrolling)
    private val _isTrueDarkMode = MutableStateFlow(false)
    val isTrueDarkMode: StateFlow<Boolean> = _isTrueDarkMode

    private val _isGridView = MutableStateFlow(false)
    val isGridView: StateFlow<Boolean> = _isGridView

    private val _isHorizontalScroll = MutableStateFlow(false)
    val isHorizontalScroll: StateFlow<Boolean> = _isHorizontalScroll

    // State: Password decryption for encrypted PDFs
    private val _isPasswordProtected = MutableStateFlow(false)
    val isPasswordProtected: StateFlow<Boolean> = _isPasswordProtected

    private val _pdfOpeningError = MutableStateFlow<String?>(null)
    val pdfOpeningError: StateFlow<String?> = _pdfOpeningError

    // State: Text Search within the open PDF
    private val _pdfSearchQuery = MutableStateFlow("")
    val pdfSearchQuery: StateFlow<String> = _pdfSearchQuery

    private val _pdfSearchResults = MutableStateFlow<List<PdfTextSearcher.SearchResult>>(emptyList())
    val pdfSearchResults: StateFlow<List<PdfTextSearcher.SearchResult>> = _pdfSearchResults

    private val _isSearchingInPdf = MutableStateFlow(false)
    val isSearchingInPdf: StateFlow<Boolean> = _isSearchingInPdf

    // State: Table of Contents / Outline entries
    private val _pdfOutline = MutableStateFlow<List<PdfTextSearcher.OutlineItem>>(emptyList())
    val pdfOutline: StateFlow<List<PdfTextSearcher.OutlineItem>> = _pdfOutline

    private val _isOutlineLoading = MutableStateFlow(false)
    val isOutlineLoading: StateFlow<Boolean> = _isOutlineLoading

    // Combines DB flows, search, sort, folder selection, and tab filters into a single StateFlow of UI list
    val libraryPdfs: StateFlow<List<PdfEntity>> = combine(
        _selectedTab,
        _searchQuery,
        _sortBy,
        _selectedCategory,
        repository.allPdfsByName
    ) { tab, query, sort, category, allPdfs ->
        val baseList = when (tab) {
            1 -> allPdfs.filter { it.lastPageRead > 0 || it.lastAccessTime > 0 }
            2 -> allPdfs.filter { it.isFavorite }
            else -> {
                if (category != null) {
                    allPdfs.filter { it.category == category }
                } else {
                    allPdfs
                }
            }
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

    private val _pageDrawings = MutableStateFlow<Map<String, Map<Int, List<DrawingStroke>>>>(emptyMap())
    val pageDrawings: StateFlow<Map<String, Map<Int, List<DrawingStroke>>>> = _pageDrawings

    fun addStroke(pdfUri: String, page: Int, stroke: DrawingStroke) {
        val currentDrawings = _pageDrawings.value.toMutableMap()
        val pdfDrawings = currentDrawings[pdfUri]?.toMutableMap() ?: mutableMapOf()
        val pageStrokes = pdfDrawings[page]?.toMutableList() ?: mutableListOf()
        pageStrokes.add(stroke)
        pdfDrawings[page] = pageStrokes
        currentDrawings[pdfUri] = pdfDrawings
        _pageDrawings.value = currentDrawings
    }

    fun undoLastStroke(pdfUri: String, page: Int) {
        val currentDrawings = _pageDrawings.value.toMutableMap()
        val pdfDrawings = currentDrawings[pdfUri]?.toMutableMap() ?: mutableMapOf()
        val pageStrokes = pdfDrawings[page]?.toMutableList() ?: mutableListOf()
        if (pageStrokes.isNotEmpty()) {
            pageStrokes.removeLast()
            pdfDrawings[page] = pageStrokes
            currentDrawings[pdfUri] = pdfDrawings
            _pageDrawings.value = currentDrawings
        }
    }

    val currentBookmarks: StateFlow<List<BookmarkEntity>> = _currentPdf
        .flatMapLatest { pdf ->
            pdf?.let { repository.getBookmarksForPdf(it.uriString) } ?: flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentNotes: StateFlow<List<com.example.data.db.NoteEntity>> = _currentPdf
        .flatMapLatest { pdf ->
            pdf?.let { repository.getNotesForPdf(it.uriString) } ?: flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _isPdfLoading = MutableStateFlow(false)
    val isPdfLoading: StateFlow<Boolean> = _isPdfLoading

    // App PIN Lock flows
    private val _isPinConfigured = MutableStateFlow(false)
    val isPinConfigured: StateFlow<Boolean> = _isPinConfigured

    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked

    init {
        // Check if an App PIN is set
        val savedPin = securityPrefs.getString("app_pin", null)
        if (!savedPin.isNullOrBlank()) {
            _isPinConfigured.value = true
            _isAppLocked.value = true
        }
        
        // Scan files on startup to populate library items
        triggerScan()
    }

    fun verifyPin(pin: String): Boolean {
        val savedPin = securityPrefs.getString("app_pin", "")
        return savedPin == pin
    }

    fun unlockApp(pin: String): Boolean {
        if (verifyPin(pin)) {
            _isAppLocked.value = false
            return true
        }
        return false
    }

    fun setAppPin(pin: String) {
        if (pin.length >= 4) {
            securityPrefs.edit().putString("app_pin", pin).apply()
            _isPinConfigured.value = true
            _isAppLocked.value = false
        }
    }

    fun disableAppPin() {
        securityPrefs.edit().remove("app_pin").apply()
        _isPinConfigured.value = false
        _isAppLocked.value = false
    }

    fun addOrUpdateNote(page: Int, text: String) {
        val pdf = _currentPdf.value ?: return
        viewModelScope.launch {
            repository.addOrUpdateNote(pdf.uriString, page, text)
        }
    }

    fun removeNote(noteId: Long) {
        viewModelScope.launch {
            repository.removeNote(noteId)
        }
    }

    fun removeNoteForPage(page: Int) {
        val pdf = _currentPdf.value ?: return
        viewModelScope.launch {
            repository.removeNoteForPage(pdf.uriString, page)
        }
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
    fun setSelectedCategory(category: String?) { _selectedCategory.value = category }
    fun updatePdfCategory(pdf: PdfEntity, category: String?) {
        viewModelScope.launch {
            repository.updateCategory(pdf.uriString, category?.takeIf { it.isNotBlank() })
        }
    }
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

    fun openPdf(pdf: PdfEntity, password: String? = null) {
        viewModelScope.launch {
            _isPdfLoading.value = true
            _currentPdf.value = pdf
            
            // Clear prior document-specific states
            _isPasswordProtected.value = false
            _pdfOpeningError.value = null
            _pdfSearchQuery.value = ""
            _pdfSearchResults.value = emptyList()
            _pdfOutline.value = emptyList()

            try {
                val pages = renderEngine.openDocument(Uri.parse(pdf.uriString), password)
                _pageCount.value = pages
                
                val lastPage = pdf.lastPageRead.coerceIn(0, (pages - 1).coerceAtLeast(0))
                _currentPage.value = lastPage
                
                renderCurrentPage(720)
                
                repository.insertOrUpdatePdf(pdf.copy(
                    lastAccessTime = System.currentTimeMillis(),
                    totalPages = pages
                ))
                
                // Dynamically fetch outline/table of contents on a background thread
                extractPdfOutline()

            } catch (e: SecurityException) {
                Log.e("SilentPdfViewModel", "Encrypted PDF file requiring password", e)
                _isPasswordProtected.value = true
                _pdfOpeningError.value = "Fadlan geli Password-ka si aad u furto buuggan"
                _pageCount.value = 0
                _currentPage.value = 0
                _currentPageBitmap.value = null
            } catch (e: Exception) {
                Log.e("SilentPdfViewModel", "Failed to load PDF doc in engine", e)
                _pdfOpeningError.value = "Ma aanan furi karin buuggan: ${e.localizedMessage}"
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
        _isPasswordProtected.value = false
        _pdfOpeningError.value = null
        _pdfSearchQuery.value = ""
        _pdfSearchResults.value = emptyList()
        _pdfOutline.value = emptyList()
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
            if (_pageCount.value > 0) {
                val bitmap = renderEngine.renderPage(page, targetWidth)
                _currentPageBitmap.value = bitmap
            }
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

    /**
     * Executes asynchronous text search across the open PDF.
     */
    fun searchInPdf(query: String) {
        _pdfSearchQuery.value = query
        val pdf = _currentPdf.value ?: return
        viewModelScope.launch {
            if (query.isBlank()) {
                _pdfSearchResults.value = emptyList()
                return@launch
            }
            _isSearchingInPdf.value = true
            try {
                val results = textSearcher.search(Uri.parse(pdf.uriString), query)
                _pdfSearchResults.value = results
            } catch (e: Exception) {
                Log.e("SilentPdfViewModel", "Failed text search inside active PDF", e)
            } finally {
                _isSearchingInPdf.value = false
            }
        }
    }

    /**
     * Extracts outline/chapters for table of contents.
     */
    fun extractPdfOutline() {
        val pdf = _currentPdf.value ?: return
        viewModelScope.launch {
            _isOutlineLoading.value = true
            try {
                val outline = textSearcher.extractOutline(Uri.parse(pdf.uriString))
                _pdfOutline.value = outline
            } catch (e: Exception) {
                Log.e("SilentPdfViewModel", "Failed outline extraction inside active PDF", e)
            } finally {
                _isOutlineLoading.value = false
            }
        }
    }

    fun deletePdf(pdf: PdfEntity) {
        viewModelScope.launch {
            try {
                repository.deletePdf(pdf.uriString)
                // If it's the currently open PDF, close it
                if (_currentPdf.value?.uriString == pdf.uriString) {
                    closePdf()
                }
            } catch (e: Exception) {
                Log.e("SilentPdfViewModel", "Failed to delete PDF", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        renderEngine.closeDocument()
    }
}
