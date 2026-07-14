package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.ByteArrayOutputStream
import java.util.zip.Inflater

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

    /**
     * Search for a query term inside a PDF and return page-matched snippets.
     */
    suspend fun getPagesText(uri: android.net.Uri): List<String> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bytes = inputStream.readBytes()
                return@withContext extractTextByPage(bytes)
            }
        } catch (e: Exception) {
            Log.e("PdfTextSearcher", "Error extracting text pages", e)
        }
        return@withContext emptyList()
    }

    suspend fun search(uri: Uri, query: String): List<SearchResult> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val results = mutableListOf<SearchResult>()
        if (query.isBlank()) return@withContext results

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bytes = inputStream.readBytes()
                val pagesText = extractTextByPage(bytes)
                
                pagesText.forEachIndexed { index, text ->
                    if (text.contains(query, ignoreCase = true)) {
                        val occurrences = countOccurrences(text, query)
                        val snippet = createSnippet(text, query)
                        results.add(SearchResult(
                            pageNumber = index,
                            snippet = snippet,
                            occurrencesCount = occurrences
                        ))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PdfTextSearcher", "Error searching PDF text", e)
        }
        return@withContext results
    }

    /**
     * Search already-cached page texts instantaneously in-memory.
     */
    fun searchCached(pagesText: List<String>, query: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        if (query.isBlank()) return results
        pagesText.forEachIndexed { index, text ->
            if (text.contains(query, ignoreCase = true)) {
                val occurrences = countOccurrences(text, query)
                val snippet = createSnippet(text, query)
                results.add(SearchResult(
                    pageNumber = index,
                    snippet = snippet,
                    occurrencesCount = occurrences
                ))
            }
        }
        return results
    }

    /**
     * Extracts outline/chapters. Uses a combination of PDF outline object parsing and 
     * smart structural heading fallback parsing.
     */
    suspend fun extractOutline(uri: Uri): List<OutlineItem> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val outline = mutableListOf<OutlineItem>()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bytes = inputStream.readBytes()
                
                // 1. Try structural heading fallback by scanning pages first (it's extremely reliable)
                val pagesText = extractTextByPage(bytes)
                val headingRegex = Regex("^(Chapter|Qaybta|Bogga|Hordhac|Cutubka|Laanta|Introduction|Section|Part)\\s+\\d+.*", RegexOption.IGNORE_CASE)
                
                pagesText.forEachIndexed { index, text ->
                    val lines = text.split("\n")
                    for (line in lines) {
                        val trimmed = line.trim()
                        if (trimmed.length in 3..60 && (headingRegex.matches(trimmed) || trimmed.startsWith("Hordhac", ignoreCase = true) || trimmed.startsWith("Gunaanad", ignoreCase = true))) {
                            outline.add(OutlineItem(title = trimmed, pageNumber = index))
                            break // Only take first major heading per page
                        }
                    }
                }

                // 2. Try formal PDF outline dictionaries if heading fallback was empty
                if (outline.isEmpty()) {
                    outline.addAll(parseFormalPdfOutline(bytes, pagesText.size))
                }
            }
        } catch (e: Exception) {
            Log.e("PdfTextSearcher", "Error extracting PDF outline", e)
        }
        
        // Remove duplicates and sort by page
        return@withContext outline.distinctBy { it.title }.sortedBy { it.pageNumber }
    }

    private fun countOccurrences(text: String, query: String): Int {
        var count = 0
        var index = 0
        while (true) {
            index = text.indexOf(query, index, ignoreCase = true)
            if (index == -1) break
            count++
            index += query.length
        }
        return count
    }

    private fun createSnippet(text: String, query: String): String {
        val index = text.indexOf(query, ignoreCase = true)
        if (index == -1) return text.take(60)
        
        val start = (index - 30).coerceAtLeast(0)
        val end = (index + query.length + 40).coerceAtMost(text.length)
        
        var snippet = text.substring(start, end).replace('\n', ' ').trim()
        if (start > 0) snippet = "...$snippet"
        if (end < text.length) snippet = "$snippet..."
        return snippet
    }

    private fun extractTextByPage(pdfBytes: ByteArray): List<String> {
        val pagesText = mutableListOf<String>()
        val streams = findStreams(pdfBytes)

        for (streamBytes in streams) {
            val text = parseTextFromStream(streamBytes)
            if (text.isNotBlank() && text.length > 3) {
                pagesText.add(text)
            }
        }
        return pagesText
    }

    private fun findStreams(pdfBytes: ByteArray): List<ByteArray> {
        val streams = mutableListOf<ByteArray>()
        var index = 0
        
        while (index < pdfBytes.size) {
            val streamStartIndex = findSequence(pdfBytes, "stream".toByteArray(), index)
            if (streamStartIndex == -1) break
            
            val streamEndIndex = findSequence(pdfBytes, "endstream".toByteArray(), streamStartIndex)
            if (streamEndIndex == -1) break
            
            var start = streamStartIndex + 6
            if (start < pdfBytes.size && pdfBytes[start] == '\r'.toByte()) start++
            if (start < pdfBytes.size && pdfBytes[start] == '\n'.toByte()) start++
            
            val length = streamEndIndex - start
            if (length > 0) {
                val streamContent = pdfBytes.copyOfRange(start, start + length)
                
                // Inspect header (from streamStartIndex backwards, up to 128 bytes)
                val headerStart = (streamStartIndex - 128).coerceAtLeast(0)
                val headerBytes = pdfBytes.copyOfRange(headerStart, streamStartIndex)
                val headerString = String(headerBytes, Charsets.ISO_8859_1)
                
                val isCompressed = headerString.contains("/FlateDecode") || headerString.contains("/Flate")
                
                try {
                    val decompressed = if (isCompressed) decompress(streamContent) else streamContent
                    if (decompressed != null) {
                        streams.add(decompressed)
                    }
                } catch (e: Exception) {
                    // Ignore decompression failures for binary media streams
                }
            }
            index = streamEndIndex + 9
        }
        return streams
    }

    private fun findSequence(bytes: ByteArray, sequence: ByteArray, startIndex: Int): Int {
        for (i in startIndex..bytes.size - sequence.size) {
            var found = true
            for (j in sequence.indices) {
                if (bytes[i + j] != sequence[j]) {
                    found = false
                    break
                }
            }
            if (found) return i
        }
        return -1
    }

    private fun decompress(compressedBytes: ByteArray): ByteArray? {
        val inflater = Inflater()
        inflater.setInput(compressedBytes)
        val outputStream = ByteArrayOutputStream(compressedBytes.size * 2)
        val buffer = ByteArray(2048)
        try {
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                if (count == 0 && inflater.needsInput()) break
                outputStream.write(buffer, 0, count)
            }
            inflater.end()
            return outputStream.toByteArray()
        } catch (e: Exception) {
            return null
        }
    }

    private fun parseTextFromStream(decompressedBytes: ByteArray): String {
        val content = String(decompressedBytes, Charsets.ISO_8859_1)
        val sb = java.lang.StringBuilder()
        
        var index = 0
        while (index < content.length) {
            val btIndex = content.indexOf("BT", index)
            if (btIndex == -1) break
            
            val etIndex = content.indexOf("ET", btIndex)
            if (etIndex == -1) break
            
            val btText = content.substring(btIndex + 2, etIndex)
            
            var pIndex = 0
            while (pIndex < btText.length) {
                val openParen = btText.indexOf("(", pIndex)
                if (openParen == -1) break
                
                val closeParen = findMatchingParen(btText, openParen)
                if (closeParen == -1) break
                
                val textSegment = btText.substring(openParen + 1, closeParen)
                sb.append(decodePdfString(textSegment)).append(" ")
                
                pIndex = closeParen + 1
            }
            index = etIndex + 2
        }
        return sb.toString().trim()
    }

    private fun findMatchingParen(text: String, openIndex: Int): Int {
        var count = 1
        for (i in openIndex + 1 until text.length) {
            val c = text[i]
            if (c == '(' && (i == 0 || text[i - 1] != '\\')) {
                count++
            } else if (c == ')' && (i == 0 || text[i - 1] != '\\')) {
                count--
                if (count == 0) return i
            }
        }
        return -1
    }

    private fun decodePdfString(segment: String): String {
        val sb = java.lang.StringBuilder()
        var i = 0
        while (i < segment.length) {
            val c = segment[i]
            if (c == '\\' && i + 1 < segment.length) {
                val next = segment[i + 1]
                if (next.isDigit()) {
                    var octalStr = ""
                    var j = i + 1
                    while (j < segment.length && j < i + 4 && segment[j].isDigit()) {
                        octalStr += segment[j]
                        j++
                    }
                    try {
                        val code = octalStr.toInt(8)
                        sb.append(code.toChar())
                    } catch (e: Exception) {
                        sb.append(octalStr)
                    }
                    i = j
                } else {
                    when (next) {
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        '(' -> sb.append('(')
                        ')' -> sb.append(')')
                        '\\' -> sb.append('\\')
                        else -> sb.append(next)
                    }
                    i += 2
                }
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }

    private fun parseFormalPdfOutline(pdfBytes: ByteArray, maxPages: Int): List<OutlineItem> {
        val items = mutableListOf<OutlineItem>()
        val content = String(pdfBytes, Charsets.ISO_8859_1)
        
        // Scan for `/Title` and destination entries `/Dest`
        var index = 0
        val titleTag = "/Title"
        
        while (index < content.length) {
            val titleIdx = content.indexOf(titleTag, index)
            if (titleIdx == -1) break
            
            // Extract title between `(` and `)`
            val openParen = content.indexOf("(", titleIdx)
            if (openParen != -1 && openParen - titleIdx < 30) {
                val closeParen = findMatchingParen(content, openParen)
                if (closeParen != -1) {
                    val rawTitle = content.substring(openParen + 1, closeParen)
                    val title = decodePdfString(rawTitle).trim()
                    
                    if (title.isNotBlank() && title.length < 100) {
                        // Find a page number reference nearby
                        var pageNum = items.size // Fallback: default to sequential
                        val searchWindow = content.substring(closeParen, (closeParen + 128).coerceAtMost(content.length))
                        val destMatch = Regex("/Dest\\s*\\[\\s*(\\d+)\\s+").find(searchWindow)
                        if (destMatch != null) {
                            pageNum = destMatch.groupValues[1].toIntOrNull()?.minus(1) ?: pageNum
                        } else {
                            val countMatch = Regex("/Page\\s+(\\d+)").find(searchWindow)
                            if (countMatch != null) {
                                pageNum = countMatch.groupValues[1].toIntOrNull()?.minus(1) ?: pageNum
                            }
                        }
                        
                        items.add(OutlineItem(title = title, pageNumber = pageNum.coerceIn(0, maxPages - 1)))
                    }
                }
            }
            index = titleIdx + 6
        }
        return items
    }
}
