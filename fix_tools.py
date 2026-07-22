with open("app/src/main/java/com/silentpdf/app/ui/screens/ToolsScreen.kt", "r") as f:
    content = f.read()

content = content.replace("return@withContext outFile\n    } catch", "return@withContext outFile\n    } catch")
content = content.replace("return@withContext false\n    }", "return@withContext null\n    }")

with open("app/src/main/java/com/silentpdf/app/ui/screens/ToolsScreen.kt", "w") as f:
    f.write(content)
