package com.silentpdf.app.search.domain

import android.graphics.Bitmap
import android.net.Uri
import com.silentpdf.app.search.engine.OCREngine
import com.silentpdf.app.search.engine.SearchEngine
import com.silentpdf.app.search.engine.TextExtractionEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SearchRepository(
    private val textExtractionEngine: TextExtractionEngine,
    private val ocrEngine: OCREngine
) {
    suspend fun searchPdf(
        uriString: String,
        query: String,
        totalPages: Int,
        bitmapProvider: suspend (Int) -> Bitmap?
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        val uri = Uri.parse(uriString)
        val allResults = mutableListOf<SearchResult>()
        
        for (i in 0 until totalPages) {
            // Try extracting text using PDFBox
            val textRes = textExtractionEngine.extractPageText(uri, i)
            
            var words = textRes.words
            // If no text, try OCR fallback
            if (words.isEmpty()) {
                val bitmap = bitmapProvider(i)
                words = ocrEngine.extractFromBitmap(uriString, i, bitmap)
            }
            
            if (words.isNotEmpty()) {
                val pageResults = SearchEngine.searchPage(i, words, query)
                allResults.addAll(pageResults)
            }
        }
        
        return@withContext allResults
    }
}
