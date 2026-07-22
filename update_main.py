with open("app/src/main/java/com/silentpdf/app/MainActivity.kt", "r") as f:
    content = f.read()

content = content.replace(
    'val isTrueDarkMode by viewModel.isTrueDarkMode.collectAsState()\n            MyApplicationTheme(darkTheme = isTrueDarkMode) {',
    'val isAppDarkMode by viewModel.isAppDarkMode.collectAsState()\n            MyApplicationTheme(darkTheme = isAppDarkMode) {'
)

with open("app/src/main/java/com/silentpdf/app/MainActivity.kt", "w") as f:
    f.write(content)
