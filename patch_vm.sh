sed -i '1i import androidx.compose.ui.geometry.Offset\nimport androidx.compose.ui.graphics.Color\n\ndata class DrawingStroke(\n    val points: List<Offset>,\n    val color: Color,\n    val width: Float\n)' app/src/main/java/com/example/ui/viewmodel/SilentPdfViewModel.kt

sed -i '/val currentBookmarks/i \
    private val _pageDrawings = MutableStateFlow<Map<String, Map<Int, List<DrawingStroke>>>>(emptyMap())\n\
    val pageDrawings: StateFlow<Map<String, Map<Int, List<DrawingStroke>>>> = _pageDrawings\n\
\n\
    fun addStroke(pdfUri: String, page: Int, stroke: DrawingStroke) {\n\
        val currentDrawings = _pageDrawings.value.toMutableMap()\n\
        val pdfDrawings = currentDrawings[pdfUri]?.toMutableMap() ?: mutableMapOf()\n\
        val pageStrokes = pdfDrawings[page]?.toMutableList() ?: mutableListOf()\n\
        pageStrokes.add(stroke)\n\
        pdfDrawings[page] = pageStrokes\n\
        currentDrawings[pdfUri] = pdfDrawings\n\
        _pageDrawings.value = currentDrawings\n\
    }\n\
\n\
    fun undoLastStroke(pdfUri: String, page: Int) {\n\
        val currentDrawings = _pageDrawings.value.toMutableMap()\n\
        val pdfDrawings = currentDrawings[pdfUri]?.toMutableMap() ?: mutableMapOf()\n\
        val pageStrokes = pdfDrawings[page]?.toMutableList() ?: mutableListOf()\n\
        if (pageStrokes.isNotEmpty()) {\n\
            pageStrokes.removeLast()\n\
            pdfDrawings[page] = pageStrokes\n\
            currentDrawings[pdfUri] = pdfDrawings\n\
            _pageDrawings.value = currentDrawings\n\
        }\n\
    }\n' app/src/main/java/com/example/ui/viewmodel/SilentPdfViewModel.kt
