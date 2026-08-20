package com.silentpdf.app.ui.viewmodel.controllers

import android.graphics.Bitmap
import android.util.Log
import com.silentpdf.app.data.db.PdfEntity
import com.silentpdf.app.data.repository.PdfRenderEngine
import com.silentpdf.app.search.domain.SearchUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class SearchController(
    private val searchUseCase: SearchUseCase,
    private val renderEngine: PdfRenderEngine,
    private val coroutineScope: CoroutineScope,
    private val getCurrentPdf: () -> PdfEntity?,
    private val getPageCount: () -> Int,
    private val getCurrentPage: () -> Int,
    private val onPageJumpRequested: (Int) -> Unit
) {
    private val _pdfSearchQuery = MutableStateFlow("")
    val pdfSearchQuery: StateFlow<String> = _pdfSearchQuery

    private var searchJob: Job? = null

    val pdfSearchResults = searchUseCase.searchResults
    val activeSearchMatchIndex = searchUseCase.activeMatchIndex
    val isSearchingInPdf = searchUseCase.isSearching
    val isOcrRequired = searchUseCase.isOcrRequired
    val searchProgress = searchUseCase.searchProgress

    fun searchInPdf(query: String) {
        _pdfSearchQuery.value = query
        val pdf = getCurrentPdf() ?: return
        
        searchJob?.cancel()
        searchJob = coroutineScope.launch {
            delay(300) // Debounce
            
            if (query.isBlank()) {
                searchUseCase.clearSearch()
                return@launch
            }
            try {
                searchUseCase.setOcrRequired(false)
                searchUseCase.performSearch(
                    uriString = pdf.uriString,
                    query = query,
                    totalPages = getPageCount(),
                    useOcr = false,
                    bitmapProvider = { pageIdx -> renderEngine.renderPage(pageIdx, 800) }
                )
                val firstMatchPage = searchUseCase.searchResults.value.firstOrNull()?.page
                if (firstMatchPage != null) {
                    onPageJumpRequested(firstMatchPage)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("SearchController", "Search failed", e)
            }
        }
    }

    fun scanCurrentPageOcr() {
        val query = _pdfSearchQuery.value
        if (query.isBlank()) return
        val pdf = getCurrentPdf() ?: return
        
        searchJob?.cancel()
        searchJob = coroutineScope.launch {
            try {
                searchUseCase.setOcrRequired(false)
                searchUseCase.performSearch(
                    uriString = pdf.uriString,
                    query = query,
                    totalPages = getPageCount(),
                    useOcr = true,
                    pageIndex = getCurrentPage(),
                    bitmapProvider = { pageIdx -> renderEngine.renderPage(pageIdx, 800) }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("SearchController", "Failed to OCR current page", e)
            }
        }
    }

    fun scanEntireDocumentOcr() {
        val query = _pdfSearchQuery.value
        if (query.isBlank()) return
        val pdf = getCurrentPdf() ?: return
        
        searchJob?.cancel()
        searchJob = coroutineScope.launch {
            try {
                searchUseCase.setOcrRequired(false)
                searchUseCase.performSearch(
                    uriString = pdf.uriString,
                    query = query,
                    totalPages = getPageCount(),
                    useOcr = true,
                    pageIndex = null,
                    bitmapProvider = { pageIdx -> renderEngine.renderPage(pageIdx, 800) }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("SearchController", "Failed to OCR entire document", e)
            }
        }
    }
    
    fun cancelOcrRequirement() {
        searchUseCase.setOcrRequired(false)
        _pdfSearchQuery.value = ""
    }

    fun setActiveSearchMatch(index: Int) {
        // Implementation updated later if needed
    }

    fun cancelSearch() {
        searchJob?.cancel()
        searchUseCase.clearSearch()
        _pdfSearchQuery.value = ""
    }

    fun nextSearchMatch() {
        val page = searchUseCase.nextMatch()
        if (page != null) onPageJumpRequested(page)
    }

    fun previousSearchMatch() {
        val page = searchUseCase.previousMatch()
        if (page != null) onPageJumpRequested(page)
    }
}
