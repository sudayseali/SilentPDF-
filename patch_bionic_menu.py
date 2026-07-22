import re
with open("app/src/main/java/com/silentpdf/app/ui/screens/ReaderScreen.kt", "r") as f:
    content = f.read()

target = """                                            DropdownMenuItem(
                                                text = { Text(if (isBionicMode) "Exit Bionic Mode" else "Bionic Reading", color = readerOnSurfaceColor) },
                                                leadingIcon = { Icon(Icons.Default.Bolt, null, tint = Color(0xFFFFB300)) },
                                                onClick = {
                                                    isBionicMode = !isBionicMode
                                                    showMoreMenu = false
                                                }
                                            )"""

content = content.replace(target, "")

with open("app/src/main/java/com/silentpdf/app/ui/screens/ReaderScreen.kt", "w") as f:
    f.write(content)
