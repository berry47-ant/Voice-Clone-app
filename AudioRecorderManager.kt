package com.example.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class AudioRecorderManager(private val context: Context) {

    companion object {
        private const val TAG = "AudioRecorderManager"
        const val TARGET_DURATION_SECONDS = 20
        const val MAX_RECORD_TIME_MS = TARGET_DURATION_SECONDS * 1000L
    }

    private var mediaRecorder: MediaRecorder? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordedDurationMs = MutableStateFlow(0L)
    val recordedDurationMs: StateFlow<Long> = _recordedDurationMs.asStateFlow()

    private val _currentAmplitude = MutableStateFlow(0f)
    val currentAmplitude: StateFlow<Float> = _currentAmplitude.asStateFlow()

    private var currentOutputFile: File? = null

    val voiceSampleFile: File
        get() {
            val audioDir = File(context.cacheDir, "audio").apply { if (!exists()) mkdirs() }
            return File(audioDir, "sample_voice_20s.m4a")
        }

    fun hasRecordedSample(): Boolean {
        val file = voiceSampleFile
        return file.exists() && file.length() > 1024
    }

    fun startRecording(onFinished: (File) -> Unit, onError: (String) -> Unit) {
        if (_isRecording.value) return

        try {
            val file = voiceSampleFile
            if (file.exists()) {
                file.delete()
            }
            currentOutputFile = file

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setAudioChannels(1)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            _isRecording.value = true
            _recordedDurationMs.value = 0L

            recordingJob?.cancel()
            recordingJob = scope.launch {
                val startTime = System.currentTimeMillis()
                while (isActive && _isRecording.value) {
                    val elapsed = System.currentTimeMillis() - startTime
                    _recordedDurationMs.value = elapsed

                    // Query amplitude for live waveform
                    try {
                        val maxAmp = mediaRecorder?.maxAmplitude ?: 0
                        // Normalize 0..32767 to 0..1
                        _currentAmplitude.value = (maxAmp / 32767f).coerceIn(0f, 1f)
                    } catch (e: Exception) {
                        _currentAmplitude.value = 0f
                    }

                    if (elapsed >= MAX_RECORD_TIME_MS) {
                        stopRecordingInternal()
                        onFinished(file)
                        break
                    }
                    delay(100)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording: ${e.message}", e)
            stopRecordingInternal()
            onError("Recording failed: ${e.message}")
        }
    }

    fun stopRecording(): File? {
        val file = currentOutputFile ?: voiceSampleFile
        stopRecordingInternal()
        return if (file.exists() && file.length() > 0) file else null
    }

    private fun stopRecordingInternal() {
        recordingJob?.cancel()
        recordingJob = null
        _isRecording.value = false
        _currentAmplitude.value = 0f

        try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (e: Exception) {
                    Log.w(TAG, "Error stopping recorder: ${e.message}")
                }
                reset()
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing recorder: ${e.message}", e)
        } finally {
            mediaRecorder = null
        }
    }

    fun cancelRecording() {
        stopRecordingInternal()
        currentOutputFile?.let {
            if (it.exists()) it.delete()
        }
        _recordedDurationMs.value = 0L
    }
}
