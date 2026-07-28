import re

with open('app/src/main/java/com/silentpdf/app/ui/screens/ReaderScreen.kt', 'r') as f:
    content = f.read()

# Find the start of the `if (showSearchOverlay) {` block
start_idx = content.find('if (showSearchOverlay) {')
if start_idx == -1:
    print("Could not find showSearchOverlay block")
    exit(1)

# Find the end of this block by counting brackets
bracket_count = 0
in_block = False
end_idx = -1

for i in range(start_idx, len(content)):
    if content[i] == '{':
        bracket_count += 1
        in_block = True
    elif content[i] == '}':
        bracket_count -= 1
    
    if in_block and bracket_count == 0:
        end_idx = i
        break

if end_idx == -1:
    print("Could not find end of block")
    exit(1)

# Now we replace the whole block with the new UI.
new_block = """if (showSearchOverlay) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
        ) {
            com.silentpdf.app.ui.components.SearchBarWithNavigation(
                query = searchInPdfQuery,
                onQueryChange = { viewModel.searchInPdf(it) },
                currentMatchIndex = activeSearchMatchIndex,
                totalMatches = searchInPdfResults.size,
                onPrevious = {
                    viewModel.previousSearchMatch()
                    if (searchInPdfResults.isNotEmpty()) {
                        viewModel.jumpToPage(searchInPdfResults[viewModel.activeSearchMatchIndex.value].pageNumber)
                    }
                },
                onNext = {
                    viewModel.nextSearchMatch()
                    if (searchInPdfResults.isNotEmpty()) {
                        viewModel.jumpToPage(searchInPdfResults[viewModel.activeSearchMatchIndex.value].pageNumber)
                    }
                },
                onClose = {
                    showSearchOverlay = false
                    viewModel.searchInPdf("")
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
            )
        }
    }"""

new_content = content[:start_idx] + new_block + content[end_idx+1:]

with open('app/src/main/java/com/silentpdf/app/ui/screens/ReaderScreen.kt', 'w') as f:
    f.write(new_content)

print("Patched successfully")
