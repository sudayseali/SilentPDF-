package com.silentpdf.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import com.silentpdf.app.data.models.SearchResult

@Composable
fun TransparentHighlightOverlay(
    searchResults: List<SearchResult>,
    activeMatchIndex: Int,
    pageIndex: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        searchResults.forEachIndexed { index, result ->
            if (result.pageNumber == pageIndex) {
                val isActive = index == activeMatchIndex
                val highlightColor = if (isActive) Color(0xAAFF9800) else Color(0x88FFEB3B)
                result.bounds.forEach { rect ->
                    val highlightRect = Rect(
                        rect.left * size.width,
                        rect.top * size.height,
                        rect.right * size.width,
                        rect.bottom * size.height
                    )
                    drawRect(
                        color = highlightColor,
                        topLeft = highlightRect.topLeft,
                        size = highlightRect.size,
                        blendMode = BlendMode.Multiply
                    )
                }
            }
        }
    }
}
