#!/bin/bash
sed -i 's/kotlinx.coroutines.GlobalScope.launch/kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch/g' app/src/main/java/com/silentpdf/app/ui/screens/LibraryScreen.kt
