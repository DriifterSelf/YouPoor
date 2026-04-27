package com.voidfilio.youpoor.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "search_results")
data class SearchResult(
    @PrimaryKey
    val id: String,
    val title: String,
    val artist: String,
    val duration: Int,
    val thumbnail: String,
    val platform: String,
)

@Entity(tableName = "downloads")
data class Download(
    @PrimaryKey
    val downloadId: String,
    val title: String,
    val artist: String,
    val duration: Int,
    val thumbnail: String,
    val platform: String,
    val filePath: String,
    val downloadedAt: Long,
)

data class DownloadRequest(
    val url: String,
    val platform: String,
)

data class DownloadResponse(
    val download_id: String,
    val file_url: String,
    val filename: String,
    val metadata: Metadata,
)

data class Metadata(
    val title: String,
    val duration: Int,
    val thumbnail: String,
    val artist: String,
)

@Entity(tableName = "history")
data class HistoryEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val artist: String,
    val platform: String,
    val downloadedAt: Long = System.currentTimeMillis(),
)
