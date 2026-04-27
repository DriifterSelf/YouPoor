package com.voidfilio.youpoor.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import coil.compose.AsyncImage
import com.voidfilio.youpoor.viewmodel.DownloadJob
import com.voidfilio.youpoor.viewmodel.DownloadStatus
import com.voidfilio.youpoor.viewmodel.DownloadUiState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class DownloadOrbController {
    internal val orbs = mutableStateListOf<FlyingOrb>()
    internal var blobCenter by mutableStateOf<Offset?>(null)
    private var nextId = 0L

    fun launchOrb(from: Offset?, tint: Color) {
        if (from == null) return
        orbs += FlyingOrb(id = nextId++, start = from, tint = tint)
    }

    internal fun remove(id: Long) {
        orbs.removeAll { it.id == id }
    }
}

internal data class FlyingOrb(
    val id: Long,
    val start: Offset,
    val tint: Color,
)

val LocalDownloadOrbController = compositionLocalOf<DownloadOrbController?> { null }

@Composable
fun DownloadOverlay(
    uiState: DownloadUiState,
    onDismissJob: (String) -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    val controller = remember { DownloadOrbController() }

    CompositionLocalProvider(LocalDownloadOrbController provides controller) {
        Box(modifier = modifier.fillMaxSize()) {
            content()

            controller.orbs.toList().forEach { orb ->
                key(orb.id) {
                    FlyingOrbView(
                        orb = orb,
                        target = controller.blobCenter,
                        onDone = { controller.remove(orb.id) },
                    )
                }
            }

            BlobDock(
                uiState = uiState,
                onDismissJob = onDismissJob,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = bottomInset + 16.dp),
                onBlobPositioned = { controller.blobCenter = it },
            )
        }
    }
}

@Composable
private fun BlobDock(
    uiState: DownloadUiState,
    onDismissJob: (String) -> Unit,
    modifier: Modifier = Modifier,
    onBlobPositioned: (Offset) -> Unit,
) {
    val active = uiState.activeJobs
    val finished = uiState.jobs.values.filter {
        it.status == DownloadStatus.COMPLETED || it.status == DownloadStatus.FAILED
    }
    val now = System.currentTimeMillis()

    val recentlyFinished = finished.maxOfOrNull { it.finishedAt } ?: 0L
    val visible = active.isNotEmpty() || (now - recentlyFinished) < 4_000L

    var expanded by remember { mutableStateOf(false) }
    LaunchedEffect(active.size, finished.size) {
        if (active.isEmpty() && finished.isEmpty()) expanded = false
    }

    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(initialScale = 0.4f) + fadeIn(),
        exit = scaleOut(targetScale = 0.4f) + fadeOut(),
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AnimatedVisibility(
                visible = expanded && (active.isNotEmpty() || finished.isNotEmpty()),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                ExpandedSheet(
                    active = active,
                    finished = finished,
                    onDismissJob = onDismissJob,
                )
            }

            Blob(
                uiState = uiState,
                onClick = { if (active.isNotEmpty() || finished.isNotEmpty()) expanded = !expanded },
                modifier = Modifier.onGloballyPositioned { coords ->
                    val pos = coords.positionInRoot()
                    val size = coords.size
                    onBlobPositioned(
                        Offset(pos.x + size.width / 2f, pos.y + size.height / 2f)
                    )
                },
            )
        }
    }
}

@Composable
private fun Blob(
    uiState: DownloadUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val active = uiState.activeJobs
    val anyActive = active.isNotEmpty()
    val justSucceeded = !anyActive && uiState.lastCompleted != null &&
        (System.currentTimeMillis() - uiState.lastCompletedAt) < 3_000L

    val accent = MaterialTheme.colorScheme.primary
    val onAccent = MaterialTheme.colorScheme.onPrimary
    val success = Color(0xFF7BE38B)

    val infinite = rememberInfiniteTransition(label = "blob")
    val phase = 0f
    val breath = if (anyActive) 1.0f else 1f

    val targetProgress = if (anyActive) uiState.aggregateProgress else if (justSucceeded) 1f else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "progress",
    )

    val blobColor = when {
        justSucceeded -> success
        anyActive -> accent
        else -> accent
    }

    Box(
        modifier = modifier
            .size(64.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val baseR = size.minDimension * 0.34f * if (anyActive) breath else 1f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(blobColor.copy(alpha = 0.45f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = size.minDimension * 0.55f,
                ),
                radius = size.minDimension * 0.55f,
                center = Offset(cx, cy),
            )

            val path = Path()
            val points = 36
            for (i in 0..points) {
                val t = i / points.toFloat()
                val a = t * 2f * PI.toFloat()
                val wobble =
                    sin((a * 3f + phase).toDouble()).toFloat() * 0.06f +
                    sin((a * 5f - phase * 0.7f).toDouble()).toFloat() * 0.04f
                val r = baseR * (1f + if (anyActive) wobble else wobble * 0.3f)
                val x = cx + cos(a.toDouble()).toFloat() * r
                val y = cy + sin(a.toDouble()).toFloat() * r
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, color = blobColor)

            if (animatedProgress > 0.01f) {
                val arcR = size.minDimension * 0.46f
                val stroke = 4.dp.toPx()
                drawArc(
                    color = if (justSucceeded) success else onAccent,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = Offset(cx - arcR, cy - arcR),
                    size = androidx.compose.ui.geometry.Size(arcR * 2f, arcR * 2f),
                    style = Stroke(width = stroke),
                )
            }
        }

        if (justSucceeded) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Downloaded",
                tint = onAccent,
                modifier = Modifier.size(22.dp),
            )
        } else if (anyActive) {
            val count = active.size
            if (count == 1) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = "Downloading",
                    tint = onAccent,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = onAccent,
                )
            }
        } else {
            Icon(
                imageVector = Icons.Filled.Download,
                contentDescription = "Downloads",
                tint = onAccent,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ExpandedSheet(
    active: List<DownloadJob>,
    finished: List<DownloadJob>,
    onDismissJob: (String) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 12.dp,
        modifier = Modifier.widthIn(min = 240.dp, max = 320.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = if (active.isNotEmpty()) "Downloading ${active.size}" else "Recent downloads",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp),
            )

            val ordered = active + finished.sortedByDescending { it.finishedAt }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.height(
                    if (ordered.size > 4) 280.dp else (ordered.size * 64).dp.coerceAtLeast(64.dp)
                ),
            ) {
                items(ordered, key = { it.id }) { job ->
                    JobRow(job = job, onDismiss = { onDismissJob(job.id) })
                }
            }
        }
    }
}

