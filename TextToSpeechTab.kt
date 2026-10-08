package com.example.ui.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.api.GeminiTtsService
import com.example.model.AudioClip
import com.example.model.QUICK_TTS_PRESETS
import com.example.model.TtsState
import com.example.ui.components.AudioPlayerCard
import com.example.ui.theme.Amber60
import com.example.ui.theme.WaveformGreen
import com.example.viewmodel.VoiceCloneViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextToSpeechTab(
    viewModel: VoiceCloneViewModel,
    onNavigateToRecord: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val voiceSampleInfo by viewModel.voiceSampleInfo.collectAsState()
    val myanmarText by viewModel.myanmarInputText.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val selectedTone by viewModel.selectedTone.collectAsState()
    val selectedVoiceName by viewModel.selectedVoiceName.collectAsState()
    val ttsState by viewModel.ttsState.collectAsState()
    val currentClip by viewModel.currentGeneratedClip.collectAsState()
    val historyClips by viewModel.historyClips.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val effectiveKey by viewModel.effectiveApiKey.collectAsState()

    val scrollState = rememberScrollState()

    var showAdvancedSettings by remember { mutableStateOf(false) }
    var modelDropdownExpanded by remember { mutableStateOf(false) }

    val models = listOf(
        GeminiTtsService.DEFAULT_MODEL,
        GeminiTtsService.FALLBACK_MODEL
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Cloned Voice Source Status Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { if (!voiceSampleInfo.hasSample) onNavigateToRecord() }
                .testTag("voice_source_status_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (voiceSampleInfo.hasSample) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (voiceSampleInfo.hasSample) WaveformGreen else Amber60
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (voiceSampleInfo.hasSample) Icons.Default.RecordVoiceOver else Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (voiceSampleInfo.hasSample) {
                            "Voice Clone Active (20s Sample)"
                        } else {
                            "No Voice Sample (Using Default Voice)"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (voiceSampleInfo.hasSample) {
                            "Speech will be cloned from your recorded Myanmar audio sample."
                        } else {
                            "Tap here to record a 20-second sample in Tab 1 for cloning."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!voiceSampleInfo.hasSample) {
                    Button(
                        onClick = onNavigateToRecord,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = ButtonDefaults.TextButtonContentPadding,
                        modifier = Modifier.testTag("record_sample_prompt_button")
                    ) {
                        Icon(imageVector = Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Record", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // API Key missing warning if needed
        if (effectiveKey.isBlank()) {
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setShowApiKeyDialog(true) }
                    .testTag("api_key_warning_banner"),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Gemini API Key Required",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Tap here to configure your Gemini API Key to enable TTS.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // 2. Myanmar Text Input Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Enter Myanmar Text / ဖတ်ကြားစေလိုသော စာသား",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "${myanmarText.length} chars",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = myanmarText,
            onValueChange = { viewModel.setMyanmarInputText(it) },
            placeholder = {
                Text("မြန်မာဘာသာဖြင့် စာသား ရိုက်ထည့်ပါ...")
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .testTag("myanmar_text_input"),
            shape = RoundedCornerShape(16.dp),
            trailingIcon = {
                if (myanmarText.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.setMyanmarInputText("") },
                        modifier = Modifier.testTag("clear_text_button")
                    ) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear text")
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Preset Chips for Easy Testing
        Text(
            text = "Quick Myanmar Presets (စမ်းသပ်ရန် စာသားများ)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(QUICK_TTS_PRESETS) { preset ->
                SuggestionChip(
                    onClick = { viewModel.setMyanmarInputText(preset) },
                    label = {
                        Text(
                            text = preset.take(22) + "...",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = null,
                    modifier = Modifier.testTag("preset_chip_${preset.hashCode()}")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Speech Tone and Settings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showAdvancedSettings = !showAdvancedSettings }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tone & Model Options ($selectedModel)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = if (showAdvancedSettings) "Hide" else "Show",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        AnimatedVisibility(visible = showAdvancedSettings) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // Tone Chips
                Text(
                    text = "Speaking Style / အသံဟန်ပန်:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tones = listOf("Natural", "Formal", "Friendly", "Storyteller")
                    tones.forEach { tone ->
                        FilterChip(
                            selected = selectedTone == tone,
                            onClick = { viewModel.setSelectedTone(tone) },
                            label = { Text(tone) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("tone_chip_$tone")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Gemini Model Dropdown
                ExposedDropdownMenuBox(
                    expanded = modelDropdownExpanded,
                    onExpandedChange = { modelDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedModel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Gemini TTS Model") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = modelDropdownExpanded,
                        onDismissRequest = { modelDropdownExpanded = false }
                    ) {
                        models.forEach { model ->
                            DropdownMenuItem(
                                text = { Text(model) },
                                onClick = {
                                    viewModel.setSelectedModel(model)
                                    modelDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 4. Primary Synthesis Button
        val isLoading = ttsState is TtsState.Loading

        Button(
            onClick = { viewModel.generateClonedSpeech() },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("synthesize_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Synthesizing Myanmar Speech...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (voiceSampleInfo.hasSample) {
                        "Clone Voice & Synthesize Speech"
                    } else {
                        "Synthesize Myanmar Speech"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Loading message banner
        if (ttsState is TtsState.Loading) {
            val msg = (ttsState as TtsState.Loading).message
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = msg,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Error message card
        if (ttsState is TtsState.Error) {
            val error = ttsState as TtsState.Error
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Generation Error",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = error.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 5. Audio Player Card (Active Generated Voice)
        currentClip?.let { clip ->
            Text(
                text = "Generated Myanmar Voice / အသံထွက် နားဆင်ရန်",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            AudioPlayerCard(
                clip = clip,
                playbackState = playbackState,
                onPlayPause = { viewModel.playOrPauseClip(clip.filePath) },
                onSeek = { pos -> viewModel.seekPlayback(pos) },
                onSpeedChange = { speed -> viewModel.setPlaybackSpeed(speed) },
                onDownload = { ctx -> viewModel.downloadClip(ctx, clip) },
                onShare = { ctx -> viewModel.shareClip(ctx, clip) },
                isCurrentClipActive = playbackState.currentAudioPath == clip.filePath
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // 6. Previously Generated Clips (History)
        if (historyClips.size > 1) {
            Text(
                text = "Recent Audio Generations (${historyClips.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            historyClips.drop(1).forEach { previousClip ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { viewModel.playOrPauseClip(previousClip.filePath) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = previousClip.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            Text(
                                text = "${previousClip.modelUsed} • Cloned Audio",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row {
                            IconButton(
                                onClick = { viewModel.downloadClip(context, previousClip) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Download",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
