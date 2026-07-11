package com.immichtv.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.immichtv.data.ImmichRepository
import com.immichtv.data.ViewerQueue
import com.immichtv.domain.Asset
import com.immichtv.ui.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SearchUiState {
    /** No query run yet. */
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Error(val message: String) : SearchUiState
    data class Results(val query: String, val assets: List<Asset>) : SearchUiState
}

/**
 * Drives smart (CLIP) search: a text query is sent to Immich's semantic search — matching by
 * meaning, scene, and recognized objects, not filename — and the results feed the shared viewer.
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: ImmichRepository,
    private val viewerQueue: ViewerQueue,
) : ViewModel() {

    private val _state = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private var lastQuery: String = ""

    fun search(query: String) {
        val q = query.trim()
        if (q.isEmpty()) return
        lastQuery = q
        _state.value = SearchUiState.Loading
        viewModelScope.launch {
            runCatching { repository.searchSmart(q) }.fold(
                onSuccess = { _state.value = SearchUiState.Results(q, it) },
                onFailure = { _state.value = SearchUiState.Error(it.toUserMessage()) },
            )
        }
    }

    fun retry() {
        if (lastQuery.isNotEmpty()) search(lastQuery)
    }

    fun prepareViewer(index: Int) {
        (state.value as? SearchUiState.Results)?.let { viewerQueue.set(it.assets, index) }
    }
}
