data class DrawingStroke(
    val points: List<Offset>,
    val color: Color,
    val width: Float,
    val isEraser: Boolean = false
)
