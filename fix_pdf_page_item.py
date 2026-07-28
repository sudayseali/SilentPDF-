import re

with open("app/src/main/java/com/silentpdf/app/ui/screens/PdfPageItem.kt", "r") as f:
    content = f.read()

# I see in the current file:
# strokes.forEach(drawStroke)
# currentStroke?.let(drawStroke)
# }
# }
# } else {
# We need to remove the broken } } and // Draw search highlights stuff.

# Let's just fix it automatically.
content = content.replace("strokes.forEach(drawStroke)\n                    currentStroke?.let(drawStroke)\n                    // Draw search highlights\n                    searchInPdfResults.forEachIndexed { index, result ->\n                    }\n                }\n            } else {", "strokes.forEach(drawStroke)\n                    currentStroke?.let(drawStroke)\n                }\n            } else {")

content = content.replace("strokes.forEach(drawStroke)\n                    currentStroke?.let(drawStroke)\n                    }\n                }\n            } else {", "strokes.forEach(drawStroke)\n                    currentStroke?.let(drawStroke)\n                }\n            } else {")

with open("app/src/main/java/com/silentpdf/app/ui/screens/PdfPageItem.kt", "w") as f:
    f.write(content)
