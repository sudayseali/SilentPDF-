with open("app/src/main/java/com/silentpdf/app/ui/screens/ReaderScreen.kt", "r") as f:
    content = f.read()

target = """                        // Premium Floating Top Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            readerBgColor.copy(alpha = 0.95f),
                                            readerBgColor.copy(alpha = 0.8f),
                                            Color.Transparent
                                        )
                                    )
                                )
                                .padding(top = 40.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(readerSurfaceColor.copy(alpha = 0.85f))
                                    .border(1.dp, readerBorderColor, RoundedCornerShape(20.dp))
                                    .padding(horizontal = 8.dp, vertical = 8.dp),"""

replacement = """                        // Premium Floating Top Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            readerBgColor.copy(alpha = 0.95f),
                                            readerBgColor.copy(alpha = 0.8f),
                                            Color.Transparent
                                        )
                                    )
                                )
                                .padding(top = 40.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(readerSurfaceColor.copy(alpha = 0.85f))
                                    .border(1.dp, readerBorderColor, RoundedCornerShape(20.dp))
                                    .padding(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),"""

content = content.replace(target, replacement)

target2 = """                                            DropdownMenu(
                                                expanded = showMoreMenu,"""

replacement2 = """                                    }
                                }
                                
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
                                            DropdownMenu(
                                                expanded = showMoreMenu,"""
content = content.replace(target2, replacement2)

with open("app/src/main/java/com/silentpdf/app/ui/screens/ReaderScreen.kt", "w") as f:
    f.write(content)
