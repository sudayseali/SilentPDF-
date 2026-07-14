sed -i '/var showSecurityDialog by remember { mutableStateOf(false) }/a \    var showSupportDialog by remember { mutableStateOf(false) }' app/src/main/java/com/example/ui/screens/LibraryScreen.kt

sed -i '/IconButton(onClick = { showSortMenu = true }) {/i \                    IconButton(onClick = { showSupportDialog = true }) {\n                        Icon(\n                            imageVector = Icons.Outlined.SupportAgent,\n                            contentDescription = "Support",\n                            tint = MaterialTheme.colorScheme.primary\n                        )\n                    }' app/src/main/java/com/example/ui/screens/LibraryScreen.kt

cat << 'INNER_EOF' > support_dialog.txt

    // SUPPORT DIALOG
    if (showSupportDialog) {
        AlertDialog(
            onDismissRequest = { showSupportDialog = false },
            title = { Text("Caawinaad / Support", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Haddii aad la kulanto wax cilad ah, fadlan nagala soo xiriir WhatsApp.")
                    Text(
                        "Fiiro gaar ah: Kaliya fariin (chat) ayaa la ogol yahay. Wicitanka codka iyo muqaalka (Voice/Video calls) lama ogola.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSupportDialog = false
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.data = Uri.parse("https://wa.me/252657864155?text=Asc,%20waxaan%20u%20baahanahay%20caawinaad%20ku%20saabsan%20SilentPDF%20app-ka.")
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "WhatsApp is not installed.", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("WhatsApp Chat")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSupportDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
INNER_EOF

sed -i '/\/\/ SORTING DIALOG/r support_dialog.txt' app/src/main/java/com/example/ui/screens/LibraryScreen.kt