@Composable
private fun JobRow(job: DownloadJob, onDismiss: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceMid = MaterialTheme.colorScheme.onSurfaceVariant
    val success = Color(0xFF7BE38B)
    val errorColor = MaterialTheme.colorScheme.error

    val animProgress by animateFloatAsState(
        targetValue = job.progress,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "rowProgress",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier.size(44.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 3.dp.toPx()
                val ringColor = when (job.status) {
                    DownloadStatus.COMPLETED -> success
                    DownloadStatus.FAILED -> errorColor
                    else -> accent
                }
                drawCircle(
                    color = ringColor.copy(alpha = 0.18f),
                    radius = (size.minDimension - stroke) / 2f,
                    style = Stroke(width = stroke),
                )
                val sweep = if (job.status == DownloadStatus.COMPLETED) 360f else 360f * animProgress
                if (sweep > 0.5f) {
                    val r = (size.minDimension - stroke) / 2f
                    drawArc(
                        color = ringColor,
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(size.width / 2f - r, size.height / 2f - r),
                        size = androidx.compose.ui.geometry.Size(r * 2f, r * 2f),
                        style = Stroke(width = stroke),
                    )
                }
            }

            if (!job.thumbnail.isNullOrEmpty()) {
                AsyncImage(
                    model = job.thumbnail,
                    contentDescription = null,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    imageVector = when (job.status) {
                        DownloadStatus.COMPLETED -> Icons.Filled.Check
                        DownloadStatus.FAILED -> Icons.Filled.Close
                        else -> Icons.Filled.MusicNote
                    },
                    contentDescription = null,
                    tint = when (job.status) {
                        DownloadStatus.COMPLETED -> success
                        DownloadStatus.FAILED -> errorColor
                        else -> accent
                    },
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = job.title ?: "Resolving…",
                style = MaterialTheme.typography.labelLarge,
                color = onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = when (job.status) {
                    DownloadStatus.QUEUED -> "Queued"
                    DownloadStatus.METADATA -> "Fetching info…"
                    DownloadStatus.DOWNLOADING -> "${(job.progress * 100).toInt()}%"
                    DownloadStatus.COMPLETED -> "Done"
                    DownloadStatus.FAILED -> job.error ?: "Failed"
                },
                style = MaterialTheme.typography.bodySmall,
                color = when (job.status) {
                    DownloadStatus.FAILED -> errorColor
                    DownloadStatus.COMPLETED -> success
                    else -> onSurfaceMid
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (job.status == DownloadStatus.COMPLETED || job.status == DownloadStatus.FAILED) {
            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Dismiss",
                    tint = onSurfaceMid,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun FlyingOrbView(
    orb: FlyingOrb,
    target: Offset?,
    onDone: () -> Unit,
) {
    val density = LocalDensity.current
    val t = remember { Animatable(0f) }

    LaunchedEffect(orb.id, target) {
        if (target == null) {
            t.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
        } else {
            t.animateTo(1f, tween(620, easing = FastOutSlowInEasing))
        }
        onDone()
    }

    val end = target ?: orb.start
    val progress = t.value

    val mid = Offset(
        x = (orb.start.x + end.x) / 2f,
        y = (orb.start.y + end.y) / 2f - 140f,
    )
    val oneMinus = 1f - progress
    val pos = Offset(
        x = oneMinus * oneMinus * orb.start.x + 2f * oneMinus * progress * mid.x + progress * progress * end.x,
        y = oneMinus * oneMinus * orb.start.y + 2f * oneMinus * progress * mid.y + progress * progress * end.y,
    )

    val orbSizeDp = 28.dp
    val orbSizePx = with(density) { orbSizeDp.toPx() }
    val scale = when {
        progress < 0.15f -> 0.4f + (progress / 0.15f) * 0.6f
        progress > 0.85f -> 1.0f - ((progress - 0.85f) / 0.15f) * 0.7f
        else -> 1.0f
    }
    val alpha = when {
        progress > 0.85f -> 1.0f - ((progress - 0.85f) / 0.15f)
        else -> 1.0f
    }

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (pos.x - orbSizePx / 2f).toInt(),
                    y = (pos.y - orbSizePx / 2f).toInt(),
                )
            }
            .size(orbSizeDp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(orb.tint.copy(alpha = 0.6f), Color.Transparent),
                    radius = size.minDimension * 0.6f,
                ),
                radius = size.minDimension * 0.6f,
            )
            drawCircle(
                color = orb.tint,
                radius = size.minDimension * 0.32f,
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = size.minDimension * 0.14f,
                center = Offset(size.width * 0.42f, size.height * 0.42f),
            )
        }
    }
}
