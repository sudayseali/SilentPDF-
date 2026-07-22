package com.silentpdf.app.bionic

import android.graphics.Bitmap
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

    suspend fun recognizeText(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            try {
                val image = InputImage.fromBitmap(bitmap, 0)
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        continuation.resume(visionText.text)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "OCR recognition failed", e)
                        continuation.resume("")
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process bitmap with ML Kit", e)
                continuation.resume("")
            }
        }
    }
}
