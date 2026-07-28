sed -i '1789,1888c\
        Box(\
            modifier = Modifier\
                .fillMaxSize()\
                .background(Color.Transparent)\
        ) {\
            com.silentpdf.app.ui.components.SearchBarWithNavigation(\
                query = searchInPdfQuery,\
                onQueryChange = { viewModel.searchInPdf(it) },\
                currentMatchIndex = activeSearchMatchIndex,\
                totalMatches = searchInPdfResults.size,\
                onPrevious = {\
                    viewModel.previousSearchMatch()\
                    if (searchInPdfResults.isNotEmpty()) {\
                        viewModel.jumpToPage(searchInPdfResults[viewModel.activeSearchMatchIndex.value].pageNumber)\
                    }\
                },\
                onNext = {\
                    viewModel.nextSearchMatch()\
                    if (searchInPdfResults.isNotEmpty()) {\
                        viewModel.jumpToPage(searchInPdfResults[viewModel.activeSearchMatchIndex.value].pageNumber)\
                    }\
                },\
                onClose = {\
                    showSearchOverlay = false\
                    viewModel.searchInPdf("")\
                },\
                modifier = Modifier\
                    .align(Alignment.TopCenter)\
                    .statusBarsPadding()\
            )\
        }\
' app/src/main/java/com/silentpdf/app/ui/screens/ReaderScreen.kt
