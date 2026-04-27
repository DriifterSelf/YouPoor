package com.voidfilio.youpoor.audio

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.voidfilio.youpoor.data.models.Download
import java.io.File

class AudioPlayer(context: Context) {
    private val player = ExoPlayer.Builder(context).build().apply {
        addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Log.e("AudioPlayer", "Playback error: ${error.errorCodeName} — ${error.message}", error)
            }
        })
    }

    fun playTrack(download: Download) {
        val uri = if (download.filePath.startsWith("/")) {
            Uri.fromFile(File(download.filePath))
        } else {
            Uri.parse(download.filePath)
        }
        Log.d("AudioPlayer", "playTrack uri=$uri")
        val mediaItem = MediaItem.fromUri(uri)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    fun pause() {
        player.pause()
    }

    fun resume() {
        player.play()
    }

    fun seekTo(position: Long) {
        player.seekTo(position)
    }

    fun getCurrentPosition(): Long = player.currentPosition

    fun getDuration(): Long = player.duration

    fun isPlaying(): Boolean = player.isPlaying

    fun release() {
        player.release()
    }

    fun getPlayer(): ExoPlayer = player
}
