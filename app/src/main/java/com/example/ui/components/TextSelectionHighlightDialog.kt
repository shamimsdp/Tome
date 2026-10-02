package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TextSelectionHighlightDialog(
    pageIndex: Int,
    initialText: String,
    pageTextSegments: List<String>,
    initialColorHex: String,
    onSaveHighlight: (text: String, colorHex: String, note: String, tag: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedText by remember { mutableStateOf(initialText) }
    var selectedColorHex by remember { mutableStateOf(initialColorHex) }
    var noteText by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf(ANNOTATION_TAGS[0]) }
    var showCustomColorPicker by remember { mutableStateOf(false) }

    val parsedColor = remember(selectedColorHex) {
        try {
            Color(android.graphics.Color.parseColor(selectedColorHex))
        } catch (_: Exception) {
            Color(0xFFFFEB3B)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Highlight,
                    contentDescription = null,
                    tint = parsedColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Highlight Passage (Page ${pageIndex + 1})")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Highlighted text field
                OutlinedTextField(
                    value = selectedText,
                    onValueChange = { selectedText = it },
                    label = { Text("Selected Text Segment") },
                    placeholder = { Text("Enter or select passage to highlight…") },
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("highlight_segment_input")
                )

                // Quick segment picker if page has multiple text segments
                if (pageTextSegments.isNotEmpty() && selectedText.isBlank()) {
                    Text("Or choose a sentence from this page:", style = MaterialTheme.typography.labelSmall)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                    ) {
                        items(pageTextSegments.take(8)) { segment ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .clickable { selectedText = segment }
                            ) {
                                Text(
                                    text = segment,
                                    fontSize = 12.sp,
                                    maxLines = 2,
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                        }
                    }
                }

                // Custom Color Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Highlight Color", style = MaterialTheme.typography.labelMedium)
                    OutlinedButton(
                        onClick = { showCustomColorPicker = true },
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.ColorLens, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Custom…", fontSize = 11.sp)
                    }
                }

                // Swatches row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EXTENDED_HIGHLIGHT_PALETTE.take(8).forEach { hex ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = selectedColorHex.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.2f),
                                    shape = CircleShape
                                )
                                .clickable { selectedColorHex = hex }
                        )
                    }
                }

                // Note input
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note / Insight (optional)") },
                    placeholder = { Text("Add personal notes, translation, or analysis…") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Tag Chips
                Text("Category Tag", style = MaterialTheme.typography.labelSmall)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ANNOTATION_TAGS.forEach { tag ->
                        FilterChip(
                            selected = selectedTag == tag,
                            onClick = { selectedTag = tag },
                            label = { Text(tag, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalText = selectedText.ifBlank { "Highlighted passage on page ${pageIndex + 1}" }
                    onSaveHighlight(finalText, selectedColorHex, noteText, selectedTag)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = parsedColor),
                modifier = Modifier.testTag("save_custom_highlight_button")
            ) {
                Text("Save Highlight", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showCustomColorPicker) {
        CustomColorPickerDialog(
            initialColorHex = selectedColorHex,
            onColorSelected = { newHex ->
                selectedColorHex = newHex
                showCustomColorPicker = false
            },
            onDismiss = { showCustomColorPicker = false }
        )
    }
}
