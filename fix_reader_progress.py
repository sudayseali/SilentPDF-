with open("app/src/main/java/com/silentpdf/app/ui/screens/ReaderScreen.kt", "r") as f:
    lines = f.readlines()

# Find line 926 which is "                },"
for i, line in enumerate(lines):
    if "bottomBar = {" in line and "}," in lines[i-1]:
        print("Found bottomBar at line", i+1)
        
        # We need to replace lines i-4 to i-1
        
        replacement = """                                }
                                if (pageCount > 0) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, bottom = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        androidx.compose.material3.LinearProgressIndicator(
                                            progress = { (currentPage + 1).toFloat() / pageCount.toFloat() },
                                            modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)),
                                            color = Color(0xFF2F80ED),
                                            trackColor = readerOnSurfaceVariantColor.copy(alpha = 0.2f),
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "${currentPage + 1} / $pageCount",
                                            fontSize = 11.sp,
                                            color = readerOnSurfaceVariantColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
"""
        lines[i-4:i] = [replacement]
        break

with open("app/src/main/java/com/silentpdf/app/ui/screens/ReaderScreen.kt", "w") as f:
    f.writelines(lines)

