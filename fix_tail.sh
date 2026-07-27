head -n 735 app/src/main/java/com/silentpdf/app/ui/viewmodel/SilentPdfViewModel.kt > temp_vm.kt
cat << 'INNER_EOF' >> temp_vm.kt

    suspend fun rotatePages(pdf: PdfEntity, pageIndices: List<Int>): Boolean {
        val app = getApplication<android.app.Application>()
        val destFile = java.io.File(app.cacheDir, "temp_rotated.pdf")
        val success = com.silentpdf.app.util.PdfPageManager.rotatePages(app, android.net.Uri.parse(pdf.uriString), pageIndices, destFile)
        if (success) {
            return replacePdfWithTemp(pdf, destFile)
        }
        return false
    }

    suspend fun deletePages(pdf: PdfEntity, pageIndices: List<Int>): Boolean {
        val app = getApplication<android.app.Application>()
        val destFile = java.io.File(app.cacheDir, "temp_deleted.pdf")
        val success = com.silentpdf.app.util.PdfPageManager.deletePages(app, android.net.Uri.parse(pdf.uriString), pageIndices, destFile)
        if (success) {
            return replacePdfWithTemp(pdf, destFile)
        }
        return false
    }

    suspend fun insertBlankPage(pdf: PdfEntity, afterPageIndex: Int): Boolean {
        val app = getApplication<android.app.Application>()
        val destFile = java.io.File(app.cacheDir, "temp_inserted.pdf")
        val success = com.silentpdf.app.util.PdfPageManager.insertBlankPage(app, android.net.Uri.parse(pdf.uriString), afterPageIndex, destFile)
        if (success) {
            return replacePdfWithTemp(pdf, destFile)
        }
        return false
    }

    suspend fun extractPages(pdf: PdfEntity, pageIndices: List<Int>): Boolean {
        val app = getApplication<android.app.Application>()
        val destFile = java.io.File(app.filesDir, "extracted_${System.currentTimeMillis()}.pdf")
        val success = com.silentpdf.app.util.PdfPageManager.extractPages(app, android.net.Uri.parse(pdf.uriString), pageIndices, destFile)
        if (success) {
            val extractedPdf = PdfEntity(
                fileName = "${pdf.fileName.substringBeforeLast(".")}_extracted.pdf",
                uriString = android.net.Uri.fromFile(destFile).toString(),
                fileSize = destFile.length(),
                lastAccessTime = System.currentTimeMillis()
            )
            repository.insertPdf(extractedPdf)
            return true
        }
        return false
    }

    private suspend fun replacePdfWithTemp(pdf: PdfEntity, tempFile: java.io.File): Boolean {
        val app = getApplication<android.app.Application>()
        return try {
            val originalUri = android.net.Uri.parse(pdf.uriString)
            if (originalUri.scheme == "file") {
                val originalFile = java.io.File(originalUri.path!!)
                tempFile.copyTo(originalFile, overwrite = true)
            } else {
                app.contentResolver.openOutputStream(originalUri)?.use { out ->
                    java.io.FileInputStream(tempFile).use { input ->
                        input.copyTo(out)
                    }
                }
            }
            // Update file size in DB
            val updatedSize = if (originalUri.scheme == "file") {
                java.io.File(originalUri.path!!).length()
            } else {
                app.contentResolver.openFileDescriptor(originalUri, "r")?.statSize ?: pdf.fileSize
            }
            repository.updatePdf(pdf.copy(fileSize = updatedSize))
            
            // Reload the PDF engine
            renderEngine.openDocument(originalUri)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Run synchronously to ensure it closes before the process dies
        renderEngine.closeDocument()
        try {
            mediaRecorder?.release()
        } catch (e: Exception) {}
    }
}
INNER_EOF
mv temp_vm.kt app/src/main/java/com/silentpdf/app/ui/viewmodel/SilentPdfViewModel.kt
