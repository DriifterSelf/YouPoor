package com.voidfilio.youpoor.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.voidfilio.youpoor.Config
import com.voidfilio.youpoor.data.api.SearchEvent
import com.voidfilio.youpoor.data.api.SearchResult
import com.voidfilio.youpoor.data.api.WebSocketSearchClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SearchUiState(
    val isLoading: Boolean = false,
    val results: List<SearchResult> = emptyList(),
    val error: String? = null,
    val loadingMessage: String = "",
)

class SearchViewModel(
    private val wsClient: WebSocketSearchClient,
) : ViewModel() {


    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                wsClient.searchEvents.collect { event ->
                    when (event) {
                        is SearchEvent.Searching -> {
                            _uiState.value = SearchUiState(
                                isLoading = true,
                                results = emptyList(),
                                loadingMessage = event.message,
                                error = null
                            )
                        }

                        is SearchEvent.Result -> {
                            val current = _uiState.value
                            val updated = current.results.toMutableList()
                            updated.add(event.result)
                            _uiState.value = current.copy(
                                isLoading = true,
                                results = updated,
                                loadingMessage = "Cargando (${event.result.index}/${event.result.total})",
                                error = null
                            )
                        }

                        is SearchEvent.Complete -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                loadingMessage = ""
                            )
                        }

                        is SearchEvent.Error -> {
                            _uiState.value = SearchUiState(
                                error = event.error
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Exception in collect: ${e.message}", e)
            }
        }
    }

    fun search(query: String) {
        if (query.isBlank()) {
            _uiState.value = SearchUiState()
            return
        }

        _uiState.value = SearchUiState(
            isLoading = true,
            results = emptyList(),
            loadingMessage = "Conectando..."
        )

        wsClient.search(query)
    }

    override fun onCleared() {
        super.onCleared()
        wsClient.disconnect()
    }

    companion object {
        fun factory(): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val wsClient = WebSocketSearchClient(Config.BACKEND_URL)
                    return SearchViewModel(wsClient) as T
                }
            }
        }
    }
}
