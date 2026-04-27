package com.voidfilio.youpoor.data.downloader

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class FileDownloader(
    private val context: Context,
    private val httpClient: OkHttpClient = OkHttpClient(),
) {
    suspend fun downloadFile(
        url: String,
        fileName: String,
        onProgress: ((downloaded: Long, total: Long) -> Unit)? = null,
    ): String = withContext(Dispatchers.IO) {
        val downloadsDir = File(context.getExternalFilesDir(null), "downloads").apply {
            mkdirs()
        }

        val file = File(downloadsDir, fileName)

        val request = Request.Builder()
            .url(url)
            .build()

        val response = httpClient.newCall(request).execute()

        if (!response.isSuccessful) {
            throw Exception("Download failed: ${response.code}")
        }

        val body = response.body ?: throw Exception("Empty response body")
        val total = body.contentLength().takeIf { it > 0 } ?: -1L

        body.byteStream().use { input ->
            FileOutputStream(file).use { output ->
                val buffer = ByteArray(16 * 1024)
                var downloaded = 0L
                var lastEmit = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    output.write(buffer, 0, read)
                    downloaded += read
                    if (onProgress != null) {
                        // Throttle emissions: every ~64KB or when complete.
                        if (downloaded - lastEmit >= 64 * 1024) {
                            onProgress(downloaded, total)
                            lastEmit = downloaded
                        }
                    }
                }
                onProgress?.invoke(downloaded, if (total > 0) total else downloaded)
            }
        }

        return@withContext file.absolutePath
    }

    fun getDownloadsDirectory(): File {
        return File(context.getExternalFilesDir(null), "downloads").apply {
            mkdirs()
        }
    }
}
