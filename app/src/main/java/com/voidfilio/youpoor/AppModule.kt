package com.voidfilio.youpoor

import android.content.Context
import com.voidfilio.youpoor.data.api.YouPoorApi
import com.voidfilio.youpoor.data.db.YouPoorDatabase
import com.voidfilio.youpoor.data.downloader.FileDownloader
import com.voidfilio.youpoor.data.repository.MusicRepository
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object AppModule {
    private val BASE_URL = Config.BACKEND_URL

    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    fun provideApi(): YouPoorApi {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(provideOkHttpClient())
            .build()
            .create(YouPoorApi::class.java)
    }

    fun provideDatabase(context: Context): YouPoorDatabase {
        return YouPoorDatabase.getInstance(context)
    }

    fun provideFileDownloader(context: Context): FileDownloader {
        return FileDownloader(context, provideOkHttpClient())
    }

    fun provideRepository(context: Context): MusicRepository {
        val api = provideApi()
        val db = provideDatabase(context)
        val downloader = provideFileDownloader(context)
        return MusicRepository(
            api = api,
            downloadDao = db.downloadDao(),
            historyDao = db.historyDao(),
            fileDownloader = downloader,
        )
    }
}
