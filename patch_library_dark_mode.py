with open("app/src/main/java/com/silentpdf/app/ui/screens/LibraryScreen.kt", "r") as f:
    content = f.read()

content = content.replace(
    'val isTrueDarkMode by viewModel.isTrueDarkMode.collectAsState()',
    'val isTrueDarkMode by viewModel.isTrueDarkMode.collectAsState()\n    val isAppDarkMode by viewModel.isAppDarkMode.collectAsState()'
)

content = content.replace(
    """                ListItem(
                    headlineContent = { Text("True Dark Mode", color = MaterialTheme.colorScheme.onSurface) },
                    supportingContent = { Text(if (isTrueDarkMode) "Active (OLED Black)" else "Inactive (Light Mode)", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
                    leadingContent = { Icon(Icons.Outlined.Contrast, null, tint = Color(0xFF2F80ED)) },
                    trailingContent = {
                        Switch(
                            checked = isTrueDarkMode,
                            onCheckedChange = { viewModel.toggleTrueDarkMode() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF2F80ED),
                                checkedTrackColor = Color(0xFF2F80ED).copy(alpha = 0.4f)
                            )
                        )
                    },
                    modifier = Modifier.clickable { viewModel.toggleTrueDarkMode() },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )""",
    """                ListItem(
                    headlineContent = { Text("App Dark Mode", color = MaterialTheme.colorScheme.onSurface) },
                    supportingContent = { Text(if (isAppDarkMode) "Active (Dark UI)" else "Inactive (Light UI)", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
                    leadingContent = { Icon(Icons.Outlined.DarkMode, null, tint = Color(0xFF2F80ED)) },
                    trailingContent = {
                        Switch(
                            checked = isAppDarkMode,
                            onCheckedChange = { viewModel.toggleAppDarkMode() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF2F80ED),
                                checkedTrackColor = Color(0xFF2F80ED).copy(alpha = 0.4f)
                            )
                        )
                    },
                    modifier = Modifier.clickable { viewModel.toggleAppDarkMode() },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                ListItem(
                    headlineContent = { Text("PDF True Dark Mode", color = MaterialTheme.colorScheme.onSurface) },
                    supportingContent = { Text(if (isTrueDarkMode) "Active (OLED Black)" else "Inactive", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
                    leadingContent = { Icon(Icons.Outlined.Contrast, null, tint = Color(0xFF2F80ED)) },
                    trailingContent = {
                        Switch(
                            checked = isTrueDarkMode,
                            onCheckedChange = { viewModel.toggleTrueDarkMode() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF2F80ED),
                                checkedTrackColor = Color(0xFF2F80ED).copy(alpha = 0.4f)
                            )
                        )
                    },
                    modifier = Modifier.clickable { viewModel.toggleTrueDarkMode() },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )"""
)

with open("app/src/main/java/com/silentpdf/app/ui/screens/LibraryScreen.kt", "w") as f:
    f.write(content)
