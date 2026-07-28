package com.silentpdf.app.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * 2. Search Logic & Match finding function
 */
data class TextMatch(
    val startIndex: Int,
    val endIndex: Int
)

fun findSearchMatches(text: String, query: String): List<TextMatch> {
    if (query.isBlank() || text.isBlank()) return emptyList()
    
    val matches = mutableListOf<TextMatch>()
    val lowerText = text.lowercase()
    val lowerQuery = query.lowercase()
    
    var index = 0
    while (index < lowerText.length) {
        val found = lowerText.indexOf(lowerQuery, index)
        if (found != -1) {
            matches.add(TextMatch(startIndex = found, endIndex = found + query.length))
            index = found + query.length
        } else {
            break
        }
    }
    return matches
}

/**
 * 1. Full composable for SearchBar
 */
@Composable
fun SearchBarWithNavigation(
    query: String,
    onQueryChange: (String) -> Unit,
    currentMatchIndex: Int,
    totalMatches: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                placeholder = { Text("Search text...", fontSize = 16.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
            )
            
            if (query.isNotEmpty()) {
                if (totalMatches > 0) {
                    Text(
                        text = "${currentMatchIndex + 1} / $totalMatches",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                } else {
                    Text(
                        text = "0 / 0",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }

                IconButton(onClick = onPrevious) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous Match",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                IconButton(onClick = onNext) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next Match",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            IconButton(onClick = {
                onQueryChange("")
                onClose()
            }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 3. HighlightedText composable
 * Highlighting is rendered ON TOP (via span backgrounds).
 */
@Composable
fun HighlightedText(
    text: String,
    matches: List<TextMatch>,
    currentMatchIndex: Int,
    modifier: Modifier = Modifier,
    activeHighlightColor: Color = Color(0xFFFF9800), // Strong orange for active
    inactiveHighlightColor: Color = Color(0x66FFEB3B), // Lighter yellow for inactive
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val annotatedString = buildAnnotatedString {
        var lastIndex = 0
        
        matches.forEachIndexed { index, match ->
            // Append normal text before the match
            if (match.startIndex > lastIndex) {
                append(text.substring(lastIndex, match.startIndex))
            }
            
            // Append the highlighted text
            val isCurrent = index == currentMatchIndex
            val bgColor = if (isCurrent) activeHighlightColor else inactiveHighlightColor
            val fgColor = if (isCurrent) Color.Black else textColor
            
            withStyle(style = SpanStyle(background = bgColor, color = fgColor, fontWeight = if (isCurrent) FontWeight.Bold else null)) {
                append(text.substring(match.startIndex, match.endIndex))
            }
            
            lastIndex = match.endIndex
        }
        
        // Append remaining text
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }

    Text(
        text = annotatedString,
        modifier = modifier,
        color = textColor
    )
}

/**
 * 4. Scroll-to-match logic
 * Use this wrapper when rendering scrollable text to automatically scroll to the active match.
 * Example usage: inside a LazyColumn or Scrollable column, you can trigger a scroll when currentMatchIndex changes.
 */
@Composable
fun SearchableTextScreenExample(
    fullText: String
) {
    var query by remember { mutableStateOf("") }
    var currentMatchIndex by remember { mutableStateOf(0) }
    
    // Find matches reactively
    val matches by remember(fullText, query) {
        derivedStateOf { findSearchMatches(fullText, query) }
    }
    
    // Reset index when query changes
    LaunchedEffect(query) {
        currentMatchIndex = 0
    }
    
    val scrollState = androidx.compose.foundation.rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    
    Column(modifier = Modifier.fillMaxSize()) {
        SearchBarWithNavigation(
            query = query,
            onQueryChange = { query = it },
            currentMatchIndex = currentMatchIndex,
            totalMatches = matches.size,
            onPrevious = {
                if (matches.isNotEmpty()) {
                    currentMatchIndex = if (currentMatchIndex > 0) currentMatchIndex - 1 else matches.size - 1
                }
            },
            onNext = {
                if (matches.isNotEmpty()) {
                    currentMatchIndex = if (currentMatchIndex < matches.size - 1) currentMatchIndex + 1 else 0
                }
            },
            onClose = { query = "" }
        )
        
        // Ensure scrolling updates when match index changes (approximation for ScrollState)
        // In a real LazyColumn, use listState.animateScrollToItem(currentMatchIndex)
        LaunchedEffect(currentMatchIndex, matches) {
            if (matches.isNotEmpty() && currentMatchIndex < matches.size) {
                val match = matches[currentMatchIndex]
                // Very basic heuristic for scroll position in a simple ScrollState text view
                val estimatedY = (match.startIndex.toFloat() / fullText.length.toFloat()) * scrollState.maxValue
                coroutineScope.launch {
                    scrollState.animateScrollTo(estimatedY.toInt())
                }
            }
        }
        
        Box(modifier = Modifier
            .weight(1f)
            .verticalScroll(scrollState)
            .padding(16.dp)) {
            HighlightedText(
                text = fullText,
                matches = matches,
                currentMatchIndex = currentMatchIndex
            )
        }
    }
}
