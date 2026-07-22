with open("app/src/main/java/com/silentpdf/app/ui/screens/ToolsScreen.kt", "r") as f:
    content = f.read()

replacement = """                    ActiveTool.PdfToLongImage -> {
                        scope.launch {
                            val file = convertPdfToLongImage(context, pdf)
                            if (file != null) {
                                Toast.makeText(context, "Long Image created!", Toast.LENGTH_SHORT).show()
                                val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "image/jpeg"
                                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Long Image"))
                            } else {
                                Toast.makeText(context, "Failed to convert to long image", Toast.LENGTH_SHORT).show()
                            }
                            activeTool = ActiveTool.None
                        }
                    }"""

content = content.replace("""                    ActiveTool.PdfToLongImage -> {
                        scope.launch {
                            val success = convertPdfToLongImage(context, pdf)
                            if (success) {
                                Toast.makeText(context, "Long Image saved to gallery!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Failed to convert to long image", Toast.LENGTH_SHORT).show()
                            }
                            activeTool = ActiveTool.None
                        }
                    }""", replacement)

with open("app/src/main/java/com/silentpdf/app/ui/screens/ToolsScreen.kt", "w") as f:
    f.write(content)
