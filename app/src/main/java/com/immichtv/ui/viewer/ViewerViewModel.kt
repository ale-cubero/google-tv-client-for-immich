package com.immichtv.ui.viewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.immichtv.data.ImmichRepository
import com.immichtv.data.ViewerQueue
import com.immichtv.domain.Asset
import com.immichtv.domain.AssetDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import javax.inject.Inject

sealed interface ViewerUiState {
    data class Error(val message: String) : ViewerUiState
    data class Ready(val assets: List<Asset>, val startIndex: Int) : ViewerUiState
}

/**
 * Full-screen viewer over whatever asset list the opening screen put in [ViewerQueue]. Exposes the
 * shared [OkHttpClient] so ExoPlayer's data source carries the same `x-api-key` header, and loads
 * rich EXIF/people detail lazily for the metadata panel.
 */
@HiltViewModel
class ViewerViewModel @Inject constructor(
    queue: ViewerQueue,
    val okHttpClient: OkHttpClient,
    private val repository: ImmichRepository,
) : ViewModel() {

    val state: ViewerUiState =
        if (queue.assets.isEmpty()) {
            ViewerUiState.Error("Nothing to show")
        } else {
            ViewerUiState.Ready(queue.assets, queue.startIndex)
        }

    private val detailCache = mutableMapOf<String, AssetDetail>()
    private val _detail = MutableStateFlow<DetailState>(DetailState.Loading)
    val detail: StateFlow<DetailState> = _detail.asStateFlow()

    /** Fetch (and cache) detail for the asset whose panel is being shown. */
    fun loadDetail(assetId: String) {
        detailCache[assetId]?.let { _detail.value = DetailState.Loaded(it); return }
        _detail.value = DetailState.Loading
        viewModelScope.launch {
            _detail.value = runCatching { repository.getAssetDetail(assetId) }
                .fold(
                    onSuccess = { detailCache[assetId] = it; DetailState.Loaded(it) },
                    onFailure = { DetailState.Error }, // surface failures instead of an endless spinner
                )
        }
    }
}

sealed interface DetailState {
    data object Loading : DetailState
    data object Error : DetailState
    data class Loaded(val detail: AssetDetail) : DetailState
}
