sed -i '754c\
                                                            val matches = com.silentpdf.app.ui.components.findSearchMatches(result.snippet, searchInPdfQuery)\
                                                            com.silentpdf.app.ui.components.HighlightedText(\
                                                                text = result.snippet,\
                                                                matches = matches,\
                                                                currentMatchIndex = -1, // No specific match is active in the snippet\
                                                                modifier = Modifier,\
                                                                activeHighlightColor = Color(0xFFFF9800),\
                                                                inactiveHighlightColor = Color(0x66FFEB3B),\
                                                                textColor = MaterialTheme.colorScheme.onSurface\
                                                            )\
' app/src/main/java/com/silentpdf/app/ui/screens/ReaderScreen.kt
