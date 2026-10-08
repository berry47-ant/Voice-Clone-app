package com.example.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.audio.PlaybackUiState
import com.example.model.AudioClip
import java.util.Locale

@Composable
fun AudioPlayerCard(
    clip: AudioClip,
    playbackState: PlaybackUiState,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onDownload: (Context) -> Unit,
    onShare: (Context) -> Unit,
    modifier: Modifier = Modifier,
    isCurrentClipActive: Boolean = true
) {
    val context = LocalContext.current
    val isPlaying = playbackState.isPlaying && isCurrentClipActive
    val currentPosition = if (isCurrentClipActive) playbackState.currentPositionMs else 0L
    val duration = if (isCurrentClipActive && playbackState.durationMs > 0) {
        playbackState.durationMs
    } else {
        clip.durationMs.coerceAtLeast(1000L)
    }

    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragSliderValue by remember { mutableFloatStateOf(0f) }

    val sliderProgress = if (isDraggingSlider) {
        dragSliderValue
    } else {
        if (duration > 0) (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("audio_player_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header: Icon + Title + Model badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Audio Player",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = clip.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (clip.isCloned) "Cloned Myanmar Voice" else "Voice Sample",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = clip.modelUsed,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Slider progress bar
            Slider(
                value = sliderProgress,
                onValueChange = {
                    isDraggingSlider = true
                    dragSliderValue = it
                },
                onValueChangeFinished = {
                    val targetMs = (dragSliderValue * duration).toLong()
                    onSeek(targetMs)
                    isDraggingSlider = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("player_progress_slider"),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            // Time Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val displayCurrent = if (isDraggingSlider) {
                    (dragSliderValue * duration).toLong()
                } else {
                    currentPosition
                }
                Text(
                    text = formatTimeMs(displayCurrent),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatTimeMs(duration),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Playback Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rewind 5s
                IconButton(
                    onClick = {
                        val newPos = (currentPosition - 5000L).coerceAtLeast(0L)
                        onSeek(newPos)
                    },
                    modifier = Modifier.testTag("player_rewind_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FastRewind,
                        contentDescription = "Rewind 5s",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Play / Pause Button
                FilledIconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("player_play_pause_button"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Fast forward 5s
                IconButton(
                    onClick = {
                        val newPos = (currentPosition + 5000L).coerceAtMost(duration)
                        onSeek(newPos)
                    },
                    modifier = Modifier.testTag("player_fast_forward_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Fast Forward 5s",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Speed Selector & Download / Share Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Playback speed chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f)
                    speeds.forEach { speed ->
                        val isSelected = playbackState.playbackSpeed == speed
                        SuggestionChip(
                            onClick = { onSpeedChange(speed) },
                            label = {
                                Text(
                                    text = "${speed}x",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                labelColor = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            ),
                            border = null,
                            modifier = Modifier.testTag("speed_chip_${speed}")
                        )
                    }
                }

                // Download & Share buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Download Button
                    OutlinedButton(
                        onClick = { onDownload(context) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("download_audio_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download Audio",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download", style = MaterialTheme.typography.labelMedium)
                    }

                    // Share Button
                    IconButton(
                        onClick = { onShare(context) },
                        modifier = Modifier.testTag("share_audio_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Audio",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

private fun formatTimeMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
