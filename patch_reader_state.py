with open("app/src/main/java/com/silentpdf/app/ui/screens/ReaderScreen.kt", "r") as f:
    content = f.read()

content = content.replace("var showOcrDialog by remember { mutableStateOf(false) }", "var showOcrDialog by remember { mutableStateOf(false) }\n    var isBionicMode by remember { mutableStateOf(false) }")
content = content.replace("val pageDrawings by viewModel.pageDrawings.collectAsState()", "val pageDrawings by viewModel.pageDrawings.collectAsState()\n    val openedPdfTextPages by viewModel.openedPdfTextPages.collectAsState()")

with open("app/src/main/java/com/silentpdf/app/ui/screens/ReaderScreen.kt", "w") as f:
    f.write(content)
