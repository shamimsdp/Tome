package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.LineWeight
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ReadingTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingTopBar(
    title: String,
    author: String,
    currentTheme: ReadingTheme,
    isBookmarked: Boolean,
    isHighlightMode: Boolean,
    onBack: () -> Unit,
    onToggleBookmark: () -> Unit,
    onToggleHighlightMode: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenNotesDrawer: () -> Unit,
    onSelectTheme: (ReadingTheme) -> Unit,
    modifier: Modifier = Modifier
) {
    var themeMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 6.dp,
        shadowElevation = 4.dp
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = author,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.testTag("reader_back_button")) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Library")
                }
            },
            actions = {
                // Search in PDF
                IconButton(onClick = onOpenSearch, modifier = Modifier.testTag("reader_search_button")) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search text")
                }

                // Highlighter mode toggle
                IconButton(
                    onClick = onToggleHighlightMode,
                    modifier = Modifier.testTag("reader_highlight_toggle")
                ) {
                    Icon(
                        imageVector = Icons.Default.Brush,
                        contentDescription = "Toggle Highlighter",
                        tint = if (isHighlightMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }

                // Bookmark toggle
                IconButton(onClick = onToggleBookmark, modifier = Modifier.testTag("reader_bookmark_toggle")) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark Page",
                        tint = if (isBookmarked) Color(0xFFE11D48) else MaterialTheme.colorScheme.onSurface
                    )
                }

                // Theme selector menu
                Box {
                    IconButton(
                        onClick = { themeMenuExpanded = true },
                        modifier = Modifier.testTag("reader_theme_button")
                    ) {
                        Icon(imageVector = Icons.Default.Palette, contentDescription = "Reading Themes")
                    }

                    DropdownMenu(
                        expanded = themeMenuExpanded,
                        onDismissRequest = { themeMenuExpanded = false }
                    ) {
                        ReadingTheme.values().forEach { theme ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(theme.paperColor)
                                                .border(1.dp, Color.Gray, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = theme.title,
                                            fontWeight = if (theme == currentTheme) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                },
                                onClick = {
                                    onSelectTheme(theme)
                                    themeMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // All Notes & Bookmarks drawer
                IconButton(onClick = onOpenNotesDrawer, modifier = Modifier.testTag("reader_notes_drawer_button")) {
                    Icon(imageVector = Icons.Default.EditNote, contentDescription = "View Notes & Bookmarks")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )
    }
}

@Composable
fun ReadingBottomBar(
    currentPage: Int,
    totalPages: Int,
    isReadingRulerEnabled: Boolean,
    onPageChange: (Int) -> Unit,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit,
    onToggleReadingRuler: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Page scrubber slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPrevPage,
                    enabled = currentPage > 0,
                    modifier = Modifier.testTag("scrubber_prev_button")
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = "Previous Page")
                }

                Slider(
                    value = currentPage.toFloat(),
                    onValueChange = { onPageChange(it.toInt()) },
                    valueRange = 0f..(totalPages - 1).coerceAtLeast(1).toFloat(),
                    steps = (totalPages - 2).coerceAtLeast(0),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("page_scrubber_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )

                IconButton(
                    onClick = onNextPage,
                    enabled = currentPage < totalPages - 1,
                    modifier = Modifier.testTag("scrubber_next_button")
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.NavigateNext, contentDescription = "Next Page")
                }
            }

            // Page stats & Ruler toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val progressPercent = if (totalPages > 0) ((currentPage + 1).toFloat() / totalPages.toFloat() * 100).toInt() else 0

                Text(
                    text = "Page ${currentPage + 1} of $totalPages  •  $progressPercent%",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleReadingRuler,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("toggle_reading_ruler_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.LineWeight,
                            contentDescription = "Reading Guide Ruler",
                            tint = if (isReadingRulerEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
