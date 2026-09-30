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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LineWeight
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.compose.ui.text.font.FontFamily
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
    bookmarkCount: Int = 0,
    sessionDurationText: String = "",
    isSessionTimerRunning: Boolean = true,
    isPageFlipEnabled: Boolean = true,
    onBack: () -> Unit,
    onToggleBookmark: () -> Unit,
    onOpenBookmarksDrawer: () -> Unit = {},
    onToggleHighlightMode: () -> Unit,
    onTogglePageFlip: () -> Unit = {},
    onOpenSearch: () -> Unit,
    onOpenNotesDrawer: () -> Unit,
    onOpenThumbnailGrid: () -> Unit = {},
    onOpenExportSheet: () -> Unit = {},
    onOpenChat: () -> Unit,
    onStartVoiceReading: () -> Unit,
    onOpenVoiceSettings: (() -> Unit)? = null,
    onOpenSessionTimer: () -> Unit = {},
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
                // Reading Session Timer pill button
                if (sessionDurationText.isNotBlank()) {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable(onClick = onOpenSessionTimer)
                            .testTag("reader_timer_chip"),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isSessionTimerRunning) Color(0xFF10B981) else Color(0xFFF59E0B))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Timer",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = sessionDurationText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                // Voice Read Aloud button
                IconButton(onClick = onStartVoiceReading, modifier = Modifier.testTag("reader_voice_read_button")) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = "Read Aloud",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Voice & TTS Engine Settings button
                if (onOpenVoiceSettings != null) {
                    IconButton(onClick = onOpenVoiceSettings, modifier = Modifier.testTag("reader_voice_settings_button")) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Voice & Engine Settings",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Ask AI Companion button
                IconButton(onClick = onOpenChat, modifier = Modifier.testTag("reader_ai_chat_button")) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Ask Tome AI",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

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

                // Bookmark toggle for current page
                IconButton(onClick = onToggleBookmark, modifier = Modifier.testTag("reader_bookmark_toggle")) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = if (isBookmarked) "Remove Bookmark" else "Bookmark Page",
                        tint = if (isBookmarked) Color(0xFFE11D48) else MaterialTheme.colorScheme.onSurface
                    )
                }

                // Display 'My Bookmarks' list in side drawer
                IconButton(onClick = onOpenBookmarksDrawer, modifier = Modifier.testTag("reader_bookmarks_drawer_button")) {
                    BadgedBox(
                        badge = {
                            if (bookmarkCount > 0) {
                                Badge(
                                    containerColor = Color(0xFFE11D48),
                                    contentColor = Color.White
                                ) {
                                    Text("$bookmarkCount", fontSize = 10.sp)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.CollectionsBookmark,
                            contentDescription = "My Bookmarks Drawer",
                            tint = if (bookmarkCount > 0) Color(0xFFE11D48) else MaterialTheme.colorScheme.onSurface
                        )
                    }
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

                // Page flip animation toggle button
                IconButton(onClick = onTogglePageFlip, modifier = Modifier.testTag("reader_page_flip_toggle")) {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = if (isPageFlipEnabled) "Page Flip Animation: ON" else "Page Flip Animation: OFF",
                        tint = if (isPageFlipEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // All Notes & Bookmarks drawer
                IconButton(onClick = onOpenNotesDrawer, modifier = Modifier.testTag("reader_notes_drawer_button")) {
                    Icon(imageVector = Icons.Default.EditNote, contentDescription = "View Notes & Bookmarks")
                }

                // Thumbnail Grid View button
                IconButton(onClick = onOpenThumbnailGrid, modifier = Modifier.testTag("reader_thumbnail_grid_button")) {
                    Icon(imageVector = Icons.Default.GridView, contentDescription = "Page Thumbnails")
                }

                // Export Annotations button
                IconButton(onClick = onOpenExportSheet, modifier = Modifier.testTag("reader_export_button")) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Export Notes & Highlights")
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
    sessionDurationText: String = "",
    isPageFlipEnabled: Boolean = true,
    onOpenSessionTimer: () -> Unit = {},
    onTogglePageFlip: () -> Unit = {},
    onPageChange: (Int) -> Unit,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit,
    onToggleReadingRuler: () -> Unit,
    onOpenThumbnailGrid: () -> Unit = {},
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

            // Page stats, session timer & Ruler toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val progressPercent = if (totalPages > 0) ((currentPage + 1).toFloat() / totalPages.toFloat() * 100).toInt() else 0

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (sessionDurationText.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onOpenSessionTimer)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                .testTag("bottom_session_timer_chip"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Reading Timer",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = sessionDurationText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Text(
                        text = "Page ${currentPage + 1} of $totalPages  •  $progressPercent%",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onTogglePageFlip,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("toggle_page_flip_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = if (isPageFlipEnabled) "Page Flip Animation: ON" else "Page Flip Animation: OFF",
                            tint = if (isPageFlipEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

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

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = onOpenThumbnailGrid,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("bottom_thumbnail_grid_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = "Thumbnails Grid",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
