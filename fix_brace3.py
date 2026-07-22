with open("app/src/main/java/com/silentpdf/app/ui/screens/ReaderScreen.kt", "r") as f:
    text = f.read()

text = text.replace("                    } else if (pageCount > 0) {", "                    }\n                    } else if (pageCount > 0) {")

with open("app/src/main/java/com/silentpdf/app/ui/screens/ReaderScreen.kt", "w") as f:
    f.write(text)
