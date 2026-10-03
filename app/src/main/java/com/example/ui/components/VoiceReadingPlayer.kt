package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.TtsEngineOption
import com.example.engine.TtsVoiceOption

/**
 * Modern Material 3 bottom control bar for Text-to-Speech (TTS) Voice Reading in the PDF Reader.
 * Features:
 * - Play / Pause toggle with rewind and forward sentence skipping
 * - Interactive TTS Progress Slider for scrubbing through page sentences
 * - Independent Volume Control Slider with mute/unmute
 * - Speed selector with dropdown menu (0.75x to 2.0x) with SharedPreferences persistence
 * - Prominent "Change Voice" button opening comprehensive Voice & TTS Engine Sheet
 * - Bangla text reading support with native Bengali speech or phonetic fallback
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
    playbackPitch: Float = 1.0f,
    availableVoices: List<TtsVoiceOption> = emptyList(),
    availableEngines: List<TtsEngineOption> = emptyList(),
    selectedVoiceName: String? = null,
    selectedEnginePackage: String? = null,
    selectedLanguageMode: String = "AUTO",
    isReadingBangla: Boolean = false,
    isBanglaSupported: Boolean = false,
    currentPage: Int = 0,
    totalPages: Int = 1,
    onTogglePlayPause: () -> Unit,
    onSkipForward: () -> Unit,
    onSkipBackward: () -> Unit,
    onSeekTo: (Int) -> Unit = {},
    onSelectSpeed: (Float) -> Unit = {},
    onVolumeChange: (Float) -> Unit = {},
    onPitchChange: (Float) -> Unit = {},
    onSelectVoice: (String?) -> Unit = {},
    onSwitchEngine: (String) -> Unit = {},
    onSelectLanguageMode: (String) -> Unit = {},
    onTestVoice: (String?, Boolean) -> Unit = { _, _ -> },
    onCycleSpeed: () -> Unit = {},
    onPrevPage: () -> Unit = {},
    onNextPage: () -> Unit = {},
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var speedMenuExpanded by remember { mutableStateOf(false) }
    var showSpeedSlider by remember { mutableStateOf(false) }
    var showVolumeSlider by remember { mutableStateOf(false) }
    var showVoiceSettings by remember { mutableStateOf(false) }
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

    // Selected voice display label
    val activeVoiceLabel = remember(selectedVoiceName, availableVoices, isReadingBangla) {
        val found = availableVoices.find { it.name == selectedVoiceName }
        when {
            found != null -> found.displayName.substringBefore(" • ").take(14)
            isReadingBangla -> "বাংলা কথক"
            else -> "Default Voice"
        }
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
                .padding(horizontal = 16.dp, vertical = 12.dp)
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Read Aloud (TTS)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (isReadingBangla) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFF059669).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "বাংলা",
                                        color = Color(0xFF059669),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
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
                Box(modifier = Modifier.width(54.dp)) {
                    AudioWaveformVisualizer(isPlaying = isPlaying)
                }

                Spacer(modifier = Modifier.width(6.dp))

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

            Spacer(modifier = Modifier.height(6.dp))

            // 2. Karaoke Highlight Preview Card
            if (currentSentenceText.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
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
                Spacer(modifier = Modifier.height(6.dp))
            }

            // 3. TTS Voice Progress Slider
            if (totalSentences > 0) {
                val maxRange = (totalSentences - 1).coerceAtLeast(1).toFloat()
                Slider(
                    value = activeSliderValue.coerceIn(0f, maxRange),
                    onValueChange = {
                        isDraggingSlider = true
                        sliderDragValue = it
                    },
                    onValueChangeFinished = {
                        isDraggingSlider = false
                        onSeekTo(sliderDragValue.toInt())
                    },
                    valueRange = 0f..maxRange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                        .testTag("voice_reading_sentence_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            // 4. Expandable Speed Slider (slider control for speech playback speed)
            AnimatedVisibility(
                visible = showSpeedSlider,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("voice_reading_speed_slider_container")
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Speech Playback Speed",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = String.format(java.util.Locale.US, "%.2fx", playbackSpeed) + if (playbackSpeed == 1.0f) " (Normal)" else "",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Slider(
                            value = playbackSpeed,
                            onValueChange = onSelectSpeed,
                            valueRange = 0.5f..2.5f,
                            steps = 19,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("speech_playback_speed_slider")
                        )

                        // Quick speed preset buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 2.5f).forEach { preset ->
                                val isSelected = (playbackSpeed * 100).toInt() == (preset * 100).toInt()
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onSelectSpeed(preset) }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${preset}x",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Expandable Volume Slider (if toggled)
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
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (playbackVolume > 0f) {
                                    lastNonZeroVolume = playbackVolume
                                    onVolumeChange(0.0f)
                                } else {
                                    onVolumeChange(lastNonZeroVolume)
                                }
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            val volumeIcon = when {
                                playbackVolume <= 0.01f -> Icons.AutoMirrored.Filled.VolumeMute
                                playbackVolume < 0.5f -> Icons.AutoMirrored.Filled.VolumeDown
                                else -> Icons.AutoMirrored.Filled.VolumeUp
                            }
                            Icon(imageVector = volumeIcon, contentDescription = null, modifier = Modifier.size(16.dp))
                        }

                        Slider(
                            value = playbackVolume,
                            onValueChange = {
                                if (it > 0.01f) lastNonZeroVolume = it
                                onVolumeChange(it)
                            },
                            valueRange = 0.0f..1.0f,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = "${(playbackVolume * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 5. Main Playback Row: Prev Page, Skip Back, Play/Pause, Skip Forward, Next Page
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPrevPage,
                    enabled = currentPage > 0,
                    modifier = Modifier.size(36.dp).testTag("voice_prev_page_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.NavigateBefore,
                        contentDescription = "Previous Page",
                        modifier = Modifier.size(24.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    IconButton(
                        onClick = onSkipBackward,
                        enabled = totalSentences > 0,
                        modifier = Modifier.size(38.dp).testTag("voice_skip_backward_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastRewind,
                            contentDescription = "Previous Sentence",
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Main Circular Play / Pause Button
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
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    IconButton(
                        onClick = onSkipForward,
                        enabled = totalSentences > 0,
                        modifier = Modifier.size(38.dp).testTag("voice_skip_forward_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "Next Sentence",
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onNextPage,
                    enabled = currentPage < totalPages - 1,
                    modifier = Modifier.size(36.dp).testTag("voice_next_page_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                        contentDescription = "Next Page",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 6. Voice Options, Speed, & Volume Row (Prominent "Change Voice" Button)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Change Voice Pill Button (Always visible & prominent!)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { showVoiceSettings = true }
                        .testTag("voice_options_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Change Voice",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Change Voice ($activeVoiceLabel)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Speed selector pill (toggles the speech playback speed slider)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (showSpeedSlider) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showSpeedSlider = !showSpeedSlider }
                            .testTag("voice_speed_selector")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Adjust Speech Speed",
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

                    // Volume button
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (showVolumeSlider) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showVolumeSlider = !showVolumeSlider }
                            .testTag("voice_volume_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val volumeIcon = when {
                                playbackVolume <= 0.01f -> Icons.AutoMirrored.Filled.VolumeMute
                                playbackVolume < 0.5f -> Icons.AutoMirrored.Filled.VolumeDown
                                else -> Icons.AutoMirrored.Filled.VolumeUp
                            }
                            Icon(
                                imageVector = volumeIcon,
                                contentDescription = "TTS Volume",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Voice Selection & Audio Customization Bottom Sheet
    if (showVoiceSettings) {
        VoiceSettingsSheet(
            availableVoices = availableVoices,
            availableEngines = availableEngines,
            selectedVoiceName = selectedVoiceName,
            selectedEnginePackage = selectedEnginePackage,
            selectedLanguageMode = selectedLanguageMode,
            playbackPitch = playbackPitch,
            playbackSpeed = playbackSpeed,
            isReadingBangla = isReadingBangla,
            isBanglaSupported = isBanglaSupported,
            onSelectVoice = onSelectVoice,
            onSwitchEngine = onSwitchEngine,
            onSelectLanguageMode = onSelectLanguageMode,
            onPitchChange = onPitchChange,
            onSpeedChange = onSelectSpeed,
            onTestVoice = onTestVoice,
            onDismiss = { showVoiceSettings = false }
        )
    }
}

/**
 * Full-featured Bottom Sheet to customize Voice, Language (including Bangla-specific voices),
 * Pitch, Speed, and installed system TTS Engines.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSettingsSheet(
    availableVoices: List<TtsVoiceOption>,
    availableEngines: List<TtsEngineOption> = emptyList(),
    selectedVoiceName: String?,
    selectedEnginePackage: String? = null,
    selectedLanguageMode: String,
    playbackPitch: Float,
    playbackSpeed: Float,
    isReadingBangla: Boolean,
    isBanglaSupported: Boolean,
    onSelectVoice: (String?) -> Unit,
    onSwitchEngine: (String) -> Unit = {},
    onSelectLanguageMode: (String) -> Unit,
    onPitchChange: (Float) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onTestVoice: (String?, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var genderFilter by remember { mutableStateOf("ALL") } // "ALL", "FEMALE", "MALE"

    val banglaVoices = remember(availableVoices, genderFilter) {
        val list = availableVoices.filter { it.isBangla }
        when (genderFilter) {
            "FEMALE" -> list.filter { it.gender == "Female" }
            "MALE" -> list.filter { it.gender == "Male" }
            else -> list
        }
    }

    val displayAllVoices = remember(availableVoices, genderFilter) {
        when (genderFilter) {
            "FEMALE" -> availableVoices.filter { it.gender == "Female" }
            "MALE" -> availableVoices.filter { it.gender == "Male" }
            else -> availableVoices
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.90f)
                .padding(horizontal = 20.dp)
                .testTag("voice_settings_sheet")
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Read Aloud & Voice Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("read_aloud_modal_title")
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_voice_settings_button")) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Prominent Speech Playback Speed Slider Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("read_aloud_speed_slider_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Speech Playback Speed",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Text(
                                text = String.format(java.util.Locale.US, "%.2fx", playbackSpeed) + when {
                                    playbackSpeed < 0.85f -> " (Slower)"
                                    playbackSpeed in 0.95f..1.05f -> " (Normal)"
                                    playbackSpeed in 1.1f..1.4f -> " (Fast)"
                                    else -> " (Very Fast)"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Slider(
                        value = playbackSpeed,
                        onValueChange = onSpeedChange,
                        valueRange = 0.5f..2.5f,
                        steps = 19,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("read_aloud_speed_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    // Quick speed presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { preset ->
                            val isSelected = kotlin.math.abs(playbackSpeed - preset) < 0.05f
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSpeedChange(preset) },
                                label = {
                                    Text(
                                        text = "${preset}x" + if (preset == 1.0f) " (Normal)" else "",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                modifier = Modifier.testTag("speed_preset_${(preset * 100).toInt()}")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tab Row: Bangla Voices, All Voices, TTS Engines
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("বাংলা Voices (${banglaVoices.size})", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                    icon = { Icon(imageVector = Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_bangla_voices")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("All Voices (${availableVoices.size})", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                    icon = { Icon(imageVector = Icons.Default.Language, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_all_voices")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("TTS Engines", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) },
                    icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_tts_engines")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            when (selectedTab) {
                0 -> {
                    // TAB 0: Bangla-Specific Voices
                    Column(modifier = Modifier.weight(1f)) {
                        // Bangla Status Card
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isBanglaSupported) Color(0xFF059669).copy(alpha = 0.12f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (isBanglaSupported) "✓ Native Bangla Voice Active" else "ℹ️ Bangla Speech Ready (Phonetic Engine)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isBanglaSupported) Color(0xFF059669) else MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isBanglaSupported) {
                                        "Bengali text is spoken with native Google Bengali TTS voice synthesis."
                                    } else {
                                        "Bengali reading is audible via our integrated phonetic speech synthesizer. For HD Google Bangla voice data, download in Android TTS Settings."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            onTestVoice("চিত্ত যেথা ভয়শূন্য, উচ্চ যেথা শির। বই মানুষের শ্রেষ্ঠ বন্ধু।", true)
                                        },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Test Bangla Voice", fontSize = 12.sp)
                                    }

                                    Button(
                                        onClick = {
                                            try {
                                                val intent = Intent("com.android.settings.TTS_SETTINGS")
                                                context.startActivity(intent)
                                            } catch (_: Exception) {
                                                try {
                                                    val intent = Intent(Settings.ACTION_SETTINGS)
                                                    context.startActivity(intent)
                                                } catch (_: Exception) {
                                                    Toast.makeText(context, "Open Settings -> System -> Text-to-Speech", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Install Bangla Voice", fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Choose Bangla Voice Persona",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Gender filter row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = genderFilter == "ALL",
                                onClick = { genderFilter = "ALL" },
                                label = { Text("All (${banglaVoices.size})", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = genderFilter == "FEMALE",
                                onClick = { genderFilter = "FEMALE" },
                                label = { Text("♀ Female", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFCE7F3),
                                    selectedLabelColor = Color(0xFFBE185D)
                                )
                            )
                            FilterChip(
                                selected = genderFilter == "MALE",
                                onClick = { genderFilter = "MALE" },
                                label = { Text("♂ Male", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFDBEAFE),
                                    selectedLabelColor = Color(0xFF1D4ED8)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(banglaVoices, key = { it.name }) { voice ->
                                VoiceItemCard(
                                    voice = voice,
                                    isSelected = selectedVoiceName == voice.name,
                                    onSelect = { onSelectVoice(voice.name) },
                                    onTest = {
                                        onSelectVoice(voice.name)
                                        onTestVoice("চিত্ত যেথা ভয়শূন্য, উচ্চ যেথা শির।", true)
                                    }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // TAB 1: All Voices & Tuning
                    Column(modifier = Modifier.weight(1f)) {
                        // Pitch & Speed Adjustment Sliders
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Voice Pitch", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Text(String.format(java.util.Locale.US, "%.1fx", playbackPitch), style = MaterialTheme.typography.labelSmall)
                                }
                                Slider(
                                    value = playbackPitch,
                                    onValueChange = onPitchChange,
                                    valueRange = 0.5f..1.5f,
                                    steps = 8,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Speed", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Text("${playbackSpeed}x", style = MaterialTheme.typography.labelSmall)
                                }
                                Slider(
                                    value = playbackSpeed,
                                    onValueChange = onSpeedChange,
                                    valueRange = 0.5f..2.5f,
                                    steps = 7,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))

                        // Default system voice option
                        val isDefaultSelected = selectedVoiceName == null
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onSelectVoice(null) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDefaultSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier.size(32.dp).clip(CircleShape).background(if (isDefaultSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(imageVector = Icons.Default.RecordVoiceOver, contentDescription = null, tint = if (isDefaultSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("System Default Voice", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Text("Matches system device language and auto-switches", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                if (isDefaultSelected) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        // Gender filter row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = genderFilter == "ALL",
                                onClick = { genderFilter = "ALL" },
                                label = { Text("All (${availableVoices.size})", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = genderFilter == "FEMALE",
                                onClick = { genderFilter = "FEMALE" },
                                label = { Text("♀ Female", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFCE7F3),
                                    selectedLabelColor = Color(0xFFBE185D)
                                )
                            )
                            FilterChip(
                                selected = genderFilter == "MALE",
                                onClick = { genderFilter = "MALE" },
                                label = { Text("♂ Male", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFDBEAFE),
                                    selectedLabelColor = Color(0xFF1D4ED8)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(displayAllVoices, key = { it.name }) { voice ->
                                VoiceItemCard(
                                    voice = voice,
                                    isSelected = selectedVoiceName == voice.name,
                                    onSelect = { onSelectVoice(voice.name) },
                                    onTest = {
                                        onSelectVoice(voice.name)
                                        val sample = if (voice.isBangla) "বই মানুষের সবচেয়ে ভালো বন্ধু।" else "The quick brown fox jumps over the lazy dog."
                                        onTestVoice(sample, voice.isBangla)
                                    }
                                )
                            }
                        }
                    }
                }
                2 -> {
                    // TAB 2: TTS Engines Selection
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Installed System Text-to-Speech Engines",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Choose which TTS speech synthesis engine powers the read-aloud reader.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (availableEngines.isEmpty()) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Default System Speech Engine", fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Android default speech synthesis service is currently active.", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        } else {
                            LazyColumn(modifier = Modifier.weight(1f)) {
                                items(availableEngines, key = { it.packageName }) { engineOption ->
                                    val isSelected = selectedEnginePackage == engineOption.packageName || (selectedEnginePackage == null && engineOption.isCurrent)
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable { onSwitchEngine(engineOption.packageName) }
                                            .testTag("tts_engine_${engineOption.packageName}"),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                RadioButton(
                                                    selected = isSelected,
                                                    onClick = { onSwitchEngine(engineOption.packageName) }
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(text = engineOption.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                                    Text(text = engineOption.packageName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }

                                            if (isSelected) {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primary,
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = "Active",
                                                        color = Color.White,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Open Android TTS Settings Button
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent("com.android.settings.TTS_SETTINGS")
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    try {
                                        val intent = Intent(Settings.ACTION_SETTINGS)
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Open Settings -> System -> Text-to-Speech", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("open_system_tts_settings_button")
                        ) {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Android System TTS Engine Settings")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceItemCard(
    voice: TtsVoiceOption,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onTest: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (voice.isBangla) Color(0xFF059669).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (voice.isBangla) "বাং" else voice.locale.language.uppercase().take(2),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (voice.isBangla) Color(0xFF059669) else MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = voice.displayName.substringBefore(" • "),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (voice.isBangla) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(color = Color(0xFF059669).copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                                Text(text = "বাংলা", color = Color(0xFF059669), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = when (voice.gender) {
                                "Female" -> Color(0xFFFCE7F3)
                                "Male" -> Color(0xFFDBEAFE)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = when (voice.gender) {
                                    "Female" -> "♀ Female"
                                    "Male" -> "♂ Male"
                                    else -> "Neutral"
                                },
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (voice.gender) {
                                    "Female" -> Color(0xFFBE185D)
                                    "Male" -> Color(0xFF1D4ED8)
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = voice.languageDisplay,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onTest, modifier = Modifier.size(32.dp)) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Test voice", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }

                if (isSelected) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(imageVector = Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

/**
 * Animated audio waveform visualizer bars reflecting TTS speech playback.
 */
@Composable
fun AudioWaveformVisualizer(isPlaying: Boolean, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")

    val h1 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = if (isPlaying) 0.95f else 0.25f,
        animationSpec = infiniteRepeatable(tween(350), repeatMode = RepeatMode.Reverse),
        label = "h1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = if (isPlaying) 1.0f else 0.35f,
        animationSpec = infiniteRepeatable(tween(260), repeatMode = RepeatMode.Reverse),
        label = "h2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = if (isPlaying) 0.85f else 0.3f,
        animationSpec = infiniteRepeatable(tween(420), repeatMode = RepeatMode.Reverse),
        label = "h3"
    )
    val h4 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = if (isPlaying) 0.75f else 0.2f,
        animationSpec = infiniteRepeatable(tween(310), repeatMode = RepeatMode.Reverse),
        label = "h4"
    )

    Row(
        modifier = modifier.height(20.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(h1, h2, h3, h4).forEach { heightFraction ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(heightFraction)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
            )
        }
    }
}
