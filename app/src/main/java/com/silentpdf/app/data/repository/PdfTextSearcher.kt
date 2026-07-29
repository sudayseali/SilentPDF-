package com.silentpdf.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import android.util.Log
import com.silentpdf.app.bionic.OCRProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import java.io.FileInputStream
import java.io.IOException
import java.text.Normalizer
import kotlin.math.max
import kotlin.math.min

class PdfTextSearcher(private val context: Context) {

    enum class PdfType {
        TEXT_BASED,
        SCANNED,
        HYBRID
    }

    data class SearchResult(
        val pageNumber: Int,
        val snippet: String,
        val occurrencesCount: Int,
        val bounds: List<RectF> = emptyList()
    )

    data class Match(
        val page: Int,
        val rect: RectF,
        val bounds: List<RectF> = listOf(rect),
        val snippet: String = ""
    )

    data class OutlineItem(
        val title: String,
        val pageNumber: Int
    )

    /**
     * Advanced Text Normalization:
     * - Expands ligatures
     * - Removes line-end hyphenation
     * - Cleans diacritics and special spaces
     * - Performs Unicode NFC normalization
     */
    fun String.normalizeSearch(): String {
        var text = this
        // Expand common ligatures
        text = text.replace("ﬁ", "fi")
            .replace("ﬂ", "fl")
            .replace("æ", "ae")
            .replace("œ", "oe")
            .replace("ß", "ss")

        // Replace non-standard whitespace and soft hyphens
        text = text.replace("\u00AD", "") // soft hyphen
            .replace("\u200B", "") // zero-width space
            .replace("\u00A0", " ") // non-breaking space
            .replace(Regex("-\\s*\\n\\s*"), "") // join hyphenated words across line breaks

        // Strip diacritics & Arabic vowel marks
        text = text.replace(Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670]"), "")
        val nfdNormalized = Normalizer.normalize(text, Normalizer.Form.NFD)
        val withoutDiacritics = nfdNormalized.replace(Regex("\\p{Mn}+"), "")

        return Normalizer.normalize(withoutDiacritics, Normalizer.Form.NFC).lowercase()
    }

    private class BoundingBoxStripper : PDFTextStripper() {
        val wordBounds = mutableListOf<Pair<String, RectF>>()
        var pageWidth = 1f
        var pageHeight = 1f

