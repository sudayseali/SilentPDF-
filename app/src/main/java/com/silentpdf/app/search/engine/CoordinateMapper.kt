package com.silentpdf.app.search.engine

import android.graphics.RectF
import kotlin.math.min

object CoordinateMapper {
    /**
     * Converts a rectangle from PDF coordinate space to Android View (Canvas) coordinate space.
     *
     * @param pdfRect The bounding box in normalized PDF space (0f to 1f).
     * @param pageWidth The logical page width.
     * @param pageHeight The logical page height.
     * @param viewWidth The size of the View rendering the PDF horizontally.
     * @param viewHeight The size of the View rendering the PDF vertically.
     * @param zoom The current scale factor.
     * @param offsetX The current horizontal translation.
     * @param offsetY The current vertical translation.
     */
    fun map(
        pdfRect: RectF,
        pageWidth: Float,
        pageHeight: Float,
        viewWidth: Float,
        viewHeight: Float,
        zoom: Float,
        offsetX: Float,
        offsetY: Float
    ): RectF {
        // Calculate aspect ratio fit of the PDF page inside the View
        val scaleX = viewWidth / pageWidth
        val scaleY = viewHeight / pageHeight
        val fitScale = min(scaleX, scaleY)
        
        val fittedWidth = pageWidth * fitScale
        val fittedHeight = pageHeight * fitScale
        
        // Calculate centering offsets
        val dx = (viewWidth - fittedWidth) / 2f
        val dy = (viewHeight - fittedHeight) / 2f
        
        // Assuming pdfRect is normalized (0..1) since our engines normalize all rects
        val left = (pdfRect.left * fittedWidth) + dx
        val top = (pdfRect.top * fittedHeight) + dy
        val right = (pdfRect.right * fittedWidth) + dx
        val bottom = (pdfRect.bottom * fittedHeight) + dy
        
        // Apply zoom and offset
        val scaledLeft = left * zoom + offsetX
        val scaledTop = top * zoom + offsetY
        val scaledRight = right * zoom + offsetX
        val scaledBottom = bottom * zoom + offsetY
        
        return RectF(scaledLeft, scaledTop, scaledRight, scaledBottom)
    }
}
