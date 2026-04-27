package com.voidfilio.youpoor.data.repository

import android.util.Log
import com.voidfilio.youpoor.Config
import com.voidfilio.youpoor.data.api.YouPoorApi
import com.voidfilio.youpoor.data.db.DownloadDao
import com.voidfilio.youpoor.data.db.HistoryDao
import com.voidfilio.youpoor.data.downloader.FileDownloader
import com.voidfilio.youpoor.data.models.Download
import com.voidfilio.youpoor.data.models.DownloadRequest
import com.voidfilio.youpoor.data.models.HistoryEntry
import com.voidfilio.youpoor.data.models.SearchResult
import kotlinx.coroutines.flow.Flow

class MusicRepository(
    private val api: YouPoorApi,
    private val downloadDao: DownloadDao,
    private val historyDao: HistoryDao,
    private val fileDownloader: FileDownloader,
) {
    suspend fun search(query: String, platform: String = "youtube"): List<SearchResult> {
        return api.search(query, platform)
    }

    suspend fun download(
        url: String,
        platform: String,
        onMetadata: ((title: String, artist: String, thumbnail: String) -> Unit)? = null,
        onProgress: ((downloaded: Long, total: Long) -> Unit)? = null,
    ): Download {
        Log.d("MusicRepository", "📥 Download started: url=$url, platform=$platform")

        Log.d("MusicRepository", "🔄 Calling API...")
        val response = api.download(DownloadRequest(url, platform))
        Log.d("MusicRepository", "✅ API response: ${response.download_id}")

        onMetadata?.invoke(
            response.metadata.title,
            response.metadata.artist,
            response.metadata.thumbnail,
        )

        val fileName = response.filename
        Log.d("MusicRepository", "📥 Downloading file: $fileName from ${Config.BACKEND_URL}${response.file_url}")
        val filePath = fileDownloader.downloadFile(
            "${Config.BACKEND_URL}${response.file_url}",
            fileName,
            onProgress,
        )
        Log.d("MusicRepository", "✅ File downloaded to: $filePath")

        val download = Download(
            downloadId = response.download_id,
            title = response.metadata.title,
            artist = response.metadata.artist,
            duration = response.metadata.duration,
            thumbnail = response.metadata.thumbnail,
            platform = platform,
            filePath = filePath,
            downloadedAt = System.currentTimeMillis(),
        )

        Log.d("MusicRepository", "💾 Inserting into database: ${download.title}")
        try {
            downloadDao.insert(download)
            Log.d("MusicRepository", "✅ Successfully inserted into database")
        } catch (e: Exception) {
            Log.e("MusicRepository", "❌ Database insert failed: ${e.message}", e)
            throw e
        }

        val historyEntry = HistoryEntry(
            title = response.metadata.title,
            artist = response.metadata.artist,
            platform = platform,
        )
        historyDao.insert(historyEntry)

        return download
    }

    fun getDownloads(): Flow<List<Download>> {
        return downloadDao.getAll()
    }

    suspend fun getHistory(): List<HistoryEntry> {
        return api.getHistory()
    }

    fun getLocalHistory(): Flow<List<HistoryEntry>> {
        return historyDao.getAll()
    }

    suspend fun addToHistory(entry: HistoryEntry) {
        historyDao.insert(entry)
    }
}
