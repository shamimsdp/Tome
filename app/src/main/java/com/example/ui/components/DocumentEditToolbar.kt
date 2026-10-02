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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
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
    "#FFE600", // Bright Yellow
    "#10B981", // Emerald Green
    "#00BCD4", // Cyan Sky
    "#F43F5E", // Rose Coral
    "#8B5CF6", // Royal Purple
    "#FF6B00"  // Sunset Orange
)

@Composable
fun ContextualSelectionToolbar(
    selectedColor: String,
    onColorSelected: (String) -> Unit,
    onOpenCustomColorPicker: () -> Unit,
    onReadAloud: () -> Unit,
    onAddNote: () -> Unit,
    onCopyText: () -> Unit,
    onOpenEditMode: (() -> Unit)? = null,
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
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
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

            // Custom color button
            IconButton(
                onClick = onOpenCustomColorPicker,
                modifier = Modifier.size(28.dp).testTag("custom_color_picker_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = "Custom Color",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            Spacer(modifier = Modifier.width(4.dp))

            // Read Aloud button
            IconButton(
                onClick = onReadAloud,
                modifier = Modifier.size(30.dp).testTag("selection_read_aloud_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Read Aloud",
                    modifier = Modifier.size(17.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Add Note button
            IconButton(
                onClick = onAddNote,
                modifier = Modifier.size(30.dp).testTag("selection_add_note_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Add Note",
                    modifier = Modifier.size(17.dp)
                )
            }

            // Copy button
            IconButton(
                onClick = onCopyText,
                modifier = Modifier.size(30.dp).testTag("selection_copy_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy Text",
                    modifier = Modifier.size(17.dp)
                )
            }

            if (onOpenEditMode != null) {
                IconButton(
                    onClick = onOpenEditMode,
                    modifier = Modifier.size(30.dp).testTag("selection_edit_mode_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.TextFields,
                        contentDescription = "Add Elements",
                        modifier = Modifier.size(17.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
fun EditModeFloatingBar(
    onAddText: () -> Unit,
    onAddImage: () -> Unit,
    onAddStamp: () -> Unit,
    onDoneEditing: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
        modifier = modifier.testTag("edit_mode_floating_bar")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "✎ EDIT MODE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            FilledTonalButton(
                onClick = onAddText,
                modifier = Modifier.height(34.dp).testTag("toolbar_add_text_button")
            ) {
                Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Text", fontSize = 12.sp)
            }

            FilledTonalButton(
                onClick = onAddImage,
                modifier = Modifier.height(34.dp).testTag("toolbar_add_image_button")
            ) {
                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Image", fontSize = 12.sp)
            }

            FilledTonalButton(
                onClick = onAddStamp,
                modifier = Modifier.height(34.dp).testTag("toolbar_add_stamp_button")
            ) {
                Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Stamp", fontSize = 12.sp)
            }

            IconButton(
                onClick = onDoneEditing,
                modifier = Modifier.size(32.dp).testTag("toolbar_done_editing_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Done Editing",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
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
