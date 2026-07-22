with open("app/src/main/java/com/silentpdf/app/ui/screens/ReaderScreen.kt", "r") as f:
    content = f.read()

replacement = """                                            DropdownMenuItem(
                                                text = { Text(if (isBionicMode) "Exit Bionic Mode" else "Bionic Reading", color = readerOnSurfaceColor) },
                                                leadingIcon = { Icon(Icons.Default.Bolt, null, tint = Color(0xFFFFB300)) },
                                                onClick = {
                                                    isBionicMode = !isBionicMode
                                                    showMoreMenu = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Extract Text (OCR)", color = readerOnSurfaceColor) }"""

content = content.replace("                                            DropdownMenuItem(\n                                                text = { Text(\"Extract Text (OCR)\", color = readerOnSurfaceColor) }", replacement)

with open("app/src/main/java/com/silentpdf/app/ui/screens/ReaderScreen.kt", "w") as f:
    f.write(content)
