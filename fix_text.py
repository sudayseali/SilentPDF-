with open("app/src/main/java/com/silentpdf/app/ui/screens/ReaderScreen.kt", "r") as f:
    lines = f.readlines()

new_lines = []
skip = False
for line in lines:
    if "Text(" in line and "text = result.snippet" in "".join(lines):
        # We need to precisely delete that specific Text call
        pass

# Actually, let's just use sed to delete the broken Text block
