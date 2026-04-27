package com.voidfilio.youpoor.data.api

import android.util.Log
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener

data class SearchResult(
    val id: String,
    val title: String,
    val artist: String,
    val duration: Int,
    val thumbnail: String,
    val platform: String,
    val index: Int = 0,
    val total: Int = 0
)

sealed class SearchEvent {
    data class Searching(val message: String) : SearchEvent()
    data class Result(val result: SearchResult) : SearchEvent()
    data class Complete(val total: Int, val timeMs: Long) : SearchEvent()
    data class Error(val error: String) : SearchEvent()
}

class WebSocketSearchClient(private val baseUrl: String) {
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    private val _searchEvents = MutableSharedFlow<SearchEvent>(
        replay = 0,
        extraBufferCapacity = 10,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    val searchEvents: SharedFlow<SearchEvent> = _searchEvents

    private var webSocket: WebSocket? = null

    fun search(query: String, platform: String = "youtube") {
        val wsUrl = baseUrl
            .replace("https://", "wss://")
            .replace("http://", "ws://") + "/ws/search"

        Log.d("WebSocketClient", "Connecting to: $wsUrl")

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                Log.d("WebSocketClient", "Connected")
                val searchPayload = """{"q":"$query","platform":"$platform"}"""
                webSocket.send(searchPayload)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    Log.d("WebSocketClient", "Raw message: $text")
                    val json = Json.parseToJsonElement(text).jsonObject

                    when (val status = json["status"]?.jsonPrimitive?.content) {
                        "searching" -> {
                            val message = json["message"]?.jsonPrimitive?.content ?: "Searching..."
                            Log.d("WebSocketClient", "Searching: $message")
                            _searchEvents.tryEmit(SearchEvent.Searching(message))
                        }

                        "result" -> {
                            val data = json["data"]?.jsonObject
                            if (data != null) {
                                val result = SearchResult(
                                    id = data["id"]?.jsonPrimitive?.content ?: "",
                                    title = data["title"]?.jsonPrimitive?.content ?: "Unknown",
                                    artist = data["artist"]?.jsonPrimitive?.content ?: "Unknown",
                                    duration = (data["duration"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0).toInt(),
                                    thumbnail = data["thumbnail"]?.jsonPrimitive?.content ?: "",
                                    platform = data["platform"]?.jsonPrimitive?.content ?: "youtube",
                                    index = data["index"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                                    total = data["total"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                                )
                                _searchEvents.tryEmit(SearchEvent.Result(result))
                                Log.d("WebSocketClient", "Result: ${result.title}")
                            }
                        }

                        "complete" -> {
                            val total = json["total"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                            val timeMs = try {
                                (json["time_ms"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0).toLong()
                            } catch (e: Exception) {
                                0L
                            }
                            _searchEvents.tryEmit(SearchEvent.Complete(total, timeMs))
                            Log.d("WebSocketClient", "Complete: $total results in ${timeMs}ms")
                            webSocket.close(1000, "Done")
                        }

                        "error" -> {
                            val error = json["error"]?.jsonPrimitive?.content ?: "Unknown error"
                            _searchEvents.tryEmit(SearchEvent.Error(error))
                            Log.e("WebSocketClient", "Error: $error")
                            webSocket.close(1000, "Error")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("WebSocketClient", "Parse error: ${e.message}")
                    _searchEvents.tryEmit(SearchEvent.Error(e.message ?: "Parse error"))
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                Log.e("WebSocketClient", "WebSocket failure: ${t.message}")
                _searchEvents.tryEmit(SearchEvent.Error(t.message ?: "Connection failed"))
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocketClient", "Closing: $code $reason")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocketClient", "Closed")
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "Client closing")
    }
}
