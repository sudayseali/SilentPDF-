package com.silentpdf.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.silentpdf.app.data.repository.PdfTextSearcher.Match
import com.silentpdf.app.data.repository.PdfTextSearcher.SearchResult

@Composable
fun TransparentHighlightOverlay(
    matches: List<Match> = emptyList(),
    searchResults: List<SearchResult> = emptyList(),
    activeMatchIndex: Int,
    pageIndex: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val corner = CornerRadius(3.dp.toPx(), 3.dp.toPx())

        if (matches.isNotEmpty()) {
            matches.forEachIndexed { index, match ->
                if (match.page == pageIndex) {
                    val isActive = (index == activeMatchIndex)
                    val fillColor = if (isActive) Color(0xEEFF9800) else Color(0x55FFEB3B)
                    val borderColor = if (isActive) Color(0xFFFF3D00) else Color.Transparent

                    val boxesToDraw = if (match.bounds.isNotEmpty()) match.bounds else listOf(match.rect)
                    boxesToDraw.forEach { rect ->
                        val highlightRect = Rect(
                            left = rect.left * size.width,
                            top = rect.top * size.height,
                            right = rect.right * size.width,
                            bottom = rect.bottom * size.height
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
            }
        } else if (searchResults.isNotEmpty()) {
            searchResults.forEachIndexed { index, result ->
                if (result.pageNumber == pageIndex) {
                    val isActive = index == activeMatchIndex
                    val fillColor = if (isActive) Color(0xEEFF9800) else Color(0x55FFEB3B)
                    val borderColor = if (isActive) Color(0xFFFF3D00) else Color.Transparent

                    result.bounds.forEach { rect ->
                        val highlightRect = Rect(
                            left = rect.left * size.width,
                            top = rect.top * size.height,
                            right = rect.right * size.width,
                            bottom = rect.bottom * size.height
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
            }
        }
    }
}
