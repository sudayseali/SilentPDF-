package com.silentpdf.app.search.engine

import android.graphics.RectF
import com.silentpdf.app.search.domain.SearchResult
import java.text.Normalizer
import kotlin.math.max
import kotlin.math.min

object SearchEngine {

    fun String.normalizeSearch(): String {
        var text = this
        text = text.replace("ﬁ", "fi").replace("ﬂ", "fl").replace("æ", "ae").replace("œ", "oe").replace("ß", "ss")
        text = text.replace("\u00AD", "").replace("\u200B", "").replace("\u00A0", " ").replace(Regex("-\\s*\\n\\s*"), "")
        text = text.replace(Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670]"), "") // Arabic shaping
        val nfd = Normalizer.normalize(text, Normalizer.Form.NFD)
        return Normalizer.normalize(nfd.replace(Regex("\\p{Mn}+"), ""), Normalizer.Form.NFC).lowercase()
    }

    fun searchPage(pageIndex: Int, words: List<Pair<String, RectF>>, query: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        if (words.isEmpty() || query.isBlank()) return results

        val normalizedQuery = query.normalizeSearch()
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
                    
                    // Allow partial word overlaps!
                    if (wordEnd > startChar && wordStart < endChar) {
                        matchBounds.add(words[i].second)
                    }
                }

                if (matchBounds.isNotEmpty()) {
                    val snipStart = max(0, found - 30)
                    val snipEnd = min(rawFullText.length, found + query.length + 30)
                    var snippet = rawFullText.substring(snipStart, snipEnd).replace('\n', ' ').trim()
                    if (snipStart > 0) snippet = "...$snippet"
                    if (snipEnd < rawFullText.length) snippet = "$snippet..."

                    results.add(SearchResult(pageIndex, matchBounds, snippet))
                }

                index = found + max(1, normalizedQuery.length)
            } else {
                break
            }
        }
        return results
    }
}
