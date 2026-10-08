package com.example.ui.tabs

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.audio.AudioRecorderManager
import com.example.model.DEFAULT_MYANMAR_SCRIPTS
import com.example.model.RecordingStatus
import com.example.model.SampleScript
import com.example.ui.components.AudioWaveformVisualizer
import com.example.ui.theme.RecordRed
import com.example.ui.theme.WaveformGreen
import com.example.viewmodel.VoiceCloneViewModel
import java.util.Locale

@Composable
fun RecordVoiceTab(
    viewModel: VoiceCloneViewModel,
    onNavigateToTts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isRecording by viewModel.isRecording.collectAsState()
    val recordedDurationMs by viewModel.recordedDurationMs.collectAsState()
    val currentAmplitude by viewModel.currentAmplitude.collectAsState()
    val voiceSampleInfo by viewModel.voiceSampleInfo.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (isGranted) {
            viewModel.startRecording()
        } else {
            viewModel.showToast("Microphone permission is required to record voice sample.")
        }
    }

    var selectedScript by remember { mutableStateOf(DEFAULT_MYANMAR_SCRIPTS.first()) }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Info Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice Clone",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "အသံနမူနာ သွင်းယူခြင်း (20s Sample)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Record 20 seconds of clear Myanmar speech to clone your voice timbre, pitch & accent.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Suggested Reading Script Selector
        Text(
            text = "Suggested Myanmar Script to Read / ဖတ်ရှုရန် စာသား",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Script category chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DEFAULT_MYANMAR_SCRIPTS.forEach { script ->
                val isSelected = selectedScript.id == script.id
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedScript = script },
                    label = { Text(script.category) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("script_chip_${script.id}")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Script Display Card
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = selectedScript.titleMy,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = selectedScript.text,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 28.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Recording Visualizer & Countdown Section
        val elapsedSec = (recordedDurationMs / 1000L).coerceIn(0L, 20L)
        val progressFraction = (recordedDurationMs.toFloat() / AudioRecorderManager.MAX_RECORD_TIME_MS).coerceIn(0f, 1f)

        // Pulsating recording ring animation
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.14f,
            animationSpec = infiniteRepeatable(
                animation = tween(800),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_scale"
        )

        val recordBgColor by animateColorAsState(
            targetValue = if (isRecording) RecordRed else MaterialTheme.colorScheme.primaryContainer,
            label = "rec_bg_color"
        )

        // Circular Recording Pod
        Box(
            modifier = Modifier
                .size(170.dp)
                .clip(CircleShape)
                .then(
                    if (isRecording) Modifier.scale(pulseScale) else Modifier
                )
                .background(recordBgColor.copy(alpha = if (isRecording) 0.18f else 0.4f))
                .border(
                    BorderStroke(
                        width = if (isRecording) 3.dp else 2.dp,
                        color = if (isRecording) RecordRed else MaterialTheme.colorScheme.primary
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Mic else if (voiceSampleInfo.hasSample) Icons.Default.CheckCircle else Icons.Default.Mic,
                    contentDescription = "Mic Indicator",
                    tint = if (isRecording) RecordRed else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(42.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Time text
                val timeDisplay = if (isRecording) {
                    String.format(Locale.getDefault(), "00:%02d / 00:20", elapsedSec)
                } else if (voiceSampleInfo.hasSample) {
                    "00:20 Ready"
                } else {
                    "00:00 / 00:20"
                }

                Text(
                    text = timeDisplay,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isRecording) RecordRed else MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = if (isRecording) "Recording..." else if (voiceSampleInfo.hasSample) "Voice Saved" else "Tap to Record",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Progress bar for 20 seconds
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (isRecording) RecordRed else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dynamic Waveform visualizer
        AudioWaveformVisualizer(
            isRecording = isRecording,
            amplitude = currentAmplitude,
            activeColor = RecordRed,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Primary Control Buttons
        if (!isRecording) {
            if (!voiceSampleInfo.hasSample) {
                // Initial Record Button
                Button(
                    onClick = {
                        if (hasMicPermission) {
                            viewModel.startRecording()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("start_record_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.Mic, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Start 20s Recording (အသံသွင်းမည်)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                // Sample is recorded: Preview & Re-record options
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(WaveformGreen.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Sample Ready",
                                        tint = WaveformGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Voice Sample Active",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "20 seconds • ${voiceSampleInfo.fileSizeKb} KB",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Delete button
                            IconButton(
                                onClick = { viewModel.deleteVoiceSample() },
                                modifier = Modifier.testTag("delete_sample_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Sample",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Preview Player Row
                        val isPlayingSample = playbackState.isPlaying &&
                                playbackState.currentAudioPath == voiceSampleInfo.filePath

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { viewModel.playVoiceSample() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("play_sample_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlayingSample) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isPlayingSample) "Pause Sample" else "Preview Sample")
                            }

                            OutlinedButton(
                                onClick = {
                                    if (hasMicPermission) {
                                        viewModel.startRecording()
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("rerecord_sample_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Re-record")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // CTA to Tab 2: Text to Speech
                Button(
                    onClick = onNavigateToTts,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("go_to_tts_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Next: Clone Voice & Text to Speech",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
        } else {
            // While Recording: Stop Early button
            Button(
                onClick = { viewModel.stopRecordingManually() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("stop_recording_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RecordRed)
            ) {
                Icon(imageVector = Icons.Default.Stop, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Stop & Save Sample (ရပ်တန့်မည်)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
