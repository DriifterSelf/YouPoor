package com.voidfilio.youpoor.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.voidfilio.youpoor.data.api.SearchResult
import com.voidfilio.youpoor.ui.components.LocalDownloadOrbController
import com.voidfilio.youpoor.viewmodel.DownloadStatus
import com.voidfilio.youpoor.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    downloadViewModel: com.voidfilio.youpoor.viewmodel.DownloadViewModel? = null,
    onDownloadClick: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchQuery by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()
    val downloadState by (downloadViewModel?.uiState?.collectAsState() ?: remember { mutableStateOf(com.voidfilio.youpoor.viewmodel.DownloadUiState()) })

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val horizontalPadding = (screenWidth * 0.03f).coerceAtLeast(8.dp)
        val verticalPadding = (screenHeight * 0.015f).coerceAtLeast(8.dp)
        val searchBarHeight = (screenHeight * 0.08f).coerceIn(48.dp, 64.dp)
        val buttonSize = searchBarHeight

        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = horizontalPadding,
                        vertical = verticalPadding,
                    ),
                horizontalArrangement = Arrangement.spacedBy(horizontalPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .weight(1f)
                        .height(searchBarHeight),
                    placeholder = {
                        Text(
                            "Search music, artists...",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = "Search",
                            modifier = Modifier.size(searchBarHeight * 0.4f),
                        )
                    },
                    enabled = !uiState.isLoading,
                    singleLine = true,
                    shape = RoundedCornerShape(searchBarHeight * 0.2f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Search,
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (searchQuery.isNotBlank() && !uiState.isLoading) {
                                viewModel.search(searchQuery)
                            }
                        },
                    ),
                )

                androidx.compose.material3.Button(
                    onClick = {
                        if (searchQuery.isNotBlank()) {
                            viewModel.search(searchQuery)
                        }
                    },
                    modifier = Modifier.size(buttonSize),
                    enabled = !uiState.isLoading && searchQuery.isNotBlank(),
                    shape = RoundedCornerShape(buttonSize * 0.2f),
                ) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = "Search",
                        modifier = Modifier.size(buttonSize * 0.55f),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding),
            ) {
                if (uiState.error != null) {
                    ErrorState(uiState.error ?: "Unknown error")
                } else if (uiState.results.isEmpty() && uiState.isLoading) {
                    LoadingState(uiState.loadingMessage)
                } else if (uiState.results.isEmpty() && !uiState.isLoading && searchQuery.isNotBlank()) {
                    EmptyState()
                } else if (uiState.results.isNotEmpty()) {
                    ResultsState(uiState, downloadState, onDownloadClick)
                } else {
                    IdleState()
                }
            }
        }
    }
}

