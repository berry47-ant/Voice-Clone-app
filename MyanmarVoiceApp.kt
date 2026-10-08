package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.ApiKeyDialog
import com.example.ui.tabs.RecordVoiceTab
import com.example.ui.tabs.TextToSpeechTab
import com.example.ui.theme.WaveformGreen
import com.example.viewmodel.VoiceCloneViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyanmarVoiceApp(
    viewModel: VoiceCloneViewModel = viewModel()
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val showApiKeyDialog by viewModel.showApiKeyDialog.collectAsState()
    val customKey by viewModel.customApiKey.collectAsState()
    val effectiveKey by viewModel.effectiveApiKey.collectAsState()
    val voiceSampleInfo by viewModel.voiceSampleInfo.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "Myanmar Voice Clone",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "မြန်မာ့အသံတု နည်းပညာ (Gemini TTS)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    // API Key Settings action button
                    IconButton(
                        onClick = { viewModel.setShowApiKeyDialog(true) },
                        modifier = Modifier.testTag("open_api_key_dialog_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (effectiveKey.isBlank()) {
                                    Badge(containerColor = MaterialTheme.colorScheme.error)
                                } else {
                                    Badge(containerColor = WaveformGreen)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "API Key Configuration",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                // Tab 1: Record Voice
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (voiceSampleInfo.hasSample) {
                                    Badge(
                                        containerColor = WaveformGreen,
                                        modifier = Modifier.size(8.dp)
                                    )
                                }
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Mic, contentDescription = "Tab 1: Record Voice")
                        }
                    },
                    label = { Text("1. Record Voice") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("tab_record_voice")
                )

                // Tab 2: Text to Speech
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    icon = {
                        Icon(imageVector = Icons.Default.RecordVoiceOver, contentDescription = "Tab 2: Text to Speech")
                    },
                    label = { Text("2. Text to Speech") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("tab_text_to_speech")
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Secondary Quick Tab Indicator
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = {
                        Text(
                            text = "Tab 1: Record Voice",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("top_tab_record")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = {
                        Text(
                            text = "Tab 2: Text to Speech",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("top_tab_tts")
                )
            }

            // Tab Screens
            when (selectedTab) {
                0 -> RecordVoiceTab(
                    viewModel = viewModel,
                    onNavigateToTts = { viewModel.selectTab(1) },
                    modifier = Modifier.fillMaxSize()
                )
                1 -> TextToSpeechTab(
                    viewModel = viewModel,
                    onNavigateToRecord = { viewModel.selectTab(0) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // API Key Dialog
        if (showApiKeyDialog) {
            ApiKeyDialog(
                currentKey = if (customKey.isNotBlank()) customKey else effectiveKey,
                onSave = { newKey -> viewModel.updateCustomApiKey(newKey) },
                onDismiss = { viewModel.setShowApiKeyDialog(false) }
            )
        }
    }
}
