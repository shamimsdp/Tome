package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Modern Material 3 bottom control bar for Text-to-Speech (TTS) Voice Reading in the PDF Reader.
 * Features:
 * - Play / Pause toggle with rewind and forward sentence skipping
 * - Interactive TTS Progress Slider for scrubbing through page sentences
 * - Independent Volume Control Slider with mute/unmute
 * - Speed selector with dropdown menu (0.75x to 2.0x) with SharedPreferences persistence
 * - Karaoke highlight preview of the currently spoken sentence
 * - Integrated page navigation
 */
@Composable
fun VoiceReadingBottomBar(
    isPlaying: Boolean,
    currentSentenceIndex: Int,
    totalSentences: Int,
    currentSentenceText: String,
    playbackSpeed: Float,
    playbackVolume: Float = 1.0f,
    currentPage: Int = 0,
    totalPages: Int = 1,
    onTogglePlayPause: () -> Unit,
    onSkipForward: () -> Unit,
    onSkipBackward: () -> Unit,
    onSeekTo: (Int) -> Unit = {},
    onSelectSpeed: (Float) -> Unit = {},
    onVolumeChange: (Float) -> Unit = {},
    onCycleSpeed: () -> Unit = {},
    onPrevPage: () -> Unit = {},
    onNextPage: () -> Unit = {},
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var speedMenuExpanded by remember { mutableStateOf(false) }
    var showVolumeSlider by remember { mutableStateOf(false) }
    var lastNonZeroVolume by remember { mutableFloatStateOf(if (playbackVolume > 0f) playbackVolume else 1.0f) }

    val availableSpeeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

    // Local slider state while user is scrubbing through sentences
    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderDragValue by remember { mutableFloatStateOf(0f) }

    val activeSliderValue = if (isDraggingSlider) {
        sliderDragValue
    } else {
        currentSentenceIndex.toFloat()
    }

    Card(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("voice_reading_bottom_bar")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            // 1. Header: Status, Waveform Visualizer & Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(MaterialTheme.colorScheme.primary, Color(0xFF8B5CF6))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "Voice Reader (TTS)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (totalSentences > 0) {
                                "Page ${currentPage + 1} of $totalPages • Sentence ${currentSentenceIndex + 1} of $totalSentences"
                            } else {
                                "Page ${currentPage + 1} of $totalPages • Ready to read"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Waveform indicator
                Box(modifier = Modifier.width(64.dp)) {
                    AudioWaveformVisualizer(isPlaying = isPlaying)
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("close_voice_player_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Voice Reader",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Karaoke Highlight Preview Card
            if (currentSentenceText.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = currentSentenceText,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            } else {
                Spacer(modifier = Modifier.height(4.dp))
            }

            // 3. TTS Voice Progress Slider
            Column(modifier = Modifier.fillMaxWidth()) {
                val maxRange = (totalSentences - 1).coerceAtLeast(1).toFloat()
                val currentStep = if (isDraggingSlider) sliderDragValue.toInt() else currentSentenceIndex
                val percent = if (totalSentences > 0) (((currentStep + 1).toFloat() / totalSentences.toFloat()) * 100).toInt() else 0

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sentence ${currentStep + 1} ($percent%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "$totalSentences sentences",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Slider(
                    value = activeSliderValue.coerceIn(0f, maxRange),
                    onValueChange = {
                        isDraggingSlider = true
                        sliderDragValue = it
                    },
                    onValueChangeFinished = {
                        isDraggingSlider = false
                        onSeekTo(sliderDragValue.toInt().coerceIn(0, (totalSentences - 1).coerceAtLeast(0)))
                    },
                    valueRange = 0f..maxRange,
                    steps = (totalSentences - 2).coerceAtLeast(0),
                    enabled = totalSentences > 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("voice_progress_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }

            // 4. Expandable Independent Volume Control Slider
            AnimatedVisibility(
                visible = showVolumeSlider,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (playbackVolume > 0.01f) {
                                    lastNonZeroVolume = playbackVolume
                                    onVolumeChange(0.0f)
                                } else {
                                    onVolumeChange(lastNonZeroVolume)
                                }
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("voice_volume_mute_button")
                        ) {
                            val volumeIcon = when {
                                playbackVolume <= 0.01f -> Icons.AutoMirrored.Filled.VolumeMute
                                playbackVolume < 0.5f -> Icons.AutoMirrored.Filled.VolumeDown
                                else -> Icons.AutoMirrored.Filled.VolumeUp
                            }
                            Icon(
                                imageVector = volumeIcon,
                                contentDescription = "Toggle Mute",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Slider(
                            value = playbackVolume,
                            onValueChange = {
                                if (it > 0.01f) lastNonZeroVolume = it
                                onVolumeChange(it)
                            },
                            valueRange = 0.0f..1.0f,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("voice_volume_slider"),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "${(playbackVolume * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 5. Bottom Controls Row (Speed Selector + Volume Toggle + Playback Controls + Page Navigation)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Speed Selector & Volume Toggle Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Speed Selector Pill with Dropdown Menu
                    Box {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { speedMenuExpanded = true }
                                .testTag("voice_speed_selector")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${playbackSpeed}x",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = speedMenuExpanded,
                            onDismissRequest = { speedMenuExpanded = false }
                        ) {
                            availableSpeeds.forEach { speed ->
                                val isSelected = playbackSpeed == speed
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "${speed}x" + if (speed == 1.0f) " (Normal)" else "",
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        onSelectSpeed(speed)
                                        speedMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Volume Toggle Pill
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (showVolumeSlider) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showVolumeSlider = !showVolumeSlider }
                            .testTag("voice_volume_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val volumeIcon = when {
                                playbackVolume <= 0.01f -> Icons.AutoMirrored.Filled.VolumeMute
                                playbackVolume < 0.5f -> Icons.AutoMirrored.Filled.VolumeDown
                                else -> Icons.AutoMirrored.Filled.VolumeUp
                            }
                            Icon(
                                imageVector = volumeIcon,
                                contentDescription = "TTS Volume ${(playbackVolume * 100).toInt()}%",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${(playbackVolume * 100).toInt()}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Center: Main Playback Controls: Skip Back, Play/Pause, Skip Forward
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Previous sentence
                    IconButton(
                        onClick = onSkipBackward,
                        enabled = totalSentences > 0,
                        modifier = Modifier.testTag("voice_skip_backward_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastRewind,
                            contentDescription = "Previous sentence",
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Play / Pause Circle Action Button
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(MaterialTheme.colorScheme.primary, Color(0xFF8B5CF6))
                                )
                            )
                            .clickable { onTogglePlayPause() }
                            .testTag("voice_toggle_play_pause_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Next sentence
                    IconButton(
                        onClick = onSkipForward,
                        enabled = totalSentences > 0,
                        modifier = Modifier.testTag("voice_skip_forward_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "Next sentence",
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // Right: Page Navigation (Prev Page / Next Page)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onPrevPage,
                        enabled = currentPage > 0,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("voice_prev_page_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.NavigateBefore,
                            contentDescription = "Previous Page",
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = onNextPage,
                        enabled = currentPage < totalPages - 1,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("voice_next_page_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                            contentDescription = "Next Page",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Backward-compatible wrapper delegating to VoiceReadingBottomBar.
 */
@Composable
fun VoiceReadingPlayer(
    isPlaying: Boolean,
    currentSentenceIndex: Int,
    totalSentences: Int,
    currentSentenceText: String,
    playbackSpeed: Float,
    playbackVolume: Float = 1.0f,
    currentPage: Int = 0,
    totalPages: Int = 1,
    onTogglePlayPause: () -> Unit,
    onSkipForward: () -> Unit,
    onSkipBackward: () -> Unit,
    onSeekTo: (Int) -> Unit = {},
    onSelectSpeed: (Float) -> Unit = {},
    onVolumeChange: (Float) -> Unit = {},
    onCycleSpeed: () -> Unit = {},
    onPrevPage: () -> Unit = {},
    onNextPage: () -> Unit = {},
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    VoiceReadingBottomBar(
        isPlaying = isPlaying,
        currentSentenceIndex = currentSentenceIndex,
        totalSentences = totalSentences,
        currentSentenceText = currentSentenceText,
        playbackSpeed = playbackSpeed,
        playbackVolume = playbackVolume,
        currentPage = currentPage,
        totalPages = totalPages,
        onTogglePlayPause = onTogglePlayPause,
        onSkipForward = onSkipForward,
        onSkipBackward = onSkipBackward,
        onSeekTo = onSeekTo,
        onSelectSpeed = onSelectSpeed,
        onVolumeChange = onVolumeChange,
        onCycleSpeed = onCycleSpeed,
        onPrevPage = onPrevPage,
        onNextPage = onNextPage,
        onClose = onClose,
        modifier = modifier
    )
}

@Composable
fun AudioWaveformVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")

    val anim1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(450), RepeatMode.Reverse),
        label = "bar1"
    )
    val anim2 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(350), RepeatMode.Reverse),
        label = "bar2"
    )
    val anim3 by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(550), RepeatMode.Reverse),
        label = "bar3"
    )
    val anim4 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(400), RepeatMode.Reverse),
        label = "bar4"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val bars = 16
        for (i in 0 until bars) {
            val scale = if (!isPlaying) {
                0.2f
            } else {
                when (i % 4) {
                    0 -> anim1
                    1 -> anim2
                    2 -> anim3
                    else -> anim4
                }
            }

            Box(
                modifier = Modifier
                    .padding(horizontal = 1.5.dp)
                    .width(2.5.dp)
                    .height((24 * scale).dp.coerceAtLeast(3.dp))
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    )
            )
        }
    }
}
