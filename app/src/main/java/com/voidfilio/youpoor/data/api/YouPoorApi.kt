package com.voidfilio.youpoor.data.api

import com.voidfilio.youpoor.data.models.DownloadRequest
import com.voidfilio.youpoor.data.models.DownloadResponse
import com.voidfilio.youpoor.data.models.HistoryEntry
import com.voidfilio.youpoor.data.models.SearchResult
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface YouPoorApi {
    @GET("/api/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("platform") platform: String = "youtube",
    ): List<SearchResult>

    @POST("/api/download")
    suspend fun download(@Body request: DownloadRequest): DownloadResponse

    @GET("/api/download/{downloadId}")
    suspend fun downloadFile(
        @Path("downloadId") downloadId: String,
    ): ResponseBody

    @GET("/api/history")
    suspend fun getHistory(): List<HistoryEntry>
}
