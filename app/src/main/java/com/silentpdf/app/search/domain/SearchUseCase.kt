package com.silentpdf.app.search.domain

import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SearchUseCase(private val repository: SearchRepository) {

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()

    private val _activeMatchIndex = MutableStateFlow(0)
    val activeMatchIndex: StateFlow<Int> = _activeMatchIndex.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()
    
    private val _isOcrRequired = MutableStateFlow(false)
    val isOcrRequired: StateFlow<Boolean> = _isOcrRequired.asStateFlow()
    
    private val _searchProgress = MutableStateFlow(0f)
    val searchProgress: StateFlow<Float> = _searchProgress.asStateFlow()

    suspend fun performSearch(
        uriString: String,
        query: String,
        totalPages: Int,
        useOcr: Boolean = false,
        pageIndex: Int? = null,
        bitmapProvider: suspend (Int) -> Bitmap?
    ) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _activeMatchIndex.value = 0
            _isOcrRequired.value = false
            _searchProgress.value = 0f
            return
        }

        _isSearching.value = true
        _isOcrRequired.value = false
        _searchProgress.value = 0f
        try {
            val results = repository.searchPdf(uriString, query, totalPages, useOcr, pageIndex, bitmapProvider) { progress ->
                _searchProgress.value = progress
            }
            
            if (pageIndex != null) {
                // Merge new results with existing results for this specific page
                val existing = _searchResults.value.filter { it.page != pageIndex }
                _searchResults.value = (existing + results).sortedBy { it.page }
            } else {
                _searchResults.value = results
            }
            
            _activeMatchIndex.value = 0
        } catch (e: OcrRequiredException) {
            _searchResults.value = emptyList()
            _activeMatchIndex.value = 0
            _isOcrRequired.value = true
        } finally {
            _isSearching.value = false
        }
    }

    fun setOcrRequired(required: Boolean) {
        _isOcrRequired.value = required
    }

    fun nextMatch(): Int? {
        val results = _searchResults.value
        if (results.isEmpty()) return null
        val nextIdx = (_activeMatchIndex.value + 1) % results.size
        _activeMatchIndex.value = nextIdx
        return results[nextIdx].page
    }

    fun previousMatch(): Int? {
        val results = _searchResults.value
        if (results.isEmpty()) return null
        val prevIdx = if (_activeMatchIndex.value - 1 < 0) results.size - 1 else _activeMatchIndex.value - 1
        _activeMatchIndex.value = prevIdx
        return results[prevIdx].page
    }

    fun clearSearch() {
        _searchResults.value = emptyList()
        _activeMatchIndex.value = 0
    }
}
