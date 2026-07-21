package com.silentpdf.app.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.FileInputStream

class PdfTextSearcher(private val context: Context) {

    data class SearchResult(
        val pageNumber: Int,
        val snippet: String,
        val occurrencesCount: Int
    )

    data class OutlineItem(
        val title: String,
        val pageNumber: Int
    )

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
        try {
            val pagesText = getPagesText(uri)
            results.addAll(searchCached(pagesText, query))
        } catch (e: Exception) {
            Log.e("PdfTextSearcher", "Error searching text", e)
        }
        return@withContext results
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
                        occurrencesCount = count
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
                        // PDFBox pages are 0-indexed in PDPageTree, but here we just need to find the page
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