@Composable
private fun ErrorState(error: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.errorContainer,
                    RoundedCornerShape(16.dp),
                )
                .padding(24.dp),
        ) {
            Text(
                "⚠️ Something went wrong",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

@Composable
private fun LoadingState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(56.dp),
                strokeWidth = 5.dp,
                color = MaterialTheme.colorScheme.primary,
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "Searching...",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f),
            )
            Text(
                "No results found",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun IdleState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0f),
                            ),
                        ),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                "Search for music",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Discover thousands of songs",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ResultsState(
    uiState: com.voidfilio.youpoor.viewmodel.SearchUiState,
    downloadState: com.voidfilio.youpoor.viewmodel.DownloadUiState,
    onDownloadClick: (String, String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = uiState.isLoading,
            enter = slideInVertically() + fadeIn(),
        ) {
            ProgressBar(uiState)
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(vertical = 8.dp),
        ) {
            items(
                uiState.results,
                key = { it.id },
            ) { result ->
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically { it / 2 } + fadeIn(),
                ) {
                    SearchResultCardWithDownloadState(result, downloadState) { url ->
                        onDownloadClick(url, result.platform)
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultCardWithDownloadState(
    result: SearchResult,
    downloadState: com.voidfilio.youpoor.viewmodel.DownloadUiState,
    onDownloadClick: (String) -> Unit,
) {
    val job = downloadState.jobs[result.id]
    val isDownloading = job != null &&
        job.status != DownloadStatus.COMPLETED &&
        job.status != DownloadStatus.FAILED
    val isCompleted = job?.status == DownloadStatus.COMPLETED
    val orbController = LocalDownloadOrbController.current
    val accent = MaterialTheme.colorScheme.primary
    var iconCenter by remember { mutableStateOf(Offset.Zero) }

    BoxWithConstraints {
        val cardWidth = maxWidth
        val thumbnailSize = (cardWidth * 0.2f).coerceIn(60.dp, 100.dp)
        val cornerRadius = thumbnailSize * 0.15f
        val cardPadding = (cardWidth * 0.02f).coerceAtLeast(8.dp)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 2.dp,
                    shape = RoundedCornerShape(cornerRadius),
                    clip = false,
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(cornerRadius),
            colors = CardDefaults.cardColors(
                containerColor = if (isDownloading) MaterialTheme.colorScheme.primaryContainer
                                 else MaterialTheme.colorScheme.surface,
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(cardPadding),
                horizontalArrangement = Arrangement.spacedBy(cardPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (result.thumbnail.isNotEmpty()) {
                    AsyncImage(
                        model = result.thumbnail,
                        contentDescription = result.title,
                        modifier = Modifier
                            .size(thumbnailSize)
                            .clip(RoundedCornerShape(cornerRadius * 0.7f))
                            .background(MaterialTheme.colorScheme.surfaceContainer),
                        contentScale = ContentScale.Crop,
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = cardPadding * 0.5f),
                    verticalArrangement = Arrangement.spacedBy(cardPadding * 0.5f),
                ) {
                    Text(
                        text = result.title,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDownloading) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = result.artist,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDownloading) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (result.duration > 0) {
                        val minutes = result.duration / 60
                        val seconds = result.duration % 60
                        Text(
                            text = String.format("%d:%02d min", minutes, seconds),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                if (isDownloading) {
                    Column(
                        modifier = Modifier.size(thumbnailSize * 0.55f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        val frac = (job?.progress ?: 0f).coerceIn(0f, 1f)
                        CircularProgressIndicator(
                            progress = { frac },
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = "${(frac * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                } else if (isCompleted) {
                    Box(
                        modifier = Modifier
                            .size(thumbnailSize * 0.55f)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = "Downloaded",
                            modifier = Modifier.size(thumbnailSize * 0.35f),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                } else {
                    IconButton(
                        onClick = {
                            orbController?.launchOrb(iconCenter.takeIf { it != Offset.Zero }, accent)
                            onDownloadClick(result.id)
                        },
                        modifier = Modifier
                            .size(thumbnailSize * 0.55f)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                CircleShape,
                            )
                            .onGloballyPositioned { coords ->
                                val pos = coords.positionInRoot()
                                iconCenter = Offset(
                                    pos.x + coords.size.width / 2f,
                                    pos.y + coords.size.height / 2f,
                                )
                            },
                    ) {
                        Icon(
                            Icons.Filled.Download,
                            contentDescription = "Download",
                            modifier = Modifier.size(thumbnailSize * 0.35f),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressBar(uiState: com.voidfilio.youpoor.viewmodel.SearchUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceContainer,
                RoundedCornerShape(12.dp),
            )
            .padding(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                uiState.loadingMessage,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "${uiState.results.size}/${uiState.results.firstOrNull()?.total ?: 0}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
        LinearProgressIndicator(
            progress = {
                val total = uiState.results.firstOrNull()?.total ?: 1
                (uiState.results.size.toFloat() / total).coerceIn(0f, 1f)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun SearchResultCard(
    result: SearchResult,
    onDownloadClick: (String) -> Unit,
) {
    BoxWithConstraints {
        val cardWidth = maxWidth
        val thumbnailSize = (cardWidth * 0.2f).coerceIn(60.dp, 100.dp)
        val cornerRadius = thumbnailSize * 0.15f
        val cardPadding = (cardWidth * 0.02f).coerceAtLeast(8.dp)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 2.dp,
                    shape = RoundedCornerShape(cornerRadius),
                    clip = false,
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(cornerRadius),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(cardPadding),
                horizontalArrangement = Arrangement.spacedBy(cardPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (result.thumbnail.isNotEmpty()) {
                    AsyncImage(
                        model = result.thumbnail,
                        contentDescription = result.title,
                        modifier = Modifier
                            .size(thumbnailSize)
                            .clip(RoundedCornerShape(cornerRadius * 0.7f))
                            .background(MaterialTheme.colorScheme.surfaceContainer),
                        contentScale = ContentScale.Crop,
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = cardPadding * 0.5f),
                    verticalArrangement = Arrangement.spacedBy(cardPadding * 0.5f),
                ) {
                    Text(
                        text = result.title,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = result.artist,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (result.duration > 0) {
                        val minutes = result.duration / 60
                        val seconds = result.duration % 60
                        Text(
                            text = String.format("%d:%02d min", minutes, seconds),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                IconButton(
                    onClick = { onDownloadClick(result.id) },
                    modifier = Modifier
                        .size(thumbnailSize * 0.55f)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            CircleShape,
                        ),
                ) {
                    Icon(
                        Icons.Filled.Download,
                        contentDescription = "Download",
                        modifier = Modifier.size(thumbnailSize * 0.35f),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
