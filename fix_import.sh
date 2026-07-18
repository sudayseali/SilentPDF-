#!/bin/bash
sed -i 's/import kotlinx.coroutines.withContext/import kotlinx.coroutines.withContext\nimport kotlinx.coroutines.launch\nimport kotlinx.coroutines.CoroutineScope/g' app/src/main/java/com/silentpdf/app/ui/screens/LibraryScreen.kt
