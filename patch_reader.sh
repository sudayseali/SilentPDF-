sed -i '/Text(/,/textAlign = TextAlign.Center/c\
                            var showJumpDialog by remember { mutableStateOf(false) }\
                            Text(\
                                text = "Page ${currentPage + 1} of $pageCount",\
                                fontSize = 14.sp,\
                                fontWeight = FontWeight.Bold,\
                                color = MaterialTheme.colorScheme.onSurface,\
                                textAlign = TextAlign.Center,\
                                modifier = Modifier\
                                    .clip(RoundedCornerShape(8.dp))\
                                    .clickable { showJumpDialog = true }\
                                    .padding(8.dp)\
                            )\
                            if (showJumpDialog) {\
                                var jumpText by remember { mutableStateOf("") }\
                                AlertDialog(\
                                    onDismissRequest = { showJumpDialog = false },\
                                    title = { Text("Go to Page") },\
                                    text = {\
                                        OutlinedTextField(\
                                            value = jumpText,\
                                            onValueChange = { jumpText = it.filter { char -> char.isDigit() } },\
                                            label = { Text("Page Number (1 - $pageCount)") },\
                                            singleLine = true\
                                        )\
                                    },\
                                    confirmButton = {\
                                        TextButton(\
                                            onClick = {\
                                                val page = jumpText.toIntOrNull()\
                                                if (page != null && page in 1..pageCount) {\
                                                    viewModel.jumpToPage(page - 1, viewWidth)\
                                                }\
                                                showJumpDialog = false\
                                            }\
                                        ) {\
                                            Text("Go")\
                                        }\
                                    },\
                                    dismissButton = {\
                                        TextButton(onClick = { showJumpDialog = false }) {\
                                            Text("Cancel")\
                                        }\
                                    }\
                                )\
                            }' app/src/main/java/com/example/ui/screens/ReaderScreen.kt
