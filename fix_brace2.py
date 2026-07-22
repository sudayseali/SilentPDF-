with open("app/src/main/java/com/silentpdf/app/ui/screens/ReaderScreen.kt", "r") as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if "} else if (pageCount > 0) {" in line:
        # Check lines before
        print(f"Line {i}: {line.strip()}")
        for j in range(i-5, i+2):
            print(f"{j}: {lines[j].rstrip()}")
        break
