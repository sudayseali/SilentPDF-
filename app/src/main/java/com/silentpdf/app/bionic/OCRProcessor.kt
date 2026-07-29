package com.silentpdf.app.bionic

import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

object OCRProcessor {

    private const val TAG = "OCRProcessor"
    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    data class OCRResult(
        val text: String,
        val wordBounds: List<Pair<String, RectF>>
    )

    suspend fun recognizeText(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        recognizeTextWithBounds(bitmap).text
    }

    suspend fun recognizeTextWithBounds(bitmap: Bitmap): OCRResult = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            try {
                val image = InputImage.fromBitmap(bitmap, 0)
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val textBuilder = StringBuilder()
                        val wordBounds = mutableListOf<Pair<String, RectF>>()
                        val width = bitmap.width.toFloat()
                        val height = bitmap.height.toFloat()

                        for (block in visionText.textBlocks) {
                            for (line in block.lines) {
                                for (element in line.elements) {
                                    val box = element.boundingBox
                                    val wordText = element.text.trim()
                                    if (wordText.isNotEmpty()) {
                                        textBuilder.append(wordText).append(" ")
                                        if (box != null && width > 0f && height > 0f) {
                                            val normRect = RectF(
                                                (box.left / width).coerceIn(0f, 1f),
                                                (box.top / height).coerceIn(0f, 1f),
                                                (box.right / width).coerceIn(0f, 1f),
                                                (box.bottom / height).coerceIn(0f, 1f)
                                            )
                                            wordBounds.add(Pair(wordText, normRect))
                                        }
                                    }
                                }
                            }
                        }
                        val fullText = if (visionText.text.isNotBlank()) visionText.text else textBuilder.toString().trim()
                        continuation.resume(OCRResult(fullText, wordBounds))
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "OCR recognition failed", e)
                        continuation.resume(OCRResult("", emptyList()))
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process bitmap with ML Kit", e)
                continuation.resume(OCRResult("", emptyList()))
            }
        }
    }
}
