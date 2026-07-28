with open("app/src/main/java/com/silentpdf/app/ui/screens/ReaderScreen.kt", "r") as f:
    content = f.read()

# Replace the specific marker
content = content.replace("@Composable\nfun VoiceNotePlayer", "}\n\n@Composable\nfun VoiceNotePlayer")

with open("app/src/main/java/com/silentpdf/app/ui/screens/ReaderScreen.kt", "w") as f:
    f.write(content)
