with open("app/src/main/java/com/silentpdf/app/ui/screens/ToolsScreen.kt", "r") as f:
    content = f.read()

replacement = """        // Other Section
        ToolCategorySection(
            title = "Other",
            tools = listOf(
                ToolItemData("Merge PDF", Icons.Outlined.MergeType, Color(0xFFFF9800), { onToolClick(ActiveTool.Merge) }),
                ToolItemData("Split PDF", Icons.Outlined.CallSplit, Color(0xFF00C853), { onToolClick(ActiveTool.Split) }),
                ToolItemData("Manage pages", Icons.Outlined.Layers, Color(0xFF9C27B0), { onToolClick(ActiveTool.ManagePages) }),
                ToolItemData("Compress", Icons.Outlined.Compress, Color(0xFFFFC107), { onToolClick(ActiveTool.Compress) }),
                ToolItemData("Lock PDF", Icons.Outlined.Lock, Color(0xFFE91E63), { onToolClick(ActiveTool.Lock) }),
                ToolItemData("Unlock PDF", Icons.Outlined.LockOpen, Color(0xFF2196F3), { onToolClick(ActiveTool.Unlock) })
            )
        )

        Spacer(modifier = Modifier.height(32.dp))
        NoWatermarkPromise()
        Spacer(modifier = Modifier.height(100.dp))"""

content = content.replace("""        // Other Section
        ToolCategorySection(
            title = "Other",
            tools = listOf(
                ToolItemData("Merge PDF", Icons.Outlined.MergeType, Color(0xFFFF9800), { onToolClick(ActiveTool.Merge) }),
                ToolItemData("Split PDF", Icons.Outlined.CallSplit, Color(0xFF00C853), { onToolClick(ActiveTool.Split) }),
                ToolItemData("Manage pages", Icons.Outlined.Layers, Color(0xFF9C27B0), { onToolClick(ActiveTool.ManagePages) }),
                ToolItemData("Compress", Icons.Outlined.Compress, Color(0xFFFFC107), { onToolClick(ActiveTool.Compress) }),
                ToolItemData("Lock PDF", Icons.Outlined.Lock, Color(0xFFE91E63), { onToolClick(ActiveTool.Lock) }),
                ToolItemData("Unlock PDF", Icons.Outlined.LockOpen, Color(0xFF2196F3), { onToolClick(ActiveTool.Unlock) })
            )
        )""", replacement)

promise_composable = """
@Composable
fun NoWatermarkPromise() {
    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Outlined.Verified, contentDescription = "Verified", tint = Color(0xFF4CAF50), modifier = Modifier.size(32.dp))
            Text(
                text = "100% Free • No Watermarks • Offline & Private",
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                text = "Everything you create or edit is yours. We never add watermarks.",
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
"""

content = content + promise_composable

with open("app/src/main/java/com/silentpdf/app/ui/screens/ToolsScreen.kt", "w") as f:
    f.write(content)
