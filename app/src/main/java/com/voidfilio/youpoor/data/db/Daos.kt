package com.voidfilio.youpoor.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.voidfilio.youpoor.data.models.Download
import com.voidfilio.youpoor.data.models.HistoryEntry
import com.voidfilio.youpoor.data.models.SearchResult
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchResultDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(searchResult: SearchResult)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(results: List<SearchResult>)

    @Query("SELECT * FROM search_results")
    fun getAll(): Flow<List<SearchResult>>

    @Delete
    suspend fun delete(searchResult: SearchResult)
}

@Dao
interface DownloadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(download: Download)

    @Query("SELECT * FROM downloads ORDER BY downloadedAt DESC")
    fun getAll(): Flow<List<Download>>

    @Query("SELECT * FROM downloads WHERE downloadId = :id")
    suspend fun getById(id: String): Download?

    @Delete
    suspend fun delete(download: Download)
}

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: HistoryEntry)

    @Query("SELECT * FROM history ORDER BY downloadedAt DESC LIMIT 50")
    fun getAll(): Flow<List<HistoryEntry>>

    @Delete
    suspend fun delete(entry: HistoryEntry)
}
