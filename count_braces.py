with open("app/src/main/java/com/silentpdf/app/ui/screens/ReaderScreen.kt", "r") as f:
    text = f.read()

count = 0
for i, char in enumerate(text):
    if char == '{': count += 1
    elif char == '}': count -= 1
    if "if (showNoteDialog) {" in text[i:i+30]:
        print(f"Brace level at showNoteDialog: {count}")
        break

print(f"Final brace level: {count}")
