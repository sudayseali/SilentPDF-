sed -i 's/} else {/} else if (pageBitmap != null) {\nval bitmap = pageBitmap!!/g' app/src/main/java/com/example/ui/screens/ReaderScreen.kt
sed -i 's/pageBitmap?.let { bitmap ->//g' app/src/main/java/com/example/ui/screens/ReaderScreen.kt
sed -i 's/} ?: run {/} else if (pageCount > 0) {/g' app/src/main/java/com/example/ui/screens/ReaderScreen.kt
