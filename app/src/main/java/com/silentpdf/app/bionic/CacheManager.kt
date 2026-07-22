package com.silentpdf.app.bionic

import android.util.LruCache

object CacheManager {

    // Store up to 50 processed pages in memory
    private val cache = LruCache<String, ProcessedBionicPage>(50)

    fun get(pdfUri: String, pageIndex: Int, config: BionicConfig): ProcessedBionicPage? {
        val key = buildKey(pdfUri, pageIndex, config)
        return cache.get(key)
    }

    fun put(pdfUri: String, pageIndex: Int, config: BionicConfig, result: ProcessedBionicPage) {
        val key = buildKey(pdfUri, pageIndex, config)
        cache.put(key, result)
    }

    fun clear() {
        cache.evictAll()
    }

    private fun buildKey(pdfUri: String, pageIndex: Int, config: BionicConfig): String {
        return "${pdfUri}_p${pageIndex}_${config.toCacheKey()}"
    }
}
