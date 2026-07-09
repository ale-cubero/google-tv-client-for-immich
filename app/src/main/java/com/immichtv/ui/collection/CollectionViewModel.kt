package com.immichtv.ui.collection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.immichtv.data.ImmichRepository
import com.immichtv.data.ViewerQueue
import com.immichtv.domain.Asset
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface CollectionUiState {
    data object Loading : CollectionUiState
    data class Error(val message: String) : CollectionUiState
    data class Ready(val title: String, val assets: List<Asset>) : CollectionUiState
}

/**
 * A titled grid of assets for either an album or a person, chosen by the `source` nav arg.
 * Reuses the same grid UI for both (albums reuse the grid component).
 */
@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val repository: ImmichRepository,
    private val viewerQueue: ViewerQueue,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val source: String = savedStateHandle["source"] ?: ""
    private val id: String = savedStateHandle["id"] ?: ""
    private val titleArg: String = savedStateHandle["title"] ?: ""

    private val _state = MutableStateFlow<CollectionUiState>(CollectionUiState.Loading)
    val state: StateFlow<CollectionUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = CollectionUiState.Loading
        viewModelScope.launch {
            runCatching {
                when (source) {
                    "album" -> {
                        val content = repository.getAlbumContent(id)
                        content.name to content.assets
                    }
                    "person" -> titleArg to repository.getPersonAssets(id)
                    else -> error("Unknown collection source: $source")
                }
            }.fold(
                onSuccess = { (title, assets) ->
                    _state.value = CollectionUiState.Ready(title, assets)
                },
                onFailure = { e ->
                    _state.value = CollectionUiState.Error(e.message ?: "Failed to load")
                },
            )
        }
    }

    fun prepareViewer(index: Int) {
        (state.value as? CollectionUiState.Ready)?.let { viewerQueue.set(it.assets, index) }
    }
}
