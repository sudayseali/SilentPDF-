package com.silentpdf.app.search.domain

import android.graphics.RectF

data class SearchResult(
    val page: Int,
    val rects: List<RectF>,
    val matchText: String
)
