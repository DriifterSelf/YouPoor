package com.voidfilio.youpoor.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.voidfilio.youpoor.data.models.Download
import com.voidfilio.youpoor.data.repository.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class DownloadStatus { QUEUED, METADATA, DOWNLOADING, COMPLETED, FAILED }

data class DownloadJob(
    val id: String,
    val title: String? = null,
    val artist: String? = null,
    val thumbnail: String? = null,
    val progress: Float = 0f,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val error: String? = null,
    val completed: Download? = null,
    val startedAt: Long = System.currentTimeMillis(),
    val finishedAt: Long = 0L,
)

data class DownloadUiState(
    val jobs: Map<String, DownloadJob> = emptyMap(),
    val lastCompleted: Download? = null,
    val lastCompletedAt: Long = 0L,
    val lastError: String? = null,
    val lastErrorAt: Long = 0L,
) {
    val activeJobs: List<DownloadJob>
        get() = jobs.values.filter {
            it.status != DownloadStatus.COMPLETED && it.status != DownloadStatus.FAILED
        }

    val isDownloading: Boolean get() = activeJobs.isNotEmpty()

    val aggregateProgress: Float
        get() {
            val active = activeJobs
            if (active.isEmpty()) return 0f
            return active.map { it.progress }.average().toFloat()
        }

    // Legacy fields for callers that haven't moved to per-job yet.
    val progress: Int get() = (aggregateProgress * 100).toInt()
    val completedDownload: Download? get() = lastCompleted
    val error: String? get() = lastError
}

class DownloadViewModel(
    private val repository: MusicRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DownloadUiState())
    val uiState: StateFlow<DownloadUiState> = _uiState.asStateFlow()

    fun downloadTrack(url: String, platform: String) {
        val existing = _uiState.value.jobs[url]
        if (existing != null && existing.status != DownloadStatus.FAILED) {
            // Already in flight or completed — don't double-launch.
            return
        }

        viewModelScope.launch {
            Log.d("DownloadViewModel", "📥 Starting download: url=$url, platform=$platform")
            updateJob(url) {
                (it ?: DownloadJob(id = url)).copy(
                    status = DownloadStatus.METADATA,
                    progress = 0f,
                    error = null,
                    completed = null,
                )
            }

            try {
                val download = repository.download(
                    url = url,
                    platform = platform,
                    onMetadata = { title, artist, thumbnail ->
                        updateJob(url) {
                            (it ?: DownloadJob(id = url)).copy(
                                title = title,
                                artist = artist,
                                thumbnail = thumbnail,
                                status = DownloadStatus.DOWNLOADING,
                            )
                        }
                    },
                    onProgress = { downloaded, total ->
                        val frac = if (total > 0) {
                            (downloaded.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                        } else 0f
                        updateJob(url) {
                            (it ?: DownloadJob(id = url)).copy(
                                status = DownloadStatus.DOWNLOADING,
                                progress = frac,
                            )
                        }
                    },
                )

                Log.d("DownloadViewModel", "✅ Download completed: ${download.title}")
                _uiState.update { state ->
                    val updated = (state.jobs[url] ?: DownloadJob(id = url)).copy(
                        status = DownloadStatus.COMPLETED,
                        progress = 1f,
                        completed = download,
                        title = download.title,
                        artist = download.artist,
                        thumbnail = download.thumbnail,
                        finishedAt = System.currentTimeMillis(),
                    )
                    state.copy(
                        jobs = state.jobs + (url to updated),
                        lastCompleted = download,
                        lastCompletedAt = System.currentTimeMillis(),
                    )
                }
            } catch (e: Exception) {
                Log.e("DownloadViewModel", "❌ Download failed: ${e.message}", e)
                val message = e.message ?: "Download failed"
                _uiState.update { state ->
                    val updated = (state.jobs[url] ?: DownloadJob(id = url)).copy(
                        status = DownloadStatus.FAILED,
                        error = message,
                        finishedAt = System.currentTimeMillis(),
                    )
                    state.copy(
                        jobs = state.jobs + (url to updated),
                        lastError = message,
                        lastErrorAt = System.currentTimeMillis(),
                    )
                }
            }
        }
    }

    fun dismissJob(id: String) {
        _uiState.update { it.copy(jobs = it.jobs - id) }
    }

    fun clearFinished() {
        _uiState.update { state ->
            state.copy(
                jobs = state.jobs.filterValues {
                    it.status != DownloadStatus.COMPLETED && it.status != DownloadStatus.FAILED
                },
            )
        }
    }

    private inline fun updateJob(id: String, transform: (DownloadJob?) -> DownloadJob) {
        _uiState.update { state ->
            val next = transform(state.jobs[id])
            state.copy(jobs = state.jobs + (id to next))
        }
    }

    companion object {
        fun factory(repository: MusicRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return DownloadViewModel(repository) as T
                }
            }
        }
    }
}
