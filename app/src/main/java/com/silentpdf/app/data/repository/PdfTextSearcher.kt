package com.silentpdf.app.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import android.graphics.RectF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import java.io.FileInputStream
import java.io.IOException
import kotlin.math.max
import kotlin.math.min
import java.text.Normalizer

class PdfTextSearcher(private val context: Context) {
    data class SearchResult(
        val pageNumber: Int,
        val snippet: String,
        val occurrencesCount: Int,
        val bounds: List<RectF> = emptyList()
    )

    data class OutlineItem(
        val title: String,
        val pageNumber: Int
    )

    private fun String.normalizeSearch(): String {
        return this.lowercase()
            .replace(Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670]"), "") // Remove Arabic diacritics
            .let { Normalizer.normalize(it, Normalizer.Form.NFC) }
    }

    private class BoundingBoxStripper : PDFTextStripper() {
        val wordBounds = mutableListOf<Pair<String, RectF>>()
        var pageWidth = 1f
        var pageHeight = 1f

        @Throws(IOException::class)
        override fun writeString(text: String?, textPositions: MutableList<TextPosition>?) {
            super.writeString(text, textPositions)
            if (text != null && textPositions != null && textPositions.isNotEmpty()) {
                var minX = Float.MAX_VALUE
                var minY = Float.MAX_VALUE
                var maxX = Float.MIN_VALUE
                var maxY = Float.MIN_VALUE

                for (pos in textPositions) {
                    minX = min(minX, pos.xDirAdj)
                    minY = min(minY, pos.yDirAdj - pos.heightDir)
                    maxX = max(maxX, pos.xDirAdj + pos.widthDirAdj)
                    maxY = max(maxY, pos.yDirAdj)
                }

                if (pageWidth > 0 && pageHeight > 0) {
                    val normRect = RectF(
                        max(0f, minX / pageWidth),
                        max(0f, minY / pageHeight),
                        min(1f, maxX / pageWidth),
                        min(1f, maxY / pageHeight)
                    )
                    wordBounds.add(Pair(text.trim(), normRect))
                }
            }
        }
    }

