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
import com.silentpdf.app.bionic.BionicConfig
import com.silentpdf.app.bionic.BionicIntensity
import com.silentpdf.app.bionic.BionicLanguage
import com.silentpdf.app.bionic.BionicPerformanceMode
import com.silentpdf.app.bionic.BionicReadingEngine
import com.silentpdf.app.bionic.ProcessedBionicPage
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
    val textSearcher = PdfTextSearcher(application)
    
    // NEW SEARCH ARCHITECTURE
    private val textExtractionEngine = com.silentpdf.app.search.engine.TextExtractionEngine(application)
    private val ocrEngine = com.silentpdf.app.search.engine.OCREngine(database.pdfDao())
    private val searchRepo = com.silentpdf.app.search.domain.SearchRepository(textExtractionEngine, ocrEngine)
    val searchUseCase = com.silentpdf.app.search.domain.SearchUseCase(searchRepo)

    private val libraryController = com.silentpdf.app.ui.viewmodel.controllers.LibraryController(application, repository, viewModelScope)

    val searchQuery: StateFlow<String> = libraryController.searchQuery
    val selectedTab: StateFlow<Int> = libraryController.selectedTab
    val sortBy: StateFlow<Int> = libraryController.sortBy
    val selectedCategory: StateFlow<String?> = libraryController.selectedCategory
    val allCategories: StateFlow<List<String>> = libraryController.allCategories
    val libraryPdfs: StateFlow<List<PdfEntity>> = libraryController.libraryPdfs

    fun createCategory(category: String) = libraryController.createCategory(category)
    fun setSearchQuery(query: String) = libraryController.setSearchQuery(query)
    fun setSelectedTab(tab: Int) = libraryController.setSelectedTab(tab)
    fun setSortBy(sort: Int) = libraryController.setSortBy(sort)
    fun setSelectedCategory(category: String?) = libraryController.setSelectedCategory(category)

    private val settingsController = com.silentpdf.app.ui.viewmodel.controllers.SettingsController(application)

    val isTrueDarkMode: StateFlow<Boolean> = settingsController.isTrueDarkMode
    val isAppDarkMode: StateFlow<Boolean> = settingsController.isAppDarkMode
    val isGridView: StateFlow<Boolean> = settingsController.isGridView
    val isHorizontalScroll: StateFlow<Boolean> = settingsController.isHorizontalScroll
    val bionicConfig: StateFlow<BionicConfig> = settingsController.bionicConfig

    fun updateBionicConfig(
        isEnabled: Boolean = bionicConfig.value.isEnabled,
        intensity: BionicIntensity = bionicConfig.value.intensity,
        customPercentage: Float = bionicConfig.value.customIntensityPercentage,
        language: BionicLanguage = bionicConfig.value.language,
        performanceMode: BionicPerformanceMode = bionicConfig.value.performanceMode,
        autoOcrForScanned: Boolean = bionicConfig.value.autoOcrForScanned
    ) {
        settingsController.updateBionicConfig(isEnabled, intensity, customPercentage, language, performanceMode, autoOcrForScanned)
    }

    fun toggleTrueDarkMode() { settingsController.toggleTrueDarkMode() }
    fun toggleAppDarkMode() { settingsController.toggleAppDarkMode() }
    fun toggleGridView() { settingsController.toggleGridView() }
    fun toggleHorizontalScroll() { settingsController.toggleHorizontalScroll() }

    suspend fun processBionicPage(
        pdfUri: String,
        pageIndex: Int,
        rawText: String,
        bitmap: Bitmap?,
        textColor: Color = Color.Unspecified
    ): ProcessedBionicPage {
        return BionicReadingEngine.processPage(
            pdfUri = pdfUri,
            pageIndex = pageIndex,
            rawText = rawText,
            bitmap = bitmap,
            config = bionicConfig.value,
            textColor = textColor
        )
    }

    // State: Password decryption for encrypted PDFs
    private val _isPasswordProtected = MutableStateFlow(false)
    val isPasswordProtected: StateFlow<Boolean> = _isPasswordProtected

    private val _pdfOpeningError = MutableStateFlow<String?>(null)
    val pdfOpeningError: StateFlow<String?> = _pdfOpeningError

    private var renderJob: kotlinx.coroutines.Job? = null

    private val searchController = com.silentpdf.app.ui.viewmodel.controllers.SearchController(
        searchUseCase = searchUseCase,
        renderEngine = renderEngine,
        coroutineScope = viewModelScope,
        getCurrentPdf = { _currentPdf.value },
        getPageCount = { _pageCount.value },
        getCurrentPage = { _currentPage.value },
        onPageJumpRequested = { page -> _currentPage.value = page }
    )

    val pdfSearchQuery: StateFlow<String> = searchController.pdfSearchQuery
    val pdfSearchResults = searchController.pdfSearchResults
    val activeSearchMatchIndex = searchController.activeSearchMatchIndex
    val isSearchingInPdf = searchController.isSearchingInPdf
    val isOcrRequired = searchController.isOcrRequired
    val searchProgress = searchController.searchProgress

    fun scanCurrentPageOcr() = searchController.scanCurrentPageOcr()
    fun scanEntireDocumentOcr() = searchController.scanEntireDocumentOcr()
    fun cancelOcrRequirement() = searchController.cancelOcrRequirement()
    fun setActiveSearchMatch(index: Int) = searchController.setActiveSearchMatch(index)

    // State: Table of Contents / Outline entries
    private val _pdfOutline = MutableStateFlow<List<PdfTextSearcher.OutlineItem>>(emptyList())
    val pdfOutline: StateFlow<List<PdfTextSearcher.OutlineItem>> = _pdfOutline

    private val _isOutlineLoading = MutableStateFlow(false)
    val isOutlineLoading: StateFlow<Boolean> = _isOutlineLoading

    // Reader UI States
    private val _currentPdf = MutableStateFlow<PdfEntity?>(null)
    val currentPdf: StateFlow<PdfEntity?> = _currentPdf

    private val _pageCount = MutableStateFlow(0)
    val pageCount: StateFlow<Int> = _pageCount

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage

    private val _currentPageBitmap = MutableStateFlow<Bitmap?>(null)
    val currentPageBitmap: StateFlow<Bitmap?> = _currentPageBitmap

    private val drawingController = com.silentpdf.app.ui.viewmodel.controllers.DrawingController()
    val pageDrawings: StateFlow<Map<String, Map<Int, List<DrawingStroke>>>> = drawingController.pageDrawings

    fun addStroke(pdfUri: String, page: Int, stroke: DrawingStroke) {
        drawingController.addStroke(pdfUri, page, stroke)
    }

    fun undoLastStroke(pdfUri: String, page: Int) {
        drawingController.undoLastStroke(pdfUri, page)
    }

    fun redoLastStroke(pdfUri: String, page: Int) {
        drawingController.redoLastStroke(pdfUri, page)
    }

    private val bookmarkNoteController = com.silentpdf.app.ui.viewmodel.controllers.BookmarkNoteController(
        repository = repository,
        coroutineScope = viewModelScope,
        currentPdf = _currentPdf,
        getCurrentPage = { _currentPage.value }
    )
    
    val currentBookmarks: StateFlow<List<BookmarkEntity>> = bookmarkNoteController.currentBookmarks
    val currentNotes: StateFlow<List<com.silentpdf.app.data.db.NoteEntity>> = bookmarkNoteController.currentNotes
    
    fun toggleBookmarkCurrentPage() = bookmarkNoteController.toggleBookmarkCurrentPage()
    fun addOrUpdateNote(page: Int, text: String) = bookmarkNoteController.addOrUpdateNote(page, text)
    fun removeNote(noteId: Long) = bookmarkNoteController.removeNote(noteId)
    fun removeNoteForPage(page: Int) = bookmarkNoteController.removeNoteForPage(page)

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _isPdfLoading = MutableStateFlow(false)
    val isPdfLoading: StateFlow<Boolean> = _isPdfLoading

    // App PIN Lock flows
    private val _isPinConfigured = MutableStateFlow(false)
    val isPinConfigured: StateFlow<Boolean> = _isPinConfigured

    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked

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

    private val voiceRecordingController = com.silentpdf.app.ui.viewmodel.controllers.VoiceRecordingController(application, viewModelScope)
    val isRecording: StateFlow<Boolean> = voiceRecordingController.isRecording
    val recordingSeconds: StateFlow<Int> = voiceRecordingController.recordingSeconds

    fun startVoiceRecording(context: android.content.Context) {
        voiceRecordingController.startVoiceRecording()
    }

    fun stopVoiceRecording() {
        voiceRecordingController.stopVoiceRecording { file ->
            val pdf = _currentPdf.value
            if (file != null && file.exists() && pdf != null) {
                val page = _currentPage.value
                val existingNote = currentNotes.value.find { it.pageNumber == page }
                val existingText = if (existingNote != null) {
                    if (existingNote.noteText.startsWith("[audio:")) {
                        existingNote.noteText.substringAfter("]").trim()
                    } else {
                        existingNote.noteText
                    }
                } else ""
                
                val newNoteText = if (existingText.isNotEmpty()) {
                    "[audio:${file.absolutePath}] $existingText"
                } else {
                    "[audio:${file.absolutePath}]"
                }
                
                addOrUpdateNote(page, newNoteText)
            }
        }
    }

    fun triggerScan() {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                repository.scanLocalPdfs()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("SilentPdfViewModel", "Failed to scan local pdf documents", e)
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun updatePdfCategory(pdf: PdfEntity, category: String?) {
        viewModelScope.launch {
            repository.updateCategory(pdf.uriString, category?.takeIf { it.isNotBlank() })
        }
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
            searchController.cancelSearch()
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
                
                // Cache plain text pages for AI capabilities and high-speed search
                cachePdfText(Uri.parse(pdf.uriString))
            } catch (e: SecurityException) {
                Log.e("SilentPdfViewModel", "Encrypted PDF file requiring password", e)
                _isPasswordProtected.value = true
                _pdfOpeningError.value = "Please enter Password to open this book"
                _pageCount.value = 0
                _currentPage.value = 0
                _currentPageBitmap.value = null
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("SilentPdfViewModel", "Failed to load PDF doc in engine", e)
                _pdfOpeningError.value = "Could not open this book: ${e.localizedMessage}"
            } finally {
                _isPdfLoading.value = false
            }
        }
    }

    fun closePdf() {
        searchController.cancelSearch()
        renderJob?.cancel()
        renderJob = null
        _currentPageBitmap.value = null
        renderEngine.closeDocument()
        com.silentpdf.app.util.ViewRecycler.clearMemory()
        _currentPdf.value = null
        _pageCount.value = 0
        _currentPage.value = 0
        _isPasswordProtected.value = false
        _pdfOpeningError.value = null
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

    fun updateCurrentPage(pageIndex: Int) {
        val total = _pageCount.value
        if (pageIndex in 0 until total) {
            _currentPage.value = pageIndex
            _currentPdf.value?.let { pdf ->
                viewModelScope.launch {
                    repository.updateProgress(pdf.uriString, pageIndex, total)
                }
            }
        }
    }

    fun renderCurrentPage(targetWidth: Int) {
        renderJob?.cancel()
        renderJob = viewModelScope.launch(Dispatchers.IO) {
            val page = _currentPage.value
            if (_pageCount.value > 0 && _currentPdf.value != null) {
                val bitmap = renderEngine.renderPage(page, targetWidth)
                _currentPageBitmap.value = bitmap
            }
        }
    }

    suspend fun getPageBitmap(pageIndex: Int, targetWidth: Int): Bitmap? {
        val total = _pageCount.value
        val pdf = _currentPdf.value
        return if (pdf != null && total > 0 && pageIndex in 0 until total) {
            renderEngine.renderPage(pageIndex, targetWidth)
        } else {
            null
        }
    }

    fun searchInPdf(query: String) = searchController.searchInPdf(query)
    fun cancelSearch() = searchController.cancelSearch()
    fun nextSearchMatch() = searchController.nextSearchMatch()
    fun previousSearchMatch() = searchController.previousSearchMatch()

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
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
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
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("SilentPdfViewModel", "Failed to delete PDF", e)
            }
        }
    }

        private val _openedPdfTextPages = MutableStateFlow<List<String>>(emptyList())
    val openedPdfTextPages: StateFlow<List<String>> = _openedPdfTextPages

    private fun cachePdfText(uri: Uri) {
        viewModelScope.launch {
            try {
                _openedPdfTextPages.value = textSearcher.getPagesText(
                    uri = uri,
                    bitmapProvider = null
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("SilentPdfViewModel", "Failed to cache PDF text pages", e)
            }
        }
    }

    private val pdfEditController = com.silentpdf.app.ui.viewmodel.controllers.PdfEditController(application, repository, renderEngine)

    suspend fun rotatePages(pdf: PdfEntity, pageIndices: List<Int>) = pdfEditController.rotatePages(pdf, pageIndices)
    suspend fun deletePages(pdf: PdfEntity, pageIndices: List<Int>) = pdfEditController.deletePages(pdf, pageIndices)
    suspend fun insertBlankPage(pdf: PdfEntity, afterPageIndex: Int) = pdfEditController.insertBlankPage(pdf, afterPageIndex)
    suspend fun extractPages(pdf: PdfEntity, pageIndices: List<Int>) = pdfEditController.extractPages(pdf, pageIndices)

    fun getPdfPageCount(): Int {
        return renderEngine.getPageCount()
    }

    override fun onCleared() {
        super.onCleared()
        // Run synchronously to ensure it closes before the process dies
        renderEngine.closeDocument()
        try {
            voiceRecordingController.stopVoiceRecording {}
        } catch (e: Exception) {}
    }
}
