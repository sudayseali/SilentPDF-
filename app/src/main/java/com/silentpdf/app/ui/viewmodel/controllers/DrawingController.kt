package com.silentpdf.app.ui.viewmodel.controllers

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.silentpdf.app.ui.viewmodel.DrawingStroke
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DrawingController {
    private val _pageDrawings = MutableStateFlow<Map<String, Map<Int, List<DrawingStroke>>>>(emptyMap())
    val pageDrawings: StateFlow<Map<String, Map<Int, List<DrawingStroke>>>> = _pageDrawings

    private val _redoStack = MutableStateFlow<List<Pair<Pair<String, Int>, DrawingStroke>>>(emptyList())

    fun addStroke(pdfUri: String, page: Int, stroke: DrawingStroke) {
        val currentDrawings = _pageDrawings.value.toMutableMap()
        val pdfDrawings = currentDrawings[pdfUri]?.toMutableMap() ?: mutableMapOf()
        val pageStrokes = pdfDrawings[page]?.toMutableList() ?: mutableListOf()
        pageStrokes.add(stroke)
        pdfDrawings[page] = pageStrokes
        currentDrawings[pdfUri] = pdfDrawings
        _pageDrawings.value = currentDrawings
        _redoStack.value = emptyList() // clear redo stack on new action
    }

    fun undoLastStroke(pdfUri: String, page: Int) {
        val currentDrawings = _pageDrawings.value.toMutableMap()
        val pdfDrawings = currentDrawings[pdfUri]?.toMutableMap() ?: mutableMapOf()
        val pageStrokes = pdfDrawings[page]?.toMutableList() ?: mutableListOf()
        if (pageStrokes.isNotEmpty()) {
            val removedStroke = pageStrokes.removeAt(pageStrokes.lastIndex)
            pdfDrawings[page] = pageStrokes
            currentDrawings[pdfUri] = pdfDrawings
            _pageDrawings.value = currentDrawings
            _redoStack.value = _redoStack.value + Pair(Pair(pdfUri, page), removedStroke)
        }
    }

    fun redoLastStroke(pdfUri: String, page: Int) {
        val stack = _redoStack.value.toMutableList()
        val lastRedoIndex = stack.indexOfLast { it.first.first == pdfUri && it.first.second == page }
        if (lastRedoIndex != -1) {
            val lastRedo = stack.removeAt(lastRedoIndex)
            _redoStack.value = stack
            
            val currentDrawings = _pageDrawings.value.toMutableMap()
            val pdfDrawings = currentDrawings[pdfUri]?.toMutableMap() ?: mutableMapOf()
            val pageStrokes = pdfDrawings[page]?.toMutableList() ?: mutableListOf()
            pageStrokes.add(lastRedo.second)
            pdfDrawings[page] = pageStrokes
            currentDrawings[pdfUri] = pdfDrawings
            _pageDrawings.value = currentDrawings
        }
    }
}
