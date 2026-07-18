#!/bin/bash

# Define strings
SEARCH_STATE="var showSettingsSheet by remember { mutableStateOf(false) }"
INSERT_STATE="    var showSettingsSheet by remember { mutableStateOf(false) }\n    var showCreateSheet by remember { mutableStateOf(false) }\n    var showTextToPdfDialog by remember { mutableStateOf(false) }"

sed -i "s/$SEARCH_STATE/$INSERT_STATE/g" app/src/main/java/com/silentpdf/app/ui/screens/LibraryScreen.kt

# Add image picker launcher
SEARCH_LAUNCHER="val filePickerLauncher = rememberLauncherForActivityResult"
INSERT_LAUNCHER="val imagePickerLauncher = rememberLauncherForActivityResult(\n        contract = ActivityResultContracts.GetMultipleContents(),\n        onResult = { uris ->\n            if (uris.isNotEmpty()) {\n                kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {\n                    val pdfUri = com.silentpdf.app.util.PdfCreator.createImagesPdf(context, uris, \"Scanned_\${System.currentTimeMillis()}\")\n                    if (pdfUri != null) {\n                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {\n                            android.widget.Toast.makeText(context, \"PDF created successfully!\", android.widget.Toast.LENGTH_SHORT).show()\n                            val file = java.io.File(pdfUri.path!!)\n                            viewModel.importPdf(pdfUri, file.name, file.length())\n                        }\n                    }\n                }\n            }\n        }\n    )\n\n    val filePickerLauncher = rememberLauncherForActivityResult"

sed -i "s/$SEARCH_LAUNCHER/$INSERT_LAUNCHER/g" app/src/main/java/com/silentpdf/app/ui/screens/LibraryScreen.kt

# Change ExtendedFloatingActionButton
SEARCH_FAB="            ExtendedFloatingActionButton(\n                onClick = { filePickerLauncher.launch(arrayOf(\"application\/pdf\")) },\n                containerColor = Color(0xFF2F80ED),\n                contentColor = Color.White,\n                shape = CircleShape,\n                icon = { Icon(Icons.Default.Add, \"Import PDF\", modifier = Modifier.size(24.dp)) },\n                text = { Text(\"Import PDF\", fontWeight = FontWeight.Black) }\n            )"
INSERT_FAB="            ExtendedFloatingActionButton(\n                onClick = { showCreateSheet = true },\n                containerColor = Color(0xFF2F80ED),\n                contentColor = Color.White,\n                shape = CircleShape,\n                icon = { Icon(Icons.Default.Add, \"Create\/Import\", modifier = Modifier.size(24.dp)) },\n                text = { Text(\"Add\", fontWeight = FontWeight.Black) }\n            )"

# Use perl to multiline replace FAB
perl -0777 -pi -e 's/ExtendedFloatingActionButton\(\s*onClick = \{ filePickerLauncher\.launch\(arrayOf\("application\/pdf"\)\) \},\s*containerColor = Color\(0xFF2F80ED\),\s*contentColor = Color\.White,\s*shape = CircleShape,\s*icon = \{ Icon\(Icons\.Default\.Add, "Import PDF", modifier = Modifier\.size\(24\.dp\)\) \},\s*text = \{ Text\("Import PDF", fontWeight = FontWeight\.Black\) \}\s*\)/ExtendedFloatingActionButton(\n                onClick = { showCreateSheet = true },\n                containerColor = Color(0xFF2F80ED),\n                contentColor = Color.White,\n                shape = CircleShape,\n                icon = { Icon(Icons.Default.Add, "Create PDF", modifier = Modifier.size(24.dp)) },\n                text = { Text("Add", fontWeight = FontWeight.Black) }\n            )/g' app/src/main/java/com/silentpdf/app/ui/screens/LibraryScreen.kt

# Insert Dialog calls at the end of LibraryScreen content
perl -0777 -pi -e 's/    if \(showSettingsSheet\) \{/    if (showCreateSheet) {\n        CreateOptionsSheet(\n            onDismiss = { showCreateSheet = false },\n            onImportPdf = { filePickerLauncher.launch(arrayOf("application\/pdf")) },\n            onImagesToPdfClick = { imagePickerLauncher.launch("image\/*") },\n            onTextToPdfClick = { showTextToPdfDialog = true }\n        )\n    }\n\n    if (showTextToPdfDialog) {\n        TextToPdfDialog(\n            onDismiss = { showTextToPdfDialog = false },\n            onPdfCreated = { uri ->\n                val file = java.io.File(uri.path!!)\n                viewModel.importPdf(uri, file.name, file.length())\n            }\n        )\n    }\n\n    if (showSettingsSheet) {/g' app/src/main/java/com/silentpdf/app/ui/screens/LibraryScreen.kt

