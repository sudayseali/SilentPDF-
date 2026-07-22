with open("app/src/main/java/com/silentpdf/app/ui/screens/ToolsScreen.kt", "r") as f:
    content = f.read()

content = content.replace("suspend fun convertPdfToImages(context: Context, pdf: PdfEntity): Boolean = withContext(Dispatchers.IO) {\n    try {\n        val contentResolver = context.contentResolver\n        val uri = Uri.parse(pdf.uriString)\n        val pfd = contentResolver.openFileDescriptor(uri, \"r\") ?: return@withContext null", "suspend fun convertPdfToImages(context: Context, pdf: PdfEntity): Boolean = withContext(Dispatchers.IO) {\n    try {\n        val contentResolver = context.contentResolver\n        val uri = Uri.parse(pdf.uriString)\n        val pfd = contentResolver.openFileDescriptor(uri, \"r\") ?: return@withContext false")

content = content.replace("        return@withContext true\n    } catch (e: Exception) {\n        e.printStackTrace()\n        return@withContext null\n    }\n}\n\nsuspend fun convertPdfToLongImage", "        return@withContext true\n    } catch (e: Exception) {\n        e.printStackTrace()\n        return@withContext false\n    }\n}\n\nsuspend fun convertPdfToLongImage")

with open("app/src/main/java/com/silentpdf/app/ui/screens/ToolsScreen.kt", "w") as f:
    f.write(content)
