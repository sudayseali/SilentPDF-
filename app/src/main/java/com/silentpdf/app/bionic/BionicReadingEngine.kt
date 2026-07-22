package com.silentpdf.app.bionic

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object BionicReadingEngine {

    suspend fun processPage(
        pdfUri: String,
        pageIndex: Int,
        rawText: String,
        bitmap: Bitmap? = null,
        config: BionicConfig,
        textColor: Color = Color.Unspecified
    ): ProcessedBionicPage = withContext(Dispatchers.Default) {
        // 1. Check LRU Cache
        val cached = CacheManager.get(pdfUri, pageIndex, config)
        if (cached != null) {
            return@withContext cached
        }

        val startTime = System.currentTimeMillis()
        var textToUse = rawText.trim()
        var isOcrUsed = false

        // 2. OCR Fallback for scanned PDFs if raw text is empty
        if (textToUse.isEmpty() && config.autoOcrForScanned && bitmap != null) {
            textToUse = OCRProcessor.recognizeText(bitmap).trim()
            isOcrUsed = textToUse.isNotEmpty()
        }

        if (textToUse.isEmpty()) {
            textToUse = "No text content found on this page."
        }

        // 3. Language & Writing System Detection
        val detection = LanguageDetector.detect(textToUse, config.language)

        // 4. Retrieve Language Profile
        val profile = LanguageRuleManager.getProfile(detection.language)

        // 5. Tokenization & Analysis
        val tokens = TextAnalyzer.tokenize(textToUse, detection.script)

        // 6. Highlight Generation
        val annotatedString = HighlightGenerator.generate(
            tokens = tokens,
            profile = profile,
            config = config,
            direction = detection.textDirection,
            textColor = textColor
        )

        val endTime = System.currentTimeMillis()
        val result = ProcessedBionicPage(
            pageIndex = pageIndex,
            annotatedText = annotatedString,
            rawText = textToUse,
            detectedLanguage = detection.language,
            scriptType = detection.script,
            textDirection = detection.textDirection,
            isOcrUsed = isOcrUsed,
            processingTimeMs = endTime - startTime
        )

        // 7. Store in Cache
        CacheManager.put(pdfUri, pageIndex, config, result)

        return@withContext result
    }

    fun clearCache() {
        CacheManager.clear()
    }
}
