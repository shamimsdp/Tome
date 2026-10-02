package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.PageElementEntity

val PRESET_TEXT_COLORS = listOf(
    "#000000", "#1E293B", "#DC2626", "#D97706", "#059669", "#2563EB", "#7C3AED", "#DB2777"
)

val PRESET_BG_COLORS = listOf(
    "#00000000", // Transparent
    "#FFFBEB",   // Warm Yellow Note
    "#FFFFFF",   // Pure White
    "#F1F5F9",   // Light Slate
    "#EFF6FF",   // Soft Ice Blue
    "#ECFDF5",   // Mint Green
    "#FDF2F8"    // Soft Rose
)

val PRESET_STAMPS = listOf(
    "APPROVED" to "#059669",
    "CONFIDENTIAL" to "#DC2626",
    "REVIEWED" to "#2563EB",
    "IMPORTANT" to "#D97706",
    "SIGNATURE" to "#475569",
    "DRAFT" to "#6B7280"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddElementDialog(
    pageIndex: Int,
    onAddTextElement: (text: String, colorHex: String, bgHex: String, fontSize: Int, isBold: Boolean, isItalic: Boolean) -> Unit,
    onAddImageElement: (uriString: String) -> Unit,
    onAddStampElement: (stampTitle: String, colorHex: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    // Text Element State
    var textContent by remember { mutableStateOf("") }
    var selectedTextColor by remember { mutableStateOf("#1E293B") }
    var selectedBgColor by remember { mutableStateOf("#FFFBEB") }
    var fontSize by remember { mutableFloatStateOf(16f) }
    var isBold by remember { mutableStateOf(false) }
    var isItalic by remember { mutableStateOf(false) }

    // Image Element State
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        selectedImageUri = uri
    }

    // Stamp State
    var selectedStamp by remember { mutableStateOf(PRESET_STAMPS[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.TextFields,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Element to Page ${pageIndex + 1}")
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Text") },
                        icon = { Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("tab_add_text")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Image") },
                        icon = { Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("tab_add_image")
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Stamps") },
                        icon = { Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("tab_add_stamp")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                when (selectedTab) {
                    0 -> {
                        // Text Editing Tool
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = textContent,
                                onValueChange = { textContent = it },
                                label = { Text("Enter text to place on page") },
                                placeholder = { Text("E.g. Note, summary, heading, comment…") },
                                maxLines = 4,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("text_element_input")
                            )

                            // Style controls (Bold, Italic, Font Size)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    IconButton(
                                        onClick = { isBold = !isBold },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isBold) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FormatBold,
                                            contentDescription = "Bold",
                                            tint = if (isBold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    IconButton(
                                        onClick = { isItalic = !isItalic },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isItalic) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FormatItalic,
                                            contentDescription = "Italic",
                                            tint = if (isItalic) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                Text(
                                    text = "Size: ${fontSize.toInt()}sp",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Slider(
                                value = fontSize,
                                onValueChange = { fontSize = it },
                                valueRange = 12f..32f,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Text Color Palette
                            Text("Text Color", style = MaterialTheme.typography.labelMedium)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PRESET_TEXT_COLORS.forEach { hex ->
                                    val color = Color(android.graphics.Color.parseColor(hex))
                                    val isSelected = selectedTextColor.equals(hex, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(
                                                width = if (isSelected) 2.5.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                                shape = CircleShape
                                            )
                                            .clickable { selectedTextColor = hex }
                                    )
                                }
                            }

                            // Background Box Color
                            Text("Background Box", style = MaterialTheme.typography.labelMedium)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PRESET_BG_COLORS.forEach { hex ->
                                    val color = if (hex == "#00000000") Color.Transparent else Color(android.graphics.Color.parseColor(hex))
                                    val isSelected = selectedBgColor.equals(hex, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(if (hex == "#00000000") MaterialTheme.colorScheme.surfaceVariant else color)
                                            .border(
                                                width = if (isSelected) 2.5.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                                shape = CircleShape
                                            )
                                            .clickable { selectedBgColor = hex },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (hex == "#00000000") {
                                            Text("∅", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }

                            // Preview Card
                            if (textContent.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Preview:", style = MaterialTheme.typography.labelSmall)
                                Surface(
                                    color = if (selectedBgColor == "#00000000") Color.Transparent else Color(android.graphics.Color.parseColor(selectedBgColor)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.6f)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = textContent,
                                        color = Color(android.graphics.Color.parseColor(selectedTextColor)),
                                        fontSize = fontSize.sp,
                                        fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                                        fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        // Image Picker Tool
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (selectedImageUri != null) {
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                    modifier = Modifier.size(160.dp)
                                ) {
                                    AsyncImage(
                                        model = selectedImageUri,
                                        contentDescription = "Selected Image",
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                Button(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Change Image")
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .clickable {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.AddPhotoAlternate,
                                            contentDescription = null,
                                            modifier = Modifier.size(40.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Tap to choose an image or photo",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Places directly on the current PDF page",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        // Stamp Elements Tool
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Select a document stamp or status watermark:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PRESET_STAMPS.forEach { stamp ->
                                    val isSelected = selectedStamp == stamp
                                    val stampColor = Color(android.graphics.Color.parseColor(stamp.second))

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        border = androidx.compose.foundation.BorderStroke(
                                            width = if (isSelected) 2.5.dp else 1.5.dp,
                                            color = stampColor
                                        ),
                                        color = if (isSelected) stampColor.copy(alpha = 0.15f) else Color.Transparent,
                                        modifier = Modifier
                                            .clickable { selectedStamp = stamp }
                                            .padding(2.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = stampColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                            }
                                            Text(
                                                text = stamp.first,
                                                fontWeight = FontWeight.Black,
                                                color = stampColor,
                                                fontSize = 13.sp,
                                                letterSpacing = 1.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when (selectedTab) {
                        0 -> {
                            if (textContent.isNotBlank()) {
                                onAddTextElement(
                                    textContent,
                                    selectedTextColor,
                                    selectedBgColor,
                                    fontSize.toInt(),
                                    isBold,
                                    isItalic
                                )
                                onDismiss()
                            }
                        }
                        1 -> {
                            selectedImageUri?.let { uri ->
                                onAddImageElement(uri.toString())
                                onDismiss()
                            }
                        }
                        2 -> {
                            onAddStampElement(selectedStamp.first, selectedStamp.second)
                            onDismiss()
                        }
                    }
                },
                enabled = when (selectedTab) {
                    0 -> textContent.isNotBlank()
                    1 -> selectedImageUri != null
                    2 -> true
                    else -> false
                },
                modifier = Modifier.testTag("confirm_add_element_button")
            ) {
                Text("Add to Page")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
