with open("app/src/main/java/com/silentpdf/app/ui/screens/ReaderScreen.kt", "r") as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if "onDismissRequest = { showNoteDialog = false }," in line:
        idx = i
        break

# idx is the line with onDismissRequest
# We need to replace everything between .statusBarsPadding() ) } } and idx
for i in range(idx - 1, 0, -1):
    if ".statusBarsPadding()" in lines[i]:
        start_idx = i + 3
        break

new_lines = lines[:start_idx] + ["    \n    if (showNoteDialog) {\n        AlertDialog(\n"] + lines[idx:]

with open("app/src/main/java/com/silentpdf/app/ui/screens/ReaderScreen.kt", "w") as f:
    f.writelines(new_lines)
