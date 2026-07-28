import re

with open("app/src/main/java/com/silentpdf/app/ui/screens/ManagePagesScreen.kt", "r") as f:
    content = f.read()

old_str = """        bottomBar = {
            BottomAppBar(
                modifier = Modifier.navigationBarsPadding().padding(bottom = 16.dp),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {"""

new_str = """        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                windowInsets = WindowInsets.navigationBars
            ) {"""

content = content.replace(old_str, new_str)

with open("app/src/main/java/com/silentpdf/app/ui/screens/ManagePagesScreen.kt", "w") as f:
    f.write(content)
