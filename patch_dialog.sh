sed -i '1270,1280c\
                    if (showManagePages && currentPdf != null) {\
                        androidx.compose.ui.window.Dialog(\
                            onDismissRequest = { showManagePages = false },\
                            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)\
                        ) {\
                            ManagePagesScreen(\
                                pdf = currentPdf!!,\
                                viewModel = viewModel,\
                                onClose = { showManagePages = false },\
                                onPagesChanged = { \
                                    viewModel.openPdf(currentPdf!!)\
                                }\
                            )\
                        }\
                    }\
' app/src/main/java/com/silentpdf/app/ui/screens/ReaderScreen.kt
