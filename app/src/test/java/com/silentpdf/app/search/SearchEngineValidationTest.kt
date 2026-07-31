package com.silentpdf.app.search

import android.graphics.RectF
import com.silentpdf.app.search.domain.SearchResult
import com.silentpdf.app.search.engine.CoordinateMapper
import com.silentpdf.app.search.engine.SearchEngine
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SearchEngineValidationTest {

    @Test
    fun `CoordinateMapper computes exact normalized translation`() {
        val pdfRect = RectF(0.5f, 0.5f, 0.6f, 0.6f)
        
        val mapped = CoordinateMapper.map(
            pdfRect = pdfRect,
            pageWidth = 1000f,
            pageHeight = 1000f,
            viewWidth = 500f,
            viewHeight = 500f,
            zoom = 2f,
            offsetX = 100f,
            offsetY = 100f
        )
        
        // At 1000 page size to 500 view size -> scale is 0.5
        // fitScale = 0.5
        // fittedWidth = 500, fittedHeight = 500
        // No dx/dy because aspect ratio matches exactly
        // pdfRect left 0.5 * 500 = 250
        // zoom 2 -> 250 * 2 = 500
        // offsetX 100 -> 500 + 100 = 600
        assertEquals(600f, mapped.left)
    }

    @Test
    fun `SearchEngine detects single word matches`() {
        val words = listOf(
            Pair("Hello", RectF(0.1f, 0.1f, 0.2f, 0.2f)),
            Pair("World", RectF(0.3f, 0.1f, 0.4f, 0.2f))
        )
        
        val results = SearchEngine.searchPage(0, words, "world")
        
        assertEquals(1, results.size)
        assertEquals(1, results[0].rects.size)
        assertEquals(0.3f, results[0].rects[0].left)
    }

    @Test
    fun `SearchEngine detects multi word matches with partial overlaps`() {
        val words = listOf(
            Pair("The", RectF(0.1f, 0.1f, 0.2f, 0.2f)),
            Pair("Quick", RectF(0.3f, 0.1f, 0.4f, 0.2f)),
            Pair("Brown", RectF(0.5f, 0.1f, 0.6f, 0.2f))
        )
        
        val results = SearchEngine.searchPage(0, words, "quick brown")
        
        assertEquals(1, results.size)
        assertEquals(2, results[0].rects.size)
        assertEquals(0.3f, results[0].rects[0].left)
        assertEquals(0.5f, results[0].rects[1].left)
    }
}
