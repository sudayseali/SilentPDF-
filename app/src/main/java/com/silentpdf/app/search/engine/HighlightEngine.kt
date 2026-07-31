package com.silentpdf.app.search.engine

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.silentpdf.app.search.domain.SearchResult

@Composable
fun HighlightEngine(
    searchResults: List<SearchResult>,
    activeMatchIndex: Int,
    pageIndex: Int,
    modifier: Modifier = Modifier,
    zoom: Float = 1f,
    offsetX: Float = 0f,
    offsetY: Float = 0f
) {
    Canvas(modifier = modifier) {
        val corner = CornerRadius(3.dp.toPx(), 3.dp.toPx())
        val actualWidth = size.width
        val actualHeight = size.height

        // Calculate global indices for all results across all pages so activeMatchIndex works correctly
        var globalIndex = 0
        searchResults.forEach { result ->
            val isActive = globalIndex == activeMatchIndex
            if (result.page == pageIndex) {
                // "Allowed: alpha 0.2–0.4 fill OR stroke highlight OR underline"
                val fillColor = if (isActive) Color(0xFFFF9800).copy(alpha = 0.4f) else Color(0xFFFFEB3B).copy(alpha = 0.2f)
                val borderColor = if (isActive) Color(0xFFFF3D00).copy(alpha = 0.8f) else Color.Transparent

                result.rects.forEach { rect ->
                    // Use CoordinateMapper mapping
                    val mapped = CoordinateMapper.map(
                        pdfRect = rect,
                        pageWidth = 1f, 
                        pageHeight = 1f,
                        viewWidth = actualWidth,
                        viewHeight = actualHeight,
                        zoom = zoom,
                        offsetX = offsetX,
                        offsetY = offsetY
                    )
                    
                    val highlightRect = Rect(
                        left = mapped.left,
                        top = mapped.top,
                        right = mapped.right,
                        bottom = mapped.bottom
                    )
                    
                    drawRoundRect(
                        color = fillColor,
                        topLeft = highlightRect.topLeft,
                        size = highlightRect.size,
                        cornerRadius = corner,
                        blendMode = BlendMode.SrcOver
                    )
                    
                    if (isActive) {
                        drawRoundRect(
                            color = borderColor,
                            topLeft = highlightRect.topLeft,
                            size = highlightRect.size,
                            cornerRadius = corner,
                            style = Stroke(width = 2.dp.toPx()),
                            blendMode = BlendMode.SrcOver
                        )
                    }
                }
            }
            globalIndex++
        }
    }
}
