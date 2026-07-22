with open("app/src/main/java/com/silentpdf/app/ui/screens/ToolsScreen.kt", "r") as f:
    content = f.read()

content = content.replace("        renderer.close()\n        pfd.close()\n        return@withContext outFile\n    } catch (e: Exception) {\n        e.printStackTrace()\n        return@withContext null\n    }\n}\n\nsuspend fun convertPdfToLongImage", "        renderer.close()\n        pfd.close()\n        return@withContext true\n    } catch (e: Exception) {\n        e.printStackTrace()\n        return@withContext false\n    }\n}\n\nsuspend fun convertPdfToLongImage")

with open("app/src/main/java/com/silentpdf/app/ui/screens/ToolsScreen.kt", "w") as f:
    f.write(content)
