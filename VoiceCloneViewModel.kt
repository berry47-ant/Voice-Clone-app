package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.GeminiTtsService
import com.example.audio.AudioPlayerManager
import com.example.audio.AudioRecorderManager
import com.example.audio.PcmToWavUtils
import com.example.audio.PlaybackUiState
import com.example.model.AudioClip
import com.example.model.RecordingStatus
import com.example.model.TtsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class VoiceSampleInfo(
    val hasSample: Boolean = false,
    val filePath: String? = null,
    val durationSeconds: Int = 0,
    val fileSizeKb: Long = 0L
)

class VoiceCloneViewModel(application: Application) : AndroidViewModel(application) {

    private val recorderManager = AudioRecorderManager(application)
    private val playerManager = AudioPlayerManager(application)
    private val ttsService = GeminiTtsService(application)

    // Current Tab: 0 = Record Voice, 1 = Text to Speech, 2 = History
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Recording States
    val isRecording: StateFlow<Boolean> = recorderManager.isRecording
    val recordedDurationMs: StateFlow<Long> = recorderManager.recordedDurationMs
    val currentAmplitude: StateFlow<Float> = recorderManager.currentAmplitude

    private val _recordingStatus = MutableStateFlow(RecordingStatus.IDLE)
    val recordingStatus: StateFlow<RecordingStatus> = _recordingStatus.asStateFlow()

    private val _voiceSampleInfo = MutableStateFlow(VoiceSampleInfo())
    val voiceSampleInfo: StateFlow<VoiceSampleInfo> = _voiceSampleInfo.asStateFlow()

    // TTS & Voice Cloning States
    private val _myanmarInputText = MutableStateFlow(
        "မင်္ဂလာပါခင်ဗျာ၊ နေကောင်းကြပါရဲ့လား။ ဒီနေ့မှာ အားလုံးပဲ စိတ်ချမ်းသာ ကိုယ်ကျန်းမာ ရှိကြပါစေ။"
    )
    val myanmarInputText: StateFlow<String> = _myanmarInputText.asStateFlow()

    private val _selectedModel = MutableStateFlow(GeminiTtsService.DEFAULT_MODEL)
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _selectedTone = MutableStateFlow("Natural")
    val selectedTone: StateFlow<String> = _selectedTone.asStateFlow()

    private val _selectedVoiceName = MutableStateFlow("Aoede")
    val selectedVoiceName: StateFlow<String> = _selectedVoiceName.asStateFlow()

    private val _ttsState = MutableStateFlow<TtsState>(TtsState.Idle)
    val ttsState: StateFlow<TtsState> = _ttsState.asStateFlow()

    private val _currentGeneratedClip = MutableStateFlow<AudioClip?>(null)
    val currentGeneratedClip: StateFlow<AudioClip?> = _currentGeneratedClip.asStateFlow()

    private val _historyClips = MutableStateFlow<List<AudioClip>>(emptyList())
    val historyClips: StateFlow<List<AudioClip>> = _historyClips.asStateFlow()

    // Player State
    val playbackState: StateFlow<PlaybackUiState> = playerManager.uiState

