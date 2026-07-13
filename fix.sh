sed -i 's/fun formatFileSize    if (bytes <= 0) return "0 B"/fun formatFileSize(bytes: Long): String {\n    if (bytes <= 0) return "0 B"/' app/src/main/java/com/example/ui/screens/LibraryScreen.kt
