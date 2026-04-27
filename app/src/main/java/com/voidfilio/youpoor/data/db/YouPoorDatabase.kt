package com.voidfilio.youpoor.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.voidfilio.youpoor.data.models.Download
import com.voidfilio.youpoor.data.models.HistoryEntry
import com.voidfilio.youpoor.data.models.SearchResult

@Database(
    entities = [SearchResult::class, Download::class, HistoryEntry::class],
    version = 1,
    exportSchema = false
)
abstract class YouPoorDatabase : RoomDatabase() {
    abstract fun searchResultDao(): SearchResultDao
    abstract fun downloadDao(): DownloadDao
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var instance: YouPoorDatabase? = null

        fun getInstance(context: Context): YouPoorDatabase {
            return instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    YouPoorDatabase::class.java,
                    "youpoor.db"
                )
                    .build()
                    .also { instance = it }
            }
        }
    }
}
