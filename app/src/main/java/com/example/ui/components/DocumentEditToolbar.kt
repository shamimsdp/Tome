package com.example.ui.components

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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val HIGHLIGHT_COLOR_PALETTE = listOf(
    "#FFEB3B", // Amber Gold
    "#10B981", // Emerald Green
    "#00BCD4", // Cyan Sky
    "#F43F5E", // Rose Coral
    "#8B5CF6", // Royal Purple
    "#374151"  // Dark Charcoal
)

@Composable
fun ContextualSelectionToolbar(
    selectedColor: String,
    onColorSelected: (String) -> Unit,
    onReadAloud: () -> Unit,
    onAddNote: () -> Unit,
    onCopyText: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = modifier.testTag("contextual_selection_toolbar")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Color circles
            HIGHLIGHT_COLOR_PALETTE.forEach { hex ->
                val color = Color(android.graphics.Color.parseColor(hex))
                val isSelected = selectedColor.equals(hex, ignoreCase = true)

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.2f),
                            shape = CircleShape
                        )
                        .clickable { onColorSelected(hex) }
                )
            }

            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            Spacer(modifier = Modifier.width(6.dp))

            // Read Aloud button
            IconButton(
                onClick = onReadAloud,
                modifier = Modifier.size(32.dp).testTag("selection_read_aloud_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Read Aloud",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Add Note button
            IconButton(
                onClick = onAddNote,
                modifier = Modifier.size(32.dp).testTag("selection_add_note_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Add Note",
                    modifier = Modifier.size(18.dp)
                )
            }

            // Copy button
            IconButton(
                onClick = onCopyText,
                modifier = Modifier.size(32.dp).testTag("selection_copy_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy Text",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun EditToolsBottomBar(
    isHighlightMode: Boolean,
    isBookmarked: Boolean,
    onToggleHighlight: () -> Unit,
    onAddNote: () -> Unit,
    onStartVoiceReading: () -> Unit,
    onToggleBookmark: () -> Unit,
    onOpenThemeMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Highlighter Tool
                EditToolItem(
                    icon = Icons.Default.Brush,
                    label = "Highlight",
                    isActive = isHighlightMode,
                    onClick = onToggleHighlight
                )

                // Add Note Tool
                EditToolItem(
                    icon = Icons.Default.TextFields,
                    label = "Add Note",
                    isActive = false,
                    onClick = onAddNote
                )

                // Voice Reader Tool (Read Aloud)
                EditToolItem(
                    icon = Icons.Default.RecordVoiceOver,
                    label = "Read Aloud",
                    isActive = false,
                    onClick = onStartVoiceReading
                )

                // Bookmark Tool
                EditToolItem(
                    icon = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    label = "Bookmark",
                    isActive = isBookmarked,
                    onClick = onToggleBookmark
                )

                // Reading Theme Tool
                EditToolItem(
                    icon = Icons.Default.Palette,
                    label = "Theme",
                    isActive = false,
                    onClick = onOpenThemeMenu
                )
            }
        }
    }
}

@Composable
private fun EditToolItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
