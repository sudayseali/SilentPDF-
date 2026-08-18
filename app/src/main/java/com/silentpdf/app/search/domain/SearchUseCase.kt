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

    suspend fun performSearch(
        uriString: String,
        query: String,
        totalPages: Int,
        useOcr: Boolean = false,
        bitmapProvider: suspend (Int) -> Bitmap?
    ) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _activeMatchIndex.value = 0
            return
        }

        _isSearching.value = true
        try {
            val results = repository.searchPdf(uriString, query, totalPages, useOcr, bitmapProvider)
            _searchResults.value = results
            _activeMatchIndex.value = 0
        } finally {
            _isSearching.value = false
        }
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
