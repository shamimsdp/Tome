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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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

val EXTENDED_HIGHLIGHT_PALETTE = listOf(
    "#FFE600", // Bright Neon Yellow
    "#FFEB3B", // Amber Gold
    "#10B981", // Vivid Emerald
    "#34D399", // Pastel Mint
    "#06B6D4", // Sky Cyan
    "#3B82F6", // Electric Blue
    "#8B5CF6", // Royal Purple
    "#A855F7", // Orchid Violet
    "#F43F5E", // Rose Coral
    "#EC4899", // Hot Pink
    "#FF6B00", // Sunset Tangerine
    "#64748B"  // Slate Charcoal
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomColorPickerDialog(
    initialColorHex: String,
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val initialColor = remember(initialColorHex) {
        try {
            android.graphics.Color.parseColor(initialColorHex)
        } catch (_: Exception) {
            android.graphics.Color.parseColor("#FFE600")
        }
    }

    var red by remember { mutableFloatStateOf(android.graphics.Color.red(initialColor) / 255f) }
    var green by remember { mutableFloatStateOf(android.graphics.Color.green(initialColor) / 255f) }
    var blue by remember { mutableFloatStateOf(android.graphics.Color.blue(initialColor) / 255f) }

    val currentColor = remember(red, green, blue) {
        Color(red, green, blue)
    }

    val currentHex = remember(red, green, blue) {
        String.format("#%02X%02X%02X", (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt())
    }

    var manualHexInput by remember { mutableStateOf(currentHex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = currentColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select Custom Color")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Color Preview Card
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(currentColor)
                                .border(1.5.dp, Color.Black.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        )
                        Column {
                            Text(
                                text = "Preview Swatch",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentHex,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Preset Swatches
                Text("Vibrant Highlighter Presets", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EXTENDED_HIGHLIGHT_PALETTE.forEach { hex ->
                        val swatchColor = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = currentHex.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(swatchColor)
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.2f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    val parsed = android.graphics.Color.parseColor(hex)
                                    red = android.graphics.Color.red(parsed) / 255f
                                    green = android.graphics.Color.green(parsed) / 255f
                                    blue = android.graphics.Color.blue(parsed) / 255f
                                    manualHexInput = hex
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text("Custom RGB Sliders", style = MaterialTheme.typography.labelMedium)

                // Red Slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("R", fontWeight = FontWeight.Bold, color = Color.Red, modifier = Modifier.width(20.dp))
                    Slider(
                        value = red,
                        onValueChange = {
                            red = it
                            manualHexInput = String.format("#%02X%02X%02X", (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt())
                        },
                        colors = SliderDefaults.colors(thumbColor = Color.Red, activeTrackColor = Color.Red),
                        modifier = Modifier.weight(1f)
                    )
                    Text("${(red * 255).toInt()}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(30.dp))
                }

                // Green Slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("G", fontWeight = FontWeight.Bold, color = Color(0xFF10B981), modifier = Modifier.width(20.dp))
                    Slider(
                        value = green,
                        onValueChange = {
                            green = it
                            manualHexInput = String.format("#%02X%02X%02X", (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt())
                        },
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF10B981), activeTrackColor = Color(0xFF10B981)),
                        modifier = Modifier.weight(1f)
                    )
                    Text("${(green * 255).toInt()}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(30.dp))
                }

                // Blue Slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("B", fontWeight = FontWeight.Bold, color = Color.Blue, modifier = Modifier.width(20.dp))
                    Slider(
                        value = blue,
                        onValueChange = {
                            blue = it
                            manualHexInput = String.format("#%02X%02X%02X", (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt())
                        },
                        colors = SliderDefaults.colors(thumbColor = Color.Blue, activeTrackColor = Color.Blue),
                        modifier = Modifier.weight(1f)
                    )
                    Text("${(blue * 255).toInt()}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(30.dp))
                }

                // Manual HEX Input
                OutlinedTextField(
                    value = manualHexInput,
                    onValueChange = { input ->
                        manualHexInput = input
                        if (input.startsWith("#") && (input.length == 7 || input.length == 9)) {
                            try {
                                val parsed = android.graphics.Color.parseColor(input)
                                red = android.graphics.Color.red(parsed) / 255f
                                green = android.graphics.Color.green(parsed) / 255f
                                blue = android.graphics.Color.blue(parsed) / 255f
                            } catch (_: Exception) {}
                        }
                    },
                    label = { Text("Hex Code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onColorSelected(currentHex)
                    onDismiss()
                },
                modifier = Modifier.testTag("apply_custom_color_button")
            ) {
                Text("Apply Color")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
