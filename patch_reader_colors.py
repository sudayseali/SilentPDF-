with open("app/src/main/java/com/silentpdf/app/ui/screens/ReaderScreen.kt", "r") as f:
    content = f.read()

content = content.replace(
    'val isTrueDarkMode by viewModel.isTrueDarkMode.collectAsState()',
    'val isTrueDarkMode by viewModel.isTrueDarkMode.collectAsState()\n    val isAppDarkMode by viewModel.isAppDarkMode.collectAsState()'
)

content = content.replace(
    'val readerBgColor = if (isTrueDarkMode) Color.Black else MaterialTheme.colorScheme.background\n    val readerSurfaceColor = if (isTrueDarkMode) Color(0xFF111422) else MaterialTheme.colorScheme.surface\n    val readerBorderColor = if (isTrueDarkMode) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)\n    val readerOnSurfaceColor = if (isTrueDarkMode) Color(0xFFF1F5F9) else MaterialTheme.colorScheme.onSurface\n    val readerOnSurfaceVariantColor = if (isTrueDarkMode) Color(0xFF94A3B8) else MaterialTheme.colorScheme.onSurfaceVariant\n    val readerButtonBgColor = if (isTrueDarkMode) Color(0xFF1E263D).copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)',
    'val readerBgColor = if (isAppDarkMode) Color.Black else MaterialTheme.colorScheme.background\n    val readerSurfaceColor = if (isAppDarkMode) Color(0xFF111422) else MaterialTheme.colorScheme.surface\n    val readerBorderColor = if (isAppDarkMode) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)\n    val readerOnSurfaceColor = if (isAppDarkMode) Color(0xFFF1F5F9) else MaterialTheme.colorScheme.onSurface\n    val readerOnSurfaceVariantColor = if (isAppDarkMode) Color(0xFF94A3B8) else MaterialTheme.colorScheme.onSurfaceVariant\n    val readerButtonBgColor = if (isAppDarkMode) Color(0xFF1E263D).copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)'
)

content = content.replace(
    '.background(if (isTrueDarkMode) Color.Black else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))',
    '.background(if (isAppDarkMode) Color.Black else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))'
)

with open("app/src/main/java/com/silentpdf/app/ui/screens/ReaderScreen.kt", "w") as f:
    f.write(content)
