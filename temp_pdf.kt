                .pointerInput(isDrawingMode) {
                    if (isDrawingMode) {
                        androidx.compose.foundation.gestures.detectDragGestures(
                            onDragStart = { startOffset ->
                                currentStroke = DrawingStroke(
                                    points = listOf(startOffset),
                                    color = if (isHighlighterMode) selectedColor.copy(alpha = 0.4f) else selectedColor,
                                    width = strokeWidth,
                                    isEraser = isEraserMode
                                )
                            },
                            onDrag = { change, _ ->
                                currentStroke = currentStroke?.copy(
                                    points = currentStroke!!.points + change.position
                                )
                            },
                            onDragEnd = {
                                currentStroke?.let { stroke ->
                                    if (stroke.points.size > 1) {
                                        currentPdf?.uriString?.let { uri ->
                                            viewModel.addStroke(uri, pageIndex, stroke)
                                        }
                                    }
                                }
                                currentStroke = null
                            },
                            onDragCancel = {
                                currentStroke = null
                            }
                        )
                    } else {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            do {
                                val event = awaitPointerEvent()
                                val canceled = event.changes.any { it.isConsumed }
                                if (!canceled) {
                                    val zoomChange = event.calculateZoom()
                                    val panChange = event.calculatePan()
                                    
                                    if (zoomChange != 1f || panChange != androidx.compose.ui.geometry.Offset.Zero) {
                                        val newScale = (scale * zoomChange).coerceIn(1f, 5f)
                                        scale = newScale
                                        if (scale == 1f) {
                                            offset = androidx.compose.ui.geometry.Offset.Zero
                                        } else {
                                            offset += panChange * scale
                                        }
                                    }
                                    
                                    val isMultiTouch = event.changes.size > 1
                                    if (isMultiTouch || scale > 1f) {
                                        event.changes.forEach {
                                            if (it.positionChanged()) {
                                                it.consume()
                                            }
                                        }
                                    }
                                }
                            } while (!canceled && event.changes.any { it.pressed })
                        }
                    }
                }
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                ),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