        @Throws(IOException::class)
        override fun writeString(text: String?, textPositions: MutableList<TextPosition>?) {
            super.writeString(text, textPositions)
            if (!text.isNullOrBlank() && !textPositions.isNullOrEmpty()) {
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
                    wordBounds.add(Pair(text.orEmpty().trim(), normRect))
                }
            }
        }
    }

    suspend fun detectPdfType(uri: Uri): PdfType = withContext(Dispatchers.IO) {
        var textPageCount = 0
        var emptyPageCount = 0
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                val document = PDDocument.load(FileInputStream(pfd.fileDescriptor), com.tom_roush.pdfbox.io.MemoryUsageSetting.setupTempFileOnly())
                val stripper = PDFTextStripper()
                val samplePages = document.numberOfPages.coerceAtMost(5)
                for (i in 1..samplePages) {
                    stripper.startPage = i
                    stripper.endPage = i
                    val text = stripper.getText(document).trim()
                    if (text.length > 30) {
                        textPageCount++
                    } else {
                        emptyPageCount++
                    }
                }
                document.close()
            }
        } catch (e: Exception) {
            Log.e("PdfTextSearcher", "Error detecting PDF type", e)
        }

        return@withContext when {
            textPageCount > 0 && emptyPageCount == 0 -> PdfType.TEXT_BASED
            textPageCount == 0 && emptyPageCount > 0 -> PdfType.SCANNED
            else -> PdfType.HYBRID
        }
    }

    suspend fun getPagesText(uri: Uri, bitmapProvider: (suspend (Int) -> Bitmap?)? = null): List<String> = withContext(Dispatchers.IO) {
        val pagesText = mutableListOf<String>()
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                val document = PDDocument.load(FileInputStream(pfd.fileDescriptor), com.tom_roush.pdfbox.io.MemoryUsageSetting.setupTempFileOnly())
                val stripper = PDFTextStripper()
                val maxPages = document.numberOfPages.coerceAtMost(500)
                for (i in 1..maxPages) {
                    stripper.startPage = i
                    stripper.endPage = i
                    var text = stripper.getText(document).trim()

                    // OCR Fallback for scanned/empty text pages
                    if (text.isBlank() && bitmapProvider != null) {
                        val bitmap = bitmapProvider(i - 1)
                        if (bitmap != null) {
                            val ocrRes = OCRProcessor.recognizeTextWithBounds(bitmap)
                            text = ocrRes.text.trim()
                        }
                    }

                    pagesText.add(text)
                }
                document.close()
            }
        } catch (e: Exception) {
            Log.e("PdfTextSearcher", "Error extracting text", e)
        }
        return@withContext pagesText
    }

    suspend fun search(
        uri: Uri,
        query: String,
        bitmapProvider: (suspend (Int) -> Bitmap?)? = null
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<SearchResult>()
        if (query.isBlank()) return@withContext results
        val normalizedQuery = query.normalizeSearch()

        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                val document = PDDocument.load(FileInputStream(pfd.fileDescriptor), com.tom_roush.pdfbox.io.MemoryUsageSetting.setupTempFileOnly())
                val maxPages = document.numberOfPages.coerceAtMost(100)

                for (i in 1..maxPages) {
                    val page = document.getPage(i - 1)
                    val stripper = BoundingBoxStripper()
                    stripper.startPage = i
                    stripper.endPage = i
                    val cropBox = page.cropBox ?: page.mediaBox
                    val rotation = page.rotation
                    if (rotation == 90 || rotation == 270) {
                        stripper.pageWidth = cropBox.height
                        stripper.pageHeight = cropBox.width
                    } else {
                        stripper.pageWidth = cropBox.width
                        stripper.pageHeight = cropBox.height
                    }

                    stripper.getText(document)
                    var words = stripper.wordBounds

                    // OCR Fallback if page text/word bounds are empty
                    if (words.isEmpty() && bitmapProvider != null) {
                        val bitmap = bitmapProvider(i - 1)
                        if (bitmap != null) {
                            val ocrRes = OCRProcessor.recognizeTextWithBounds(bitmap)
                            words = ocrRes.wordBounds.toMutableList()
                        }
                    }

                    if (words.isNotEmpty()) {
                        results.addAll(findMatches(i - 1, words, normalizedQuery, query))
                    }
                }
                document.close()
            }
        } catch (e: Exception) {
            Log.e("PdfTextSearcher", "Error searching text", e)
            val pagesText = getPagesText(uri, bitmapProvider)
            results.addAll(searchCached(pagesText, query))
        }
        return@withContext results
    }

    suspend fun searchMatches(
        uri: Uri,
        query: String,
        bitmapProvider: (suspend (Int) -> Bitmap?)? = null
    ): List<Match> = withContext(Dispatchers.IO) {
        val matches = mutableListOf<Match>()
        if (query.isBlank()) return@withContext matches
        val normalizedQuery = query.normalizeSearch()

        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                val document = PDDocument.load(FileInputStream(pfd.fileDescriptor), com.tom_roush.pdfbox.io.MemoryUsageSetting.setupTempFileOnly())
                val maxPages = document.numberOfPages.coerceAtMost(200)

                for (i in 1..maxPages) {
                    val page = document.getPage(i - 1)
                    val stripper = BoundingBoxStripper()
                    stripper.startPage = i
                    stripper.endPage = i
                    val cropBox = page.cropBox ?: page.mediaBox
                    val rotation = page.rotation
                    if (rotation == 90 || rotation == 270) {
                        stripper.pageWidth = cropBox.height
                        stripper.pageHeight = cropBox.width
                    } else {
                        stripper.pageWidth = cropBox.width
                        stripper.pageHeight = cropBox.height
                    }

                    stripper.getText(document)
                    var words = stripper.wordBounds

                    if (words.isEmpty() && bitmapProvider != null) {
                        val bitmap = bitmapProvider(i - 1)
                        if (bitmap != null) {
                            val ocrRes = OCRProcessor.recognizeTextWithBounds(bitmap)
                            words = ocrRes.wordBounds.toMutableList()
                        }
                    }

                    if (words.isNotEmpty()) {
                        matches.addAll(findIndividualMatches(i - 1, words, normalizedQuery, query))
                    }
                }
                document.close()
            }
        } catch (e: Exception) {
            Log.e("PdfTextSearcher", "Error searching matches", e)
        }
        return@withContext matches
    }

    private fun findIndividualMatches(
        pageIndex: Int,
        words: List<Pair<String, RectF>>,
        normalizedQuery: String,
        originalQuery: String
    ): List<Match> {
        val matches = mutableListOf<Match>()
        if (words.isEmpty() || normalizedQuery.isBlank()) return matches

        val fullTextBuilder = StringBuilder()
        val wordPositions = mutableListOf<Int>()

        for (w in words) {
            wordPositions.add(fullTextBuilder.length)
            fullTextBuilder.append(w.first).append(" ")
        }

        val rawFullText = fullTextBuilder.toString()
        val fullTextNormalized = rawFullText.normalizeSearch()
        var index = 0

        while (index < fullTextNormalized.length) {
            val found = fullTextNormalized.indexOf(normalizedQuery, index)
            if (found != -1) {
                val startChar = found
                val endChar = found + normalizedQuery.length
                val matchBounds = mutableListOf<RectF>()

                for (i in words.indices) {
                    val wordStart = wordPositions[i]
                    val wordEnd = wordStart + words[i].first.length
                    if (wordEnd >= startChar && wordStart <= endChar) {
                        matchBounds.add(words[i].second)
                    }
                }

                if (matchBounds.isNotEmpty()) {
                    var minL = Float.MAX_VALUE
                    var minT = Float.MAX_VALUE
                    var maxR = Float.MIN_VALUE
                    var maxB = Float.MIN_VALUE
                    for (r in matchBounds) {
                        minL = min(minL, r.left)
                        minT = min(minT, r.top)
                        maxR = max(maxR, r.right)
                        maxB = max(maxB, r.bottom)
                    }
                    val primaryRect = RectF(minL, minT, maxR, maxB)

                    val snipStart = max(0, found - 30)
                    val snipEnd = min(rawFullText.length, found + originalQuery.length + 30)
                    var snippet = rawFullText.substring(snipStart, snipEnd).replace('\n', ' ').trim()
                    if (snipStart > 0) snippet = "...$snippet"
                    if (snipEnd < rawFullText.length) snippet = "$snippet..."

                    matches.add(Match(page = pageIndex, rect = primaryRect, bounds = matchBounds, snippet = snippet))
                }

                index = found + max(1, normalizedQuery.length)
            } else {
                break
            }
        }

        return matches
    }

    private fun findMatches(
        pageIndex: Int,
        words: List<Pair<String, RectF>>,
        normalizedQuery: String,
        originalQuery: String
    ): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        val fullTextBuilder = StringBuilder()
        val wordPositions = mutableListOf<Int>()

        for (w in words) {
            wordPositions.add(fullTextBuilder.length)
            fullTextBuilder.append(w.first).append(" ")
        }

        val rawFullText = fullTextBuilder.toString()
        val fullTextNormalized = rawFullText.normalizeSearch()
        var index = 0
        var count = 0
        val matchingBounds = mutableListOf<RectF>()

        while (index < fullTextNormalized.length) {
            val found = fullTextNormalized.indexOf(normalizedQuery, index)
            if (found != -1) {
                count++
                val startChar = found
                val endChar = found + normalizedQuery.length

                for (i in words.indices) {
                    val wordStart = wordPositions[i]
                    val wordEnd = wordStart + words[i].first.length
                    if (wordEnd >= startChar && wordStart <= endChar) {
                        matchingBounds.add(words[i].second)
                    }
                }
                index = found + max(1, normalizedQuery.length)
            } else {
                break
            }
        }

        if (count > 0) {
            val idx = fullTextNormalized.indexOf(normalizedQuery)
            val snipStart = max(0, idx - 40)
            val snipEnd = min(rawFullText.length, idx + originalQuery.length + 40)
            var snippet = rawFullText.substring(snipStart, min(snipEnd, rawFullText.length)).replace('\n', ' ').trim()
            if (snipStart > 0) snippet = "...$snippet"
            if (snipEnd < rawFullText.length) snippet = "$snippet..."

            results.add(SearchResult(pageIndex, snippet, count, matchingBounds))
        }
        return results
    }

    fun searchCached(pagesText: List<String>, query: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        if (query.isBlank()) return results
        val normQuery = query.normalizeSearch()
        pagesText.forEachIndexed { index, text ->
            val normText = text.normalizeSearch()
            val count = countOccurrences(normText, normQuery)
            if (count > 0) {
                results.add(
                    SearchResult(
                        pageNumber = index,
                        snippet = createSnippet(text, query),
                        occurrencesCount = count,
                        bounds = emptyList()
                    )
                )
            }
        }
        return results
    }

    suspend fun getPageSearchBounds(
        uri: Uri,
        pageIndex: Int,
        query: String,
        pageBitmap: Bitmap? = null
    ): List<RectF> = withContext(Dispatchers.IO) {
        val bounds = mutableListOf<RectF>()
        if (query.isBlank()) return@withContext bounds
        val normalizedQuery = query.normalizeSearch()

        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                val document = PDDocument.load(FileInputStream(pfd.fileDescriptor), com.tom_roush.pdfbox.io.MemoryUsageSetting.setupTempFileOnly())
                val page = document.getPage(pageIndex)
                val stripper = BoundingBoxStripper()
                stripper.startPage = pageIndex + 1
                stripper.endPage = pageIndex + 1
                val mediaBox = page.mediaBox
                stripper.pageWidth = mediaBox.width
                stripper.pageHeight = mediaBox.height

                stripper.getText(document)
                var words = stripper.wordBounds

                // OCR Fallback if page text/word bounds are empty
                if (words.isEmpty() && pageBitmap != null) {
                    val ocrRes = OCRProcessor.recognizeTextWithBounds(pageBitmap)
                    words = ocrRes.wordBounds.toMutableList()
                }

                if (words.isNotEmpty()) {
                    val fullTextBuilder = StringBuilder()
                    val wordPositions = mutableListOf<Int>()
                    for (w in words) {
                        wordPositions.add(fullTextBuilder.length)
                        fullTextBuilder.append(w.first).append(" ")
                    }
                    val fullText = fullTextBuilder.toString().normalizeSearch()
                    var index = 0
                    while (index < fullText.length) {
                        val found = fullText.indexOf(normalizedQuery, index)
                        if (found != -1) {
                            val startChar = found
                            val endChar = found + normalizedQuery.length
                            for (i in words.indices) {
                                val wordStart = wordPositions[i]
                                val wordEnd = wordStart + words[i].first.length
                                if (wordEnd >= startChar && wordStart <= endChar) {
                                    bounds.add(words[i].second)
                                }
                            }
                            index = found + max(1, normalizedQuery.length)
                        } else {
                            break
                        }
                    }
                }
                document.close()
            }
        } catch (e: Exception) {
            Log.e("PdfTextSearcher", "Error getting page bounds", e)
            if (pageBitmap != null) {
                val ocrRes = OCRProcessor.recognizeTextWithBounds(pageBitmap)
                val words = ocrRes.wordBounds
                if (words.isNotEmpty()) {
                    val fullTextBuilder = StringBuilder()
                    val wordPositions = mutableListOf<Int>()
                    for (w in words) {
                        wordPositions.add(fullTextBuilder.length)
                        fullTextBuilder.append(w.first).append(" ")
                    }
                    val fullText = fullTextBuilder.toString().normalizeSearch()
                    var index = 0
                    while (index < fullText.length) {
                        val found = fullText.indexOf(normalizedQuery, index)
                        if (found != -1) {
                            val startChar = found
                            val endChar = found + normalizedQuery.length
                            for (i in words.indices) {
                                val wordStart = wordPositions[i]
                                val wordEnd = wordStart + words[i].first.length
                                if (wordEnd >= startChar && wordStart <= endChar) {
                                    bounds.add(words[i].second)
                                }
                            }
                            index = found + max(1, normalizedQuery.length)
                        } else {
                            break
                        }
                    }
                }
            }
        }
        return@withContext bounds
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
                index = found + max(1, query.length)
            } else {
                break
            }
        }
        return count
    }

    private fun createSnippet(text: String, query: String): String {
        val normText = text.normalizeSearch()
        val normQuery = query.normalizeSearch()
        val idx = normText.indexOf(normQuery)
        if (idx == -1) return text.take(100) + "..."
        val start = (idx - 40).coerceAtLeast(0)
        val end = (idx + query.length + 40).coerceAtMost(text.length)
        var snippet = text.substring(start, end).replace('\n', ' ').trim()
        if (start > 0) snippet = "...$snippet"
        if (end < text.length) snippet = "$snippet..."
        return snippet
    }
}