    suspend fun getPagesText(uri: Uri): List<String> = withContext(Dispatchers.IO) {
        val pagesText = mutableListOf<String>()
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                val document = PDDocument.load(FileInputStream(pfd.fileDescriptor), com.tom_roush.pdfbox.io.MemoryUsageSetting.setupTempFileOnly())
                val stripper = PDFTextStripper()
                // Limit to max 500 pages to avoid OOM for giant text books
                val maxPages = document.numberOfPages.coerceAtMost(500)
                for (i in 1..maxPages) {
                    stripper.startPage = i
                    stripper.endPage = i
                    val text = stripper.getText(document)
                    pagesText.add(text.trim())
                }
                document.close()
            }
        } catch (e: Exception) {
            Log.e("PdfTextSearcher", "Error extracting text", e)
        }
        return@withContext pagesText
    }

    suspend fun search(uri: Uri, query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<SearchResult>()
        if (query.isBlank()) return@withContext results
        val normalizedQuery = query.normalizeSearch()

        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                val document = PDDocument.load(FileInputStream(pfd.fileDescriptor), com.tom_roush.pdfbox.io.MemoryUsageSetting.setupTempFileOnly())
                val maxPages = document.numberOfPages.coerceAtMost(100) // limited for performance
                
                for (i in 1..maxPages) {
                    val page = document.getPage(i - 1)
                    val stripper = BoundingBoxStripper()
                    stripper.startPage = i
                    stripper.endPage = i
                    val mediaBox = page.mediaBox
                    stripper.pageWidth = mediaBox.width
                    stripper.pageHeight = mediaBox.height
                    
                    stripper.getText(document)
                    val words = stripper.wordBounds
                    
                    if (words.isNotEmpty()) {
                        results.addAll(findMatches(i - 1, words, normalizedQuery, query))
                    }
                }
                document.close()
            }
        } catch (e: Exception) {
            Log.e("PdfTextSearcher", "Error searching text", e)
            // Fallback
            val pagesText = getPagesText(uri)
            results.addAll(searchCached(pagesText, query))
        }
        return@withContext results
    }

    private fun findMatches(pageIndex: Int, words: List<Pair<String, RectF>>, query: String, originalQuery: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        val fullTextBuilder = java.lang.StringBuilder()
        val wordPositions = mutableListOf<Int>()
        
        for (w in words) {
            wordPositions.add(fullTextBuilder.length)
            fullTextBuilder.append(w.first).append(" ")
        }
        
        val fullText = fullTextBuilder.toString().normalizeSearch()
        var index = 0
        var count = 0
        val matchingBounds = mutableListOf<RectF>()
        
        while (index < fullText.length) {
            val found = fullText.indexOf(query, index)
            if (found != -1) {
                count++
                val startChar = found
                val endChar = found + query.length
                
                for (i in words.indices) {
                    val wordStart = wordPositions[i]
                    val wordEnd = wordStart + words[i].first.length
                    if (wordEnd >= startChar && wordStart <= endChar) {
                        matchingBounds.add(words[i].second)
                    }
                }
                index = found + query.length
            } else {
                break
            }
        }
        
        if (count > 0) {
            val idx = fullText.indexOf(query)
            val snipStart = max(0, idx - 40)
            val snipEnd = min(fullText.length, idx + query.length + 40)
            var snippet = fullText.substring(snipStart, snipEnd)
            if (snipStart > 0) snippet = "...$snippet"
            if (snipEnd < fullText.length) snippet = "$snippet..."
            
            results.add(SearchResult(pageIndex, snippet, count, matchingBounds))
        }
        return results
    }

    fun searchCached(pagesText: List<String>, query: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        if (query.isBlank()) return results
        val lowerQuery = query.lowercase()
        pagesText.forEachIndexed { index, text ->
            val count = countOccurrences(text.lowercase(), lowerQuery)
            if (count > 0) {
                results.add(
                    SearchResult(
                        pageNumber = index,
                        snippet = createSnippet(text, query),
                        occurrencesCount = count,
                        bounds = emptyList() // We don't have bounds in cache
                    )
                )
            }
        }
        return results
    }

    suspend fun extractOutline(uri: Uri): List<OutlineItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<OutlineItem>()
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                val document = PDDocument.load(FileInputStream(pfd.fileDescriptor), com.tom_roush.pdfbox.io.MemoryUsageSetting.setupTempFileOnly())
                val outline = document.documentCatalog.documentOutline
                if (outline != null) {
                    var current: PDOutlineItem? = outline.firstChild
                    while (current != null) {
                        val title = current.title
                        var pageNumber = 0
                        val page = current.findDestinationPage(document)
                        if (page != null) {
                            pageNumber = document.pages.indexOf(page)
                        }
                        if (title != null) {
                            items.add(OutlineItem(title = title.toString(), pageNumber = pageNumber.coerceAtLeast(0)))
                        }
                        current = current.nextSibling
                    }
                }
                document.close()
            }
        } catch (e: Exception) {
            Log.e("PdfTextSearcher", "Error extracting outline", e)
        }
        return@withContext items
    }

    private fun countOccurrences(text: String, query: String): Int {
        var count = 0
        var index = 0
        while (index < text.length) {
            val found = text.indexOf(query, index)
            if (found != -1) {
                count++
                index = found + query.length
            } else {
                break
            }
        }
        return count
    }

    private fun createSnippet(text: String, query: String): String {
        val lowerText = text.lowercase()
        val lowerQuery = query.lowercase()
        val idx = lowerText.indexOf(lowerQuery)
        if (idx == -1) return text.take(100) + "..."
        val start = (idx - 40).coerceAtLeast(0)
        val end = (idx + query.length + 40).coerceAtMost(text.length)
        var snippet = text.substring(start, end).replace('\n', ' ').trim()
        if (start > 0) snippet = "...$snippet"
        if (end < text.length) snippet = "$snippet..."
        return snippet
    }
}
