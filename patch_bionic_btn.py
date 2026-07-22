with open("app/src/main/java/com/silentpdf/app/ui/screens/ReaderScreen.kt", "r") as f:
    content = f.read()

content = content.replace(
"""                                    IconButton(
                                        onClick = { viewModel.toggleTrueDarkMode() },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Outlined.Contrast, contentDescription = "Black / White", tint = readerOnSurfaceVariantColor, modifier = Modifier.size(20.dp))
                                    }""",
"""                                    IconButton(
                                        onClick = { viewModel.toggleTrueDarkMode() },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Outlined.Contrast, contentDescription = "Black / White", tint = readerOnSurfaceVariantColor, modifier = Modifier.size(20.dp))
                                    }
                                    IconButton(
                                        onClick = { isBionicMode = !isBionicMode },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Bolt, contentDescription = "Bionic Reading", tint = if (isBionicMode) Color(0xFFFFB300) else readerOnSurfaceVariantColor, modifier = Modifier.size(20.dp))
                                    }""")

with open("app/src/main/java/com/silentpdf/app/ui/screens/ReaderScreen.kt", "w") as f:
    f.write(content)
