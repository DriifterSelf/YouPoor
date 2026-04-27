package com.voidfilio.youpoor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.voidfilio.youpoor.audio.AudioPlayer
import com.voidfilio.youpoor.data.models.Download
import com.voidfilio.youpoor.data.repository.MusicRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerUiState(
    val currentTrack: Download? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val downloads: List<Download> = emptyList(),
)

class PlayerViewModel(
    private val repository: MusicRepository,
    private val audioPlayer: AudioPlayer,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    val downloads: Flow<List<Download>> = repository.getDownloads()

    private var positionJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                val d = audioPlayer.getDuration()
                _uiState.value = _uiState.value.copy(
                    duration = if (d > 0) d else _uiState.value.duration,
                )
            }
        }
    }

    init {
        audioPlayer.getPlayer().addListener(playerListener)

        viewModelScope.launch {
            repository.getDownloads().collect { downloads ->
                _uiState.value = _uiState.value.copy(downloads = downloads)
            }
        }
    }

    fun playTrack(track: Download) {
        audioPlayer.playTrack(track)
        _uiState.value = _uiState.value.copy(
            currentTrack = track,
            isPlaying = true,
            currentPosition = 0L,
            duration = (track.duration * 1000L).coerceAtLeast(0L),
        )
        startPositionUpdates()
    }

    fun pauseTrack() {
        audioPlayer.pause()
        _uiState.value = _uiState.value.copy(isPlaying = false)
    }

    fun resumeTrack() {
        audioPlayer.resume()
        _uiState.value = _uiState.value.copy(isPlaying = true)
        startPositionUpdates()
    }

    fun seekTo(position: Long) {
        audioPlayer.seekTo(position)
        _uiState.value = _uiState.value.copy(currentPosition = position)
    }

    private fun startPositionUpdates() {
        positionJob?.cancel()
        positionJob = viewModelScope.launch {
            while (isActive) {
                val pos = audioPlayer.getCurrentPosition()
                val dur = audioPlayer.getDuration()
                _uiState.value = _uiState.value.copy(
                    currentPosition = if (pos >= 0) pos else 0L,
                    duration = if (dur > 0) dur else _uiState.value.duration,
                )
                delay(500)
            }
        }
    }

    override fun onCleared() {
        positionJob?.cancel()
        audioPlayer.getPlayer().removeListener(playerListener)
        audioPlayer.release()
        super.onCleared()
    }

    companion object {
        fun factory(repository: MusicRepository, audioPlayer: AudioPlayer): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PlayerViewModel(repository, audioPlayer) as T
                }
            }
        }
    }
}
