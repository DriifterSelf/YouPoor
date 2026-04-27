package com.voidfilio.youpoor.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.voidfilio.youpoor.audio.AudioPlayer

@Composable
fun AudioPlayerView(audioPlayer: AudioPlayer) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
    ) {
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    player = audioPlayer.getPlayer()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
        )
    }
}
