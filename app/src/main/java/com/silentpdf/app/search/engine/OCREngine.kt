package com.silentpdf.app.search.engine

import android.graphics.Bitmap
import android.graphics.RectF
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.silentpdf.app.data.db.OcrResultEntity
import com.silentpdf.app.data.db.SilentPdfDao
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min

class OCREngine(private val dao: SilentPdfDao) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun extractFromBitmap(pdfUriString: String, pageIndex: Int, bitmapProvider: suspend () -> Bitmap?): List<Pair<String, RectF>> = withContext(Dispatchers.IO) {
        // Check DB Cache first to avoid expensive bitmap rendering
        val cached = dao.getOcrResult(pdfUriString, pageIndex)
        if (cached != null) {
            return@withContext parseJsonBounds(cached.boundingBoxesJson)
        }

        val bitmap = bitmapProvider()
        if (bitmap == null) return@withContext emptyList()

        // Run ML Kit OCR
        val image = InputImage.fromBitmap(bitmap, 0)
        try {
            val visionText = recognizer.process(image).await()
            val words = mutableListOf<Pair<String, RectF>>()
            val w = bitmap.width.toFloat()
            val h = bitmap.height.toFloat()

            for (block in visionText.textBlocks) {
                for (line in block.lines) {
                    for (element in line.elements) {
                        val box = element.boundingBox
                        if (box != null && w > 0 && h > 0) {
                            val normRect = RectF(
                                max(0f, box.left.toFloat() / w),
                                max(0f, box.top.toFloat() / h),
                                min(1f, box.right.toFloat() / w),
                                min(1f, box.bottom.toFloat() / h)
                            )
                            words.add(Pair(element.text.trim(), normRect))
                        }
                    }
                }
            }
            
            // Cache in DB
            val json = serializeBounds(words)
            dao.insertOcrResult(OcrResultEntity(
                pdfUriString = pdfUriString,
                pageNumber = pageIndex,
                recognizedText = visionText.text,
                boundingBoxesJson = json
            ))
            
            return@withContext words
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }

    private fun serializeBounds(words: List<Pair<String, RectF>>): String {
        val array = JSONArray()
        for (w in words) {
            val obj = JSONObject()
            obj.put("t", w.first)
            obj.put("l", w.second.left.toDouble())
            obj.put("t_y", w.second.top.toDouble())
            obj.put("r", w.second.right.toDouble())
            obj.put("b", w.second.bottom.toDouble())
            array.put(obj)
        }
        return array.toString()
    }

    private fun parseJsonBounds(jsonStr: String): List<Pair<String, RectF>> {
        val list = mutableListOf<Pair<String, RectF>>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val text = obj.getString("t")
                val r = RectF(
                    obj.getDouble("l").toFloat(),
                    obj.getDouble("t_y").toFloat(),
                    obj.getDouble("r").toFloat(),
                    obj.getDouble("b").toFloat()
                )
                list.add(Pair(text, r))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
