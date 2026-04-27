package com.voidfilio.youpoor.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.voidfilio.youpoor.data.models.Download
import com.voidfilio.youpoor.viewmodel.PlayerUiState
import com.voidfilio.youpoor.viewmodel.PlayerViewModel

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    downloadViewModel: androidx.lifecycle.ViewModel? = null,
    onDownloadClick: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState  by viewModel.uiState.collectAsState()
    val downloads by viewModel.downloads.collectAsState(initial = emptyList())
    val downloadUiState by (downloadViewModel as? com.voidfilio.youpoor.viewmodel.DownloadViewModel)?.uiState?.collectAsState() ?: remember { mutableStateOf(com.voidfilio.youpoor.viewmodel.DownloadUiState()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        item {
            PlayerHeroPanel(uiState, viewModel)
        }

        item {
            Text(
                text     = "Library",
                style    = MaterialTheme.typography.titleMedium,
                color    = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }

        items(downloads, key = { it.downloadId }) { download ->
            DownloadCard(
                download  = download,
                isPlaying = uiState.isPlaying &&
                            uiState.currentTrack?.downloadId == download.downloadId,
                isCurrent = uiState.currentTrack?.downloadId == download.downloadId,
                isDownloading = downloadUiState.isDownloading,
                onPlayClick = { viewModel.playTrack(download) },
            )
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun PlayerHeroPanel(uiState: PlayerUiState, viewModel: PlayerViewModel) {
    val track = uiState.currentTrack

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.BottomCenter,
        ) {
            if (track?.thumbnail?.isNotEmpty() == true) {
                AsyncImage(
                    model            = track.thumbnail,
                    contentDescription = track.title,
                    modifier         = Modifier.fillMaxSize(),
                    contentScale     = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier         = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                                MaterialTheme.colorScheme.surface,
                            ),
                        )
                    ),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 4.dp, bottom = 8.dp),
        ) {
            Text(
                text     = track?.title ?: "No track selected",
                style    = MaterialTheme.typography.titleLarge,
                color    = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text  = track?.artist ?: "Choose a track from your library below",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        ) {
            Slider(
                value       = uiState.currentPosition.toFloat(),
                onValueChange = { viewModel.seekTo(it.toLong()) },
                valueRange  = 0f..uiState.duration.toFloat().coerceAtLeast(1f),
                modifier    = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor            = MaterialTheme.colorScheme.primary,
                    activeTrackColor      = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor    = MaterialTheme.colorScheme.outline,
                ),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text  = formatMillis(uiState.currentPosition),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text  = formatMillis(uiState.duration),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick  = { /* Previous */ },
                modifier = Modifier.size(52.dp),
            ) {
                Icon(
                    Icons.Filled.SkipPrevious,
                    contentDescription = "Previous",
                    tint     = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(30.dp),
                )
            }

            Spacer(Modifier.width(24.dp))

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        color  = MaterialTheme.colorScheme.primary,
                        shape  = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(
                    onClick  = {
                        if (uiState.isPlaying) viewModel.pauseTrack()
                        else viewModel.resumeTrack()
                    },
                    modifier = Modifier.size(64.dp),
                ) {
                    Icon(
                        imageVector      = if (uiState.isPlaying) Icons.Filled.Pause
                                           else Icons.Filled.PlayArrow,
                        contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                        tint             = MaterialTheme.colorScheme.onPrimary,
                        modifier         = Modifier.size(34.dp),
                    )
                }
            }

            Spacer(Modifier.width(24.dp))

            IconButton(
                onClick  = { /* Next */ },
                modifier = Modifier.size(52.dp),
            ) {
                Icon(
                    Icons.Filled.SkipNext,
                    contentDescription = "Next",
                    tint     = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(30.dp),
                )
            }
        }
    }
}

@Composable
private fun DownloadCard(
    download      : Download,
    isPlaying     : Boolean,
    isCurrent     : Boolean,
    isDownloading : Boolean = false,
    onPlayClick   : () -> Unit,
) {
    val bgColor = if (isCurrent)
        MaterialTheme.colorScheme.primaryContainer
    else if (isDownloading)
        MaterialTheme.colorScheme.surfaceContainer
    else
        MaterialTheme.colorScheme.surface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncImage(
            model            = download.thumbnail,
            contentDescription = null,
            modifier         = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer),
            contentScale     = ContentScale.Crop,
        )

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text     = download.title,
                    style    = MaterialTheme.typography.labelLarge,
                    color    = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer
                               else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (isDownloading) {
                    Text(
                        text  = "⬇",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text  = download.artist,
                style = MaterialTheme.typography.bodySmall,
                color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text  = formatSeconds(download.duration),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        if (isPlaying) {
            EqualizerBars(
                modifier = Modifier.size(28.dp),
                color    = MaterialTheme.colorScheme.primary,
            )
        } else {
            IconButton(
                onClick  = onPlayClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color  = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape  = CircleShape,
                    ),
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "Play",
                    tint     = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun EqualizerBars(modifier: Modifier = Modifier, color: androidx.compose.ui.graphics.Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "eq")

    val bar1 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue  = 1.0f,
        animationSpec = infiniteRepeatable(
            animation   = tween(400, easing = LinearEasing),
            repeatMode  = RepeatMode.Reverse,
        ),
        label = "bar1",
    )
    val bar2 by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue  = 0.2f,
        animationSpec = infiniteRepeatable(
            animation   = tween(300, easing = LinearEasing),
            repeatMode  = RepeatMode.Reverse,
        ),
        label = "bar2",
    )
    val bar3 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue  = 0.9f,
        animationSpec = infiniteRepeatable(
            animation   = tween(500, easing = LinearEasing),
            repeatMode  = RepeatMode.Reverse,
        ),
        label = "bar3",
    )

    Row(
        modifier  = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        listOf(bar1, bar2, bar3).forEach { scale ->
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleY = scale
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    }
                    .background(color, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)),
            )
        }
    }
}

private fun formatMillis(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes      = totalSeconds / 60
    val seconds      = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

private fun formatSeconds(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}
