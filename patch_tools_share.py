with open("app/src/main/java/com/silentpdf/app/ui/screens/ToolsScreen.kt", "r") as f:
    content = f.read()

content = content.replace("suspend fun convertPdfToLongImage(context: Context, pdf: PdfEntity): Boolean = withContext(Dispatchers.IO) {", "suspend fun convertPdfToLongImage(context: Context, pdf: PdfEntity): java.io.File? = withContext(Dispatchers.IO) {")
content = content.replace("return@withContext true\n    } catch", "return@withContext outFile\n    } catch")
content = content.replace("return@withContext false\n    }", "return@withContext null\n    }")

with open("app/src/main/java/com/silentpdf/app/ui/screens/ToolsScreen.kt", "w") as f:
    f.write(content)
