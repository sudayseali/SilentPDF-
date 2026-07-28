sed -i '136,146c\
                        androidx.compose.foundation.gestures.awaitEachGesture {\
                            androidx.compose.foundation.gestures.awaitFirstDown(requireUnconsumed = false)\
                            do {\
                                val event = awaitPointerEvent()\
                                val canceled = event.changes.any { it.isConsumed }\
                                if (!canceled) {\
                                    val zoomChange = androidx.compose.foundation.gestures.calculateZoom()\
                                    val panChange = androidx.compose.foundation.gestures.calculatePan()\
                                    \
                                    if (zoomChange != 1f || panChange != androidx.compose.ui.geometry.Offset.Zero) {\
                                        val newScale = (scale * zoomChange).coerceIn(1f, 5f)\
                                        scale = newScale\
                                        if (scale == 1f) {\
                                            offset = androidx.compose.ui.geometry.Offset.Zero\
                                        } else {\
                                            offset += panChange * scale\
                                        }\
                                    }\
                                    \
                                    val isMultiTouch = event.changes.size > 1\
                                    if (isMultiTouch || scale > 1f) {\
                                        event.changes.forEach {\
                                            if (it.positionChanged()) {\
                                                it.consume()\
                                            }\
                                        }\
                                    }\
                                }\
                            } while (!canceled && event.changes.any { it.pressed })\
                        }\
' app/src/main/java/com/silentpdf/app/ui/screens/PdfPageItem.kt
