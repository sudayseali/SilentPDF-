with open("app/src/main/java/com/silentpdf/app/ui/screens/ReaderScreen.kt", "r") as f:
    content = f.read()

replacement = """                    } else if (pageBitmap != null) {
                        val bitmap = pageBitmap!!
                        if (isBionicMode) {
                            val pageText = openedPdfTextPages.getOrNull(currentPage) ?: "Extracting text..."
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                            ) {
                                item {
                                    BionicText(
                                        text = pageText.ifEmpty { "No text found on this page." },
                                        color = readerOnSurfaceColor
                                    )
                                }
                            }
                        } else {
                        Box("""

content = content.replace("                    } else if (pageBitmap != null) {\n                        val bitmap = pageBitmap!!\n                        Box(", replacement)

content = content.replace("                                    }\n                                }\n                            }\n                        }\n                    }", "                                    }\n                                }\n                            }\n                        }\n                    }\n                    }")

with open("app/src/main/java/com/silentpdf/app/ui/screens/ReaderScreen.kt", "w") as f:
    f.write(content)
