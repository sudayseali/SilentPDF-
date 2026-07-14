sed -i '/var pdfToMoveToFolder/a \    var pdfToDelete by remember { mutableStateOf<PdfEntity?>(null) }' app/src/main/java/com/example/ui/screens/LibraryScreen.kt

sed -i 's/onDelete = { viewModel.deletePdf(pdf) }/onDelete = { pdfToDelete = pdf }/g' app/src/main/java/com/example/ui/screens/LibraryScreen.kt

cat << 'INNER_EOF' > delete_dialog.txt

    // DELETE CONFIRMATION DIALOG
    if (pdfToDelete != null) {
        val pdf = pdfToDelete!!
        AlertDialog(
            onDismissRequest = { pdfToDelete = null },
            title = { Text("Delete PDF", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${pdf.fileName}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePdf(pdf)
                        pdfToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("OK", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { pdfToDelete = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
INNER_EOF

sed -i '/\/\/ SORTING DIALOG/r delete_dialog.txt' app/src/main/java/com/example/ui/screens/LibraryScreen.kt