    // API Key State
    private val _customApiKey = MutableStateFlow("")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private val _showApiKeyDialog = MutableStateFlow(false)
    val showApiKeyDialog: StateFlow<Boolean> = _showApiKeyDialog.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    val effectiveApiKey: StateFlow<String> = combine(_customApiKey) { custom ->
        val customKey = custom.firstOrNull().orEmpty()
        if (customKey.isNotBlank()) {
            customKey
        } else {
            val buildKey = try {
                BuildConfig.GEMINI_API_KEY
            } catch (e: Exception) {
                ""
            }
            if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") buildKey else ""
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    init {
        refreshVoiceSampleState()
    }

    fun selectTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    fun setMyanmarInputText(text: String) {
        _myanmarInputText.value = text
    }

    fun setSelectedModel(model: String) {
        _selectedModel.value = model
    }

    fun setSelectedTone(tone: String) {
        _selectedTone.value = tone
    }

    fun setSelectedVoiceName(voice: String) {
        _selectedVoiceName.value = voice
    }

    fun setShowApiKeyDialog(show: Boolean) {
        _showApiKeyDialog.value = show
    }

    fun updateCustomApiKey(key: String) {
        _customApiKey.value = key.trim()
        showToast("API key updated")
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun refreshVoiceSampleState() {
        val file = recorderManager.voiceSampleFile
        if (file.exists() && file.length() > 1024) {
            _voiceSampleInfo.value = VoiceSampleInfo(
                hasSample = true,
                filePath = file.absolutePath,
                durationSeconds = AudioRecorderManager.TARGET_DURATION_SECONDS,
                fileSizeKb = file.length() / 1024
            )
            _recordingStatus.value = RecordingStatus.COMPLETED
        } else {
            _voiceSampleInfo.value = VoiceSampleInfo(hasSample = false)
            _recordingStatus.value = RecordingStatus.IDLE
        }
    }

    fun startRecording() {
        // Stop any current playback
        playerManager.stop()

        _recordingStatus.value = RecordingStatus.RECORDING
        recorderManager.startRecording(
            onFinished = { file ->
                viewModelScope.launch {
                    refreshVoiceSampleState()
                    showToast("20-second voice sample recorded successfully!")
                }
            },
            onError = { error ->
                viewModelScope.launch {
                    _recordingStatus.value = RecordingStatus.IDLE
                    showToast("Recording error: $error")
                }
            }
        )
    }

    fun stopRecordingManually() {
        val file = recorderManager.stopRecording()
        if (file != null && file.exists() && file.length() > 1024) {
            refreshVoiceSampleState()
            showToast("Voice sample saved (${file.length() / 1024} KB)")
        } else {
            _recordingStatus.value = RecordingStatus.IDLE
            showToast("Recording stopped")
        }
    }

    fun deleteVoiceSample() {
        playerManager.stop()
        recorderManager.cancelRecording()
        val file = recorderManager.voiceSampleFile
        if (file.exists()) file.delete()
        refreshVoiceSampleState()
        showToast("Voice sample deleted")
    }

    fun playVoiceSample() {
        val path = _voiceSampleInfo.value.filePath
        if (!path.isNullOrEmpty()) {
            playerManager.play(path)
        }
    }

    fun playOrPauseClip(filePath: String) {
        playerManager.play(filePath)
    }

    fun pausePlayback() {
        playerManager.pause()
    }

    fun resumePlayback() {
        playerManager.resume()
    }

    fun seekPlayback(posMs: Long) {
        playerManager.seekTo(posMs)
    }

    fun setPlaybackSpeed(speed: Float) {
        playerManager.setSpeed(speed)
    }

    fun stopPlayback() {
        playerManager.stop()
    }

    fun generateClonedSpeech() {
        val text = _myanmarInputText.value.trim()
        if (text.isBlank()) {
            showToast("Please enter Myanmar text")
            return
        }

        val apiKey = effectiveApiKey.value
        if (apiKey.isBlank()) {
            _showApiKeyDialog.value = true
            showToast("Please enter Gemini API Key to continue")
            return
        }

        val sampleFile = if (_voiceSampleInfo.value.hasSample) {
            File(_voiceSampleInfo.value.filePath ?: "")
        } else {
            null
        }

        _ttsState.value = TtsState.Loading("Cloning Myanmar voice with ${selectedModel.value}...")
        viewModelScope.launch {
            val result = ttsService.generateClonedSpeech(
                apiKey = apiKey,
                myanmarText = text,
                voiceSampleFile = sampleFile,
                model = _selectedModel.value,
                voiceStyle = _selectedTone.value,
                prebuiltVoice = _selectedVoiceName.value
            )

            result.fold(
                onSuccess = { clip ->
                    _currentGeneratedClip.value = clip
                    _historyClips.value = listOf(clip) + _historyClips.value
                    _ttsState.value = TtsState.Success(clip)
                    // Auto-play the synthesized speech
                    playerManager.play(clip.filePath)
                    showToast("Audio generated successfully!")
                },
                onFailure = { error ->
                    _ttsState.value = TtsState.Error(
                        message = error.localizedMessage ?: "Generation failed",
                        details = error.message
                    )
                }
            )
        }
    }

    fun downloadClip(context: Context, clip: AudioClip) {
        viewModelScope.launch {
            val file = File(clip.filePath)
            if (!file.exists()) {
                showToast("File no longer exists")
                return@launch
            }

            val sanitizedTitle = clip.title
                .replace(Regex("[^a-zA-Z0-9\\u1000-\\u109F]"), "_")
                .take(30)
            val fileName = "Myanmar_Voice_${sanitizedTitle}_${clip.id}.wav"

            val result = PcmToWavUtils.exportAudioToDownloads(context, file, fileName)
            result.fold(
                onSuccess = {
                    showToast("Downloaded to Downloads/MyanmarVoiceClone folder!")
                },
                onFailure = { err ->
                    showToast("Download failed: ${err.message}")
                }
            )
        }
    }

    fun shareClip(context: Context, clip: AudioClip) {
        val file = File(clip.filePath)
        if (!file.exists()) {
            showToast("Audio file not found")
            return
        }
        try {
            val shareIntent = PcmToWavUtils.createShareIntent(context, file, clip.title)
            val chooser = android.content.Intent.createChooser(shareIntent, "Share Myanmar Cloned Voice")
            chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            showToast("Share failed: ${e.message}")
        }
    }

    override fun onCleared() {
        super.onCleared()
        recorderManager.cancelRecording()
        playerManager.release()
    }
}
