with open("app/src/main/java/com/silentpdf/app/ui/screens/ToolsScreen.kt", "r") as f:
    content = f.read()

# Fix convertPdfToLongImage
content = content.replace("suspend fun convertPdfToLongImage(context: Context, pdf: PdfEntity): java.io.File? = withContext(Dispatchers.IO) {\n    try {\n        val contentResolver = context.contentResolver\n        val uri = Uri.parse(pdf.uriString)\n        val pfd = contentResolver.openFileDescriptor(uri, \"r\") ?: return@withContext false", "suspend fun convertPdfToLongImage(context: Context, pdf: PdfEntity): java.io.File? = withContext(Dispatchers.IO) {\n    try {\n        val contentResolver = context.contentResolver\n        val uri = Uri.parse(pdf.uriString)\n        val pfd = contentResolver.openFileDescriptor(uri, \"r\") ?: return@withContext null")

content = content.replace("        return@withContext outFile\n    } catch (e: Exception) {\n        e.printStackTrace()\n        return@withContext false\n    }\n}", "        return@withContext outFile\n    } catch (e: Exception) {\n        e.printStackTrace()\n        return@withContext null\n    }\n}")

with open("app/src/main/java/com/silentpdf/app/ui/screens/ToolsScreen.kt", "w") as f:
    f.write(content)
