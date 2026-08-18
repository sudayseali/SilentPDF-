package com.silentpdf.app.search.domain

import android.graphics.Bitmap
import android.net.Uri
import com.silentpdf.app.search.engine.OCREngine
import com.silentpdf.app.search.engine.SearchEngine
import com.silentpdf.app.search.engine.TextExtractionEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

class OcrRequiredException : Exception("No searchable text found. OCR is required.")

class SearchRepository(
    private val textExtractionEngine: TextExtractionEngine,
    private val ocrEngine: OCREngine
) {

    suspend fun searchPdf(
        uriString: String,
        query: String,
        totalPages: Int,
        useOcr: Boolean = false,
        pageIndex: Int? = null,
        bitmapProvider: suspend (Int) -> Bitmap?,
        onProgress: ((Float) -> Unit)? = null
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        val uri = Uri.parse(uriString)
        val allResults = mutableListOf<SearchResult>()
        var hasTextInDocument = false
        
        val pageRange = if (pageIndex != null) {
            pageIndex..pageIndex
        } else {
            0 until totalPages
        }
        
        var processedCount = 0
        val totalToProcess = if (pageIndex != null) 1 else totalPages

        for (i in pageRange) {
            ensureActive()
            
            // Report progress
            if (totalToProcess > 0) {
                onProgress?.invoke(processedCount.toFloat() / totalToProcess)
            }
            
            // Try extracting text using PDFBox
            val textRes = textExtractionEngine.extractPageText(uri, i)
            if (textRes.hasText) {
                hasTextInDocument = true
            }
            
            var words = textRes.words
            // If no text and OCR is explicitly requested, try OCR fallback
            if (words.isEmpty() && useOcr) {
                words = ocrEngine.extractFromBitmap(uriString, i) { bitmapProvider.invoke(i) }
            }
            
            if (words.isNotEmpty()) {
                val pageResults = SearchEngine.searchPage(i, words, query)
                allResults.addAll(pageResults)
            }
            
            processedCount++
        }
        
        onProgress?.invoke(1f)
        
        if (!hasTextInDocument && !useOcr && allResults.isEmpty()) {
            throw OcrRequiredException()
        }
        
        return@withContext allResults
    }
}
