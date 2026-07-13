sed -i '/@Composable/,/fun formatFileSize/c\
@Composable\
fun ListPdfItem(\
    pdf: PdfEntity,\
    onClick: () -> Unit,\
    onFavoriteToggle: () -> Unit,\
    onShare: () -> Unit,\
    onDelete: () -> Unit\
) {\
    var showMenu by remember { mutableStateOf(false) }\
    Row(\
        modifier = Modifier\
            .fillMaxWidth()\
            .clip(RoundedCornerShape(16.dp))\
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))\
            .clickable { onClick() }\
            .padding(12.dp),\
        verticalAlignment = Alignment.CenterVertically,\
        horizontalArrangement = Arrangement.spacedBy(12.dp)\
    ) {\
        Box(\
            modifier = Modifier\
                .size(44.dp)\
                .clip(RoundedCornerShape(10.dp))\
                .background(MaterialTheme.colorScheme.surface),\
            contentAlignment = Alignment.Center\
        ) {\
            Text(\
                text = "PDF",\
                fontSize = 10.sp,\
                fontWeight = FontWeight.Bold,\
                color = MaterialTheme.colorScheme.primary\
            )\
        }\
        Column(\
            modifier = Modifier.weight(1f)\
        ) {\
            Text(\
                text = pdf.fileName,\
                fontSize = 14.sp,\
                fontWeight = FontWeight.Bold,\
                maxLines = 1,\
                overflow = TextOverflow.Ellipsis,\
                color = MaterialTheme.colorScheme.onSurface\
            )\
            Text(\
                text = "${formatFileSize(pdf.fileSize)} • ${formatRelativeTime(pdf.lastAccessTime)}",\
                fontSize = 12.sp,\
                color = MaterialTheme.colorScheme.onSurfaceVariant,\
                modifier = Modifier.padding(top = 2.dp)\
            )\
        }\
        IconButton(onClick = onFavoriteToggle) {\
            Icon(\
                imageVector = if (pdf.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,\
                contentDescription = "Favorite Toggle",\
                tint = if (pdf.isFavorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant\
            )\
        }\
        Box {\
            IconButton(onClick = { showMenu = true }) {\
                Icon(\
                    imageVector = Icons.Default.MoreVert,\
                    contentDescription = "More Options",\
                    tint = MaterialTheme.colorScheme.onSurfaceVariant\
                )\
            }\
            DropdownMenu(\
                expanded = showMenu,\
                onDismissRequest = { showMenu = false }\
            ) {\
                DropdownMenuItem(\
                    text = { Text("Share") },\
                    onClick = {\
                        showMenu = false\
                        onShare()\
                    },\
                    leadingIcon = { Icon(Icons.Outlined.Share, null) }\
                )\
                DropdownMenuItem(\
                    text = { Text("Delete") },\
                    onClick = {\
                        showMenu = false\
                        onDelete()\
                    },\
                    leadingIcon = { Icon(Icons.Outlined.Delete, null) }\
                )\
            }\
        }\
    }\
}\
\
@Composable\
fun GridPdfItem(\
    pdf: PdfEntity,\
    onClick: () -> Unit,\
    onFavoriteToggle: () -> Unit,\
    onShare: () -> Unit,\
    onDelete: () -> Unit\
) {\
    var showMenu by remember { mutableStateOf(false) }\
    Card(\
        onClick = onClick,\
        shape = RoundedCornerShape(20.dp),\
        colors = CardDefaults.cardColors(\
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)\
        ),\
        modifier = Modifier.fillMaxWidth()\
    ) {\
        Column(\
            modifier = Modifier\
                .padding(12.dp)\
                .fillMaxWidth()\
        ) {\
            Row(\
                modifier = Modifier.fillMaxWidth(),\
                horizontalArrangement = Arrangement.SpaceBetween,\
                verticalAlignment = Alignment.Top\
            ) {\
                Box(\
                    modifier = Modifier\
                        .size(36.dp, 48.dp)\
                        .clip(RoundedCornerShape(6.dp))\
                        .background(MaterialTheme.colorScheme.surface),\
                    contentAlignment = Alignment.Center\
                ) {\
                    Text(\
                        text = "PDF",\
                        fontSize = 9.sp,\
                        fontWeight = FontWeight.Bold,\
                        color = MaterialTheme.colorScheme.primary\
                    )\
                }\
                Row {\
                    IconButton(\
                        onClick = onFavoriteToggle,\
                        modifier = Modifier.size(24.dp)\
                    ) {\
                        Icon(\
                            imageVector = if (pdf.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,\
                            contentDescription = "Favorite Toggle",\
                            tint = if (pdf.isFavorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant,\
                            modifier = Modifier.size(20.dp)\
                        )\
                    }\
                    Spacer(modifier = Modifier.width(8.dp))\
                    Box {\
                        IconButton(\
                            onClick = { showMenu = true },\
                            modifier = Modifier.size(24.dp)\
                        ) {\
                            Icon(\
                                imageVector = Icons.Default.MoreVert,\
                                contentDescription = "More Options",\
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,\
                                modifier = Modifier.size(20.dp)\
                            )\
                        }\
                        DropdownMenu(\
                            expanded = showMenu,\
                            onDismissRequest = { showMenu = false }\
                        ) {\
                            DropdownMenuItem(\
                                text = { Text("Share") },\
                                onClick = {\
                                    showMenu = false\
                                    onShare()\
                                },\
                                leadingIcon = { Icon(Icons.Outlined.Share, null) }\
                            )\
                            DropdownMenuItem(\
                                text = { Text("Delete") },\
                                onClick = {\
                                    showMenu = false\
                                    onDelete()\
                                },\
                                leadingIcon = { Icon(Icons.Outlined.Delete, null) }\
                            )\
                        }\
                    }\
                }\
            }\
            Spacer(modifier = Modifier.height(12.dp))\
            Text(\
                text = pdf.fileName,\
                fontSize = 13.sp,\
                fontWeight = FontWeight.Bold,\
                maxLines = 2,\
                overflow = TextOverflow.Ellipsis,\
                color = MaterialTheme.colorScheme.onSurface,\
                lineHeight = 16.sp\
            )\
            Spacer(modifier = Modifier.height(4.dp))\
            Text(\
                text = formatFileSize(pdf.fileSize),\
                fontSize = 11.sp,\
                color = MaterialTheme.colorScheme.onSurfaceVariant\
            )\
        }\
    }\
}\
\
fun formatFileSize' app/src/main/java/com/example/ui/screens/LibraryScreen.kt
