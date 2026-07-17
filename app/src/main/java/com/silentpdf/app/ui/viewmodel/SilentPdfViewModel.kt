package com.silentpdf.app.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.silentpdf.app.data.db.BookmarkEntity
import com.silentpdf.app.data.db.PdfEntity
import com.silentpdf.app.data.db.SilentPdfDatabase
import com.silentpdf.app.data.repository.PdfRenderEngine
import com.silentpdf.app.data.repository.PdfRepository
import com.silentpdf.app.data.repository.PdfTextSearcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

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

    private val customCategoriesPref = application.getSharedPreferences("app_custom_categories", android.content.Context.MODE_PRIVATE)
    private val viewSettingsPrefs = application.getSharedPreferences("app_view_settings", android.content.Context.MODE_PRIVATE)

    private val _customCategories = MutableStateFlow<Set<String>>(
        customCategoriesPref.getStringSet("categories", emptySet()) ?: emptySet()
    )

    // Get list of unique categories/folders created by user
    val allCategories: StateFlow<List<String>> = combine(
        repository.allPdfsByName,
        _customCategories
    ) { pdfs, custom ->
        val fromPdfs = pdfs.mapNotNull { it.category }
        (fromPdfs + custom).distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createCategory(category: String) {
        val trimmed = category.trim()
        if (trimmed.isNotBlank()) {
            val updated = _customCategories.value + trimmed
            _customCategories.value = updated
            customCategoriesPref.edit().putStringSet("categories", updated).apply()
            setSelectedCategory(trimmed)
        }
    }

    // View Settings (True Dark Mode, Grid/List view, Horizontal vs Vertical scrolling)
    private val _isTrueDarkMode = MutableStateFlow(viewSettingsPrefs.getBoolean("true_dark_mode", false))
    val isTrueDarkMode: StateFlow<Boolean> = _isTrueDarkMode

    private val _isGridView = MutableStateFlow(viewSettingsPrefs.getBoolean("grid_view", false))
    val isGridView: StateFlow<Boolean> = _isGridView

    private val _isHorizontalScroll = MutableStateFlow(viewSettingsPrefs.getBoolean("horizontal_scroll", false))
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
            pageStrokes.removeAt(pageStrokes.lastIndex)
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

    val currentNotes: StateFlow<List<com.silentpdf.app.data.db.NoteEntity>> = _currentPdf
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

    private var mediaRecorder: android.media.MediaRecorder? = null
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _recordingSeconds = MutableStateFlow(0)
    val recordingSeconds: StateFlow<Int> = _recordingSeconds

    private var recordingFile: java.io.File? = null
    private var recordingJob: kotlinx.coroutines.Job? = null

    init {
        // Check if an App PIN is set
        val savedPin = securityPrefs.getString("app_pin", null)
        if (!savedPin.isNullOrBlank()) {
            _isPinConfigured.value = true
            _isAppLocked.value = true
        } else {
            // Scan files on startup to populate library items if not locked
            triggerScan()
        }
    }

    fun verifyPin(pin: String): Boolean {
        val savedPin = securityPrefs.getString("app_pin", "")
        return savedPin == pin
    }

    fun unlockApp(pin: String): Boolean {
        if (verifyPin(pin)) {
            _isAppLocked.value = false
            triggerScan()
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
            val note = currentNotes.value.find { it.id == noteId }
            if (note != null && note.noteText.startsWith("[audio:")) {
                try {
                    val filePath = note.noteText.removePrefix("[audio:").removeSuffix("]")
                    val file = java.io.File(filePath)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    Log.e("SilentPdfViewModel", "Error deleting voice note file", e)
                }
            }
            repository.removeNote(noteId)
        }
    }

    fun removeNoteForPage(page: Int) {
        val pdf = _currentPdf.value ?: return
        viewModelScope.launch {
            val note = currentNotes.value.find { it.pageNumber == page }
            if (note != null && note.noteText.startsWith("[audio:")) {
                try {
                    val filePath = note.noteText.removePrefix("[audio:").removeSuffix("]")
                    val file = java.io.File(filePath)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    Log.e("SilentPdfViewModel", "Error deleting voice note file for page", e)
                }
            }
            repository.removeNoteForPage(pdf.uriString, page)
        }
    }

    fun startVoiceRecording(context: android.content.Context) {
        val pdf = _currentPdf.value ?: return
        try {
            val dir = java.io.File(context.filesDir, "voice_notes").apply { mkdirs() }
            val file = java.io.File(dir, "voice_${System.currentTimeMillis()}.mp4")
            recordingFile = file
            
            val attributionContext = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                context.createAttributionContext("voice_notes")
            } else {
                context
            }

            val recorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                android.media.MediaRecorder(attributionContext)
            } else {
                @Suppress("DEPRECATION")
                android.media.MediaRecorder()
            }
            
            recorder.apply {
                setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
                setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            
            mediaRecorder = recorder
            _isRecording.value = true
            _recordingSeconds.value = 0
            
            recordingJob = viewModelScope.launch {
                while (_isRecording.value) {
                    kotlinx.coroutines.delay(1000)
                    _recordingSeconds.value += 1
                }
            }
        } catch (e: Exception) {
            Log.e("SilentPdfViewModel", "Failed to start audio recording", e)
        }
    }

    fun stopVoiceRecording() {
        if (!_isRecording.value) return
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("SilentPdfViewModel", "Error stopping MediaRecorder", e)
        } finally {
            mediaRecorder = null
            _isRecording.value = false
            recordingJob?.cancel()
            recordingJob = null
        }
        
        val file = recordingFile
        val pdf = _currentPdf.value
        if (file != null && file.exists() && pdf != null) {
            val page = _currentPage.value
            addOrUpdateNote(page, "[audio:${file.absolutePath}]")
        }
        recordingFile = null
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
    fun toggleTrueDarkMode() {
        val newValue = !_isTrueDarkMode.value
        _isTrueDarkMode.value = newValue
        viewSettingsPrefs.edit().putBoolean("true_dark_mode", newValue).apply()
    }
    fun toggleGridView() {
        val newValue = !_isGridView.value
        _isGridView.value = newValue
        viewSettingsPrefs.edit().putBoolean("grid_view", newValue).apply()
    }
    fun toggleHorizontalScroll() {
        val newValue = !_isHorizontalScroll.value
        _isHorizontalScroll.value = newValue
        viewSettingsPrefs.edit().putBoolean("horizontal_scroll", newValue).apply()
    }

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
                
                // Cache plain text pages for AI capabilities
                cachePdfText(Uri.parse(pdf.uriString))

            } catch (e: SecurityException) {
                Log.e("SilentPdfViewModel", "Encrypted PDF file requiring password", e)
                _isPasswordProtected.value = true
                _pdfOpeningError.value = "Please enter Password to open this book"
                _pageCount.value = 0
                _currentPage.value = 0
                _currentPageBitmap.value = null
            } catch (e: Exception) {
                Log.e("SilentPdfViewModel", "Failed to load PDF doc in engine", e)
                _pdfOpeningError.value = "Could not open this book: ${e.localizedMessage}"
            } finally {
                _isPdfLoading.value = false
            }
        }
    }

    fun closePdf() {
        _currentPageBitmap.value = null
        viewModelScope.launch(Dispatchers.IO) {
            renderEngine.closeDocument()
        }
        _currentPdf.value = null
        _pageCount.value = 0
        _currentPage.value = 0
        _isPasswordProtected.value = false
        _pdfOpeningError.value = null
        _pdfSearchQuery.value = ""
        _pdfSearchResults.value = emptyList()
        _pdfOutline.value = emptyList()
        _openedPdfTextPages.value = emptyList()
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
                val cached = _openedPdfTextPages.value
                val results = if (cached.isNotEmpty()) {
                    textSearcher.searchCached(cached, query)
                } else {
                    textSearcher.search(Uri.parse(pdf.uriString), query)
                }
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

        private val _openedPdfTextPages = MutableStateFlow<List<String>>(emptyList())

    private fun cachePdfText(uri: Uri) {
        viewModelScope.launch {
            try {
                _openedPdfTextPages.value = textSearcher.getPagesText(uri)
            } catch (e: Exception) {
                Log.e("SilentPdfViewModel", "Failed to cache PDF text pages", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Run synchronously to ensure it closes before the process dies
        renderEngine.closeDocument()
        try {
            mediaRecorder?.release()
        } catch (e: Exception) {}
    }
}
