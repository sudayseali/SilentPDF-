with open("app/src/main/java/com/silentpdf/app/ui/screens/ReaderScreen.kt", "r") as f:
    text = f.read()

# Revert the bad replacement
text = text.replace("                                    }\n                                }\n                            }\n                        }\n                    }\n                    }", "                                    }\n                                }\n                            }\n                        }\n                    }")

with open("app/src/main/java/com/silentpdf/app/ui/screens/ReaderScreen.kt", "w") as f:
    f.write(text)
