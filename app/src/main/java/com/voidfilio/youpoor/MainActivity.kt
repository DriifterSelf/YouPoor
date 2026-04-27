package com.voidfilio.youpoor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.voidfilio.youpoor.ui.components.DownloadOverlay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.voidfilio.youpoor.audio.AudioPlayer
import com.voidfilio.youpoor.ui.screens.PlayerScreen
import com.voidfilio.youpoor.ui.screens.SearchScreen
import com.voidfilio.youpoor.ui.theme.YoupoorTheme
import com.voidfilio.youpoor.viewmodel.DownloadViewModel
import com.voidfilio.youpoor.viewmodel.PlayerViewModel
import com.voidfilio.youpoor.viewmodel.PlayerUiState
import com.voidfilio.youpoor.viewmodel.SearchViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YoupoorTheme {
                YouPoorApp(this)
            }
        }
    }
}

@Composable
fun YouPoorApp(activity: MainActivity) {
    val repository = AppModule.provideRepository(activity)
    val audioPlayer = remember { AudioPlayer(activity.applicationContext) }

    val searchViewModel: SearchViewModel = viewModel(
        factory = SearchViewModel.factory()
    )
    val playerViewModel: PlayerViewModel = viewModel(
        factory = PlayerViewModel.factory(repository, audioPlayer)
    )
    val downloadViewModel: DownloadViewModel = viewModel(
        factory = DownloadViewModel.factory(repository)
    )

    var currentTab by remember { mutableIntStateOf(0) }

    val playerUiState by playerViewModel.uiState.collectAsState()
    val downloadUiState by downloadViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(downloadUiState.lastCompletedAt) {
        val done = downloadUiState.lastCompleted
        if (done != null && downloadUiState.lastCompletedAt > 0L) {
            snackbarHostState.showSnackbar(
                message = "✅ Downloaded: ${done.title}",
                duration = androidx.compose.material3.SnackbarDuration.Short,
            )
        }
    }

    LaunchedEffect(downloadUiState.lastErrorAt) {
        val err = downloadUiState.lastError
        if (err != null && downloadUiState.lastErrorAt > 0L) {
            snackbarHostState.showSnackbar(
                message = "❌ Download failed: $err",
                duration = androidx.compose.material3.SnackbarDuration.Long,
            )
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column {
                if (playerUiState.currentTrack != null) {
                    MiniPlayerBar(
                        uiState     = playerUiState,
                        onPlayPause = {
                            if (playerUiState.isPlaying) playerViewModel.pauseTrack()
                            else playerViewModel.resumeTrack()
                        },
                        onBarClick  = { currentTab = 1 },
                    )
                }

                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                ) {
                    NavigationBarItem(
                        icon = {
                            Icon(Icons.Filled.Search, contentDescription = "Search")
                        },
                        label = { Text("Search") },
                        selected = currentTab == 0,
                        onClick  = { currentTab = 0 },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor   = MaterialTheme.colorScheme.primary,
                            selectedTextColor   = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor      = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(Icons.Filled.MusicNote, contentDescription = "Player")
                        },
                        label = { Text("Player") },
                        selected = currentTab == 1,
                        onClick  = { currentTab = 1 },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor   = MaterialTheme.colorScheme.primary,
                            selectedTextColor   = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor      = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        DownloadOverlay(
            uiState = downloadUiState,
            onDismissJob = { downloadViewModel.dismissJob(it) },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    val direction = if (targetState > initialState) 1 else -1
                    (slideInHorizontally { it * direction } + fadeIn()).togetherWith(
                        slideOutHorizontally { -it * direction } + fadeOut()
                    )
                },
                label = "TabTransition",
            ) { tab ->
                when (tab) {
                    0 -> SearchScreen(
                        viewModel = searchViewModel,
                        downloadViewModel = downloadViewModel,
                        onDownloadClick = { url, platform ->
                            downloadViewModel.downloadTrack(url, platform)
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                    1 -> PlayerScreen(
                        viewModel = playerViewModel,
                        downloadViewModel = downloadViewModel,
                        onDownloadClick = { url, platform ->
                            downloadViewModel.downloadTrack(url, platform)
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniPlayerBar(
    uiState    : PlayerUiState,
    onPlayPause: () -> Unit,
    onBarClick : () -> Unit,
) {
    val track = uiState.currentTrack ?: return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onBarClick)
            .padding(horizontal = 12.dp),
        verticalAlignment    = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncImage(
            model            = track.thumbnail,
            contentDescription = null,
            modifier         = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer),
            contentScale     = ContentScale.Crop,
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text     = track.title,
                style    = MaterialTheme.typography.titleSmall,
                color    = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text     = track.artist,
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        IconButton(onClick = onPlayPause) {
            Icon(
                imageVector      = if (uiState.isPlaying) Icons.Filled.Pause
                                   else Icons.Filled.PlayArrow,
                contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                tint             = MaterialTheme.colorScheme.primary,
                modifier         = Modifier.size(28.dp),
            )
        }
    }
}
