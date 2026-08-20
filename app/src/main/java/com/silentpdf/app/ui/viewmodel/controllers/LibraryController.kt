package com.silentpdf.app.ui.viewmodel.controllers

import android.content.Context
import com.silentpdf.app.data.db.PdfEntity
import com.silentpdf.app.data.repository.PdfRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class LibraryController(
    context: Context,
    private val repository: PdfRepository,
    private val coroutineScope: CoroutineScope
) {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab

    private val _sortBy = MutableStateFlow(0)
    val sortBy: StateFlow<Int> = _sortBy

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    private val customCategoriesPref = context.getSharedPreferences("app_custom_categories", Context.MODE_PRIVATE)

    private val _customCategories = MutableStateFlow<Set<String>>(
        customCategoriesPref.getStringSet("categories", emptySet()) ?: emptySet()
    )

    val allCategories: StateFlow<List<String>> = combine(
        repository.allPdfsByName,
        _customCategories
    ) { pdfs, custom ->
        val fromPdfs = pdfs.mapNotNull { it.category }
        (fromPdfs + custom).distinct().sorted()
    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createCategory(category: String) {
        val trimmed = category.trim()
        if (trimmed.isNotBlank()) {
            val updated = _customCategories.value + trimmed
            _customCategories.value = updated
            customCategoriesPref.edit().putStringSet("categories", updated).apply()
            setSelectedCategory(trimmed)
        }
    }

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setSelectedTab(tab: Int) { _selectedTab.value = tab }
    fun setSortBy(sort: Int) { _sortBy.value = sort }
    fun setSelectedCategory(category: String?) { _selectedCategory.value = category }
}
