with open("app/src/main/java/com/silentpdf/app/ui/viewmodel/SilentPdfViewModel.kt", "r") as f:
    content = f.read()

content = content.replace("private val _openedPdfTextPages = MutableStateFlow<List<String>>(emptyList())", "private val _openedPdfTextPages = MutableStateFlow<List<String>>(emptyList())\n    val openedPdfTextPages: StateFlow<List<String>> = _openedPdfTextPages")

with open("app/src/main/java/com/silentpdf/app/ui/viewmodel/SilentPdfViewModel.kt", "w") as f:
    f.write(content)
