package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
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

data class PlaybackUiState(
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val currentAudioPath: String? = null
)

class AudioPlayerManager(private val context: Context) {

    companion object {
        private const val TAG = "AudioPlayerManager"
    }

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _uiState = MutableStateFlow(PlaybackUiState())
    val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()

    fun play(filePath: String, speed: Float = _uiState.value.playbackSpeed) {
        val file = File(filePath)
        if (!file.exists()) {
            Log.e(TAG, "File not found: $filePath")
            return
        }

        try {
            // If already playing the same file, toggle pause/play
            if (mediaPlayer != null && _uiState.value.currentAudioPath == filePath) {
                if (_uiState.value.isPlaying) {
                    pause()
                } else {
                    resume()
                }
                return
            }

            stopInternal()

            val player = MediaPlayer()
            player.setDataSource(file.absolutePath)
            player.prepare()

            val duration = player.duration.toLong().coerceAtLeast(0L)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    val params = PlaybackParams().apply { this.speed = speed }
                    player.playbackParams = params
                } catch (e: Exception) {
                    Log.w(TAG, "Could not set playback speed: ${e.message}")
                }
            }

            player.setOnCompletionListener {
                _uiState.value = _uiState.value.copy(
                    isPlaying = false,
                    currentPositionMs = duration
                )
                stopProgressTracking()
            }

            player.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
                stopInternal()
                true
            }

            player.start()
            mediaPlayer = player

            _uiState.value = PlaybackUiState(
                isPlaying = true,
                currentPositionMs = 0L,
                durationMs = duration,
                playbackSpeed = speed,
                currentAudioPath = filePath
            )

            startProgressTracking()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play audio: ${e.message}", e)
            stopInternal()
        }
    }

    fun pause() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    _uiState.value = _uiState.value.copy(isPlaying = false)
                    stopProgressTracking()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing: ${e.message}")
        }
    }

    fun resume() {
        try {
            mediaPlayer?.let {
                it.start()
                _uiState.value = _uiState.value.copy(isPlaying = true)
                startProgressTracking()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming: ${e.message}")
        }
    }

    fun seekTo(positionMs: Long) {
        try {
            mediaPlayer?.let {
                it.seekTo(positionMs.toInt())
                _uiState.value = _uiState.value.copy(currentPositionMs = positionMs)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking: ${e.message}")
        }
    }

    fun setSpeed(speed: Float) {
        _uiState.value = _uiState.value.copy(playbackSpeed = speed)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mediaPlayer?.let {
                    val wasPlaying = it.isPlaying
                    val params = it.playbackParams
                    params.speed = speed
                    it.playbackParams = params
                    if (!wasPlaying && it.isPlaying) {
                        it.pause()
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to apply playback speed: ${e.message}")
            }
        }
    }

    fun stop() {
        stopInternal()
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && _uiState.value.isPlaying) {
                try {
                    mediaPlayer?.let {
                        if (it.isPlaying) {
                            val pos = it.currentPosition.toLong()
                            val dur = it.duration.toLong().coerceAtLeast(0L)
                            _uiState.value = _uiState.value.copy(
                                currentPositionMs = pos,
                                durationMs = dur
                            )
                        }
                    }
                } catch (e: Exception) {
                    // Ignore transient position query errors
                }
                delay(100)
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun stopInternal() {
        stopProgressTracking()
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                reset()
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing player: ${e.message}")
        } finally {
            mediaPlayer = null
            _uiState.value = _uiState.value.copy(
                isPlaying = false,
                currentPositionMs = 0L
            )
        }
    }

    fun release() {
        stopInternal()
    }
}
