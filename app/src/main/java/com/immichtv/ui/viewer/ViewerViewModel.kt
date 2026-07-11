/*
 * TV client for Immich — a read-only Android TV client for a self-hosted Immich server.
 * Copyright (C) 2026 Alejandro Cubero
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.immichtv.ui.viewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.immichtv.data.ImmichRepository
import com.immichtv.data.ViewerQueue
import com.immichtv.data.prefs.SettingsRepository
import com.immichtv.domain.Asset
import com.immichtv.domain.AssetDetail
import com.immichtv.domain.SlideshowPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import javax.inject.Inject

/** How close to the list edge the pager may get before the adjacent month is loaded. */
private const val EDGE_PREFETCH = 6

sealed interface ViewerUiState {
    data class Error(val message: String) : ViewerUiState
    data class Ready(val startIndex: Int) : ViewerUiState
}

/**
 * Full-screen viewer over whatever asset list the opening screen put in [ViewerQueue]. Exposes the
 * shared [OkHttpClient] so ExoPlayer's data source carries the same `x-api-key` header, and loads
 * rich EXIF/people detail lazily for the metadata panel.
 *
 * When opened from the timeline it also carries [com.immichtv.data.TimelineNav] context: as the
 * pager nears either end of [assets], the adjacent month bucket is fetched and appended/prepended,
 * so paging flows across month boundaries. A prepend shifts indices, so it emits the shift on
 * [prepended] for the UI to compensate (jump the pager by the same amount, no animation).
 */
@HiltViewModel
class ViewerViewModel @Inject constructor(
    private val queue: ViewerQueue,
    val okHttpClient: OkHttpClient,
    private val repository: ImmichRepository,
    settings: SettingsRepository,
) : ViewModel() {

    val state: ViewerUiState =
        if (queue.assets.isEmpty()) {
            ViewerUiState.Error("Nothing to show")
        } else {
            ViewerUiState.Ready(queue.startIndex)
        }

    private val _assets = MutableStateFlow(queue.assets)
    val assets: StateFlow<List<Asset>> = _assets.asStateFlow()

    /** Number of items just prepended; the pager must jump forward by this amount. */
    private val _prepended = MutableSharedFlow<Int>(extraBufferCapacity = 4)
    val prepended: SharedFlow<Int> = _prepended.asSharedFlow()

    val slideshowPrefs: StateFlow<SlideshowPrefs> =
        settings.slideshowPrefs.stateIn(viewModelScope, SharingStarted.Eagerly, SlideshowPrefs())

    // Cross-bucket paging state (timeline context only).
    // Buckets are newest-first, and assets within a month are newest-first, so the flattened order
    // matches the grid: END of the list continues into the NEXT (older) bucket, and the START into
    // the PREVIOUS (newer) one.
    private var newerBucketIndex = (queue.timeline?.bucketIndex ?: 0) - 1
    private var olderBucketIndex = (queue.timeline?.bucketIndex ?: -1) + 1
    private var loadingEdge = false

    /** Called on every pager page settle; extends the list near either edge. */
    fun onPageSettled(page: Int) {
        val nav = queue.timeline ?: return
        if (loadingEdge) return
        val size = _assets.value.size
        when {
            page >= size - EDGE_PREFETCH && olderBucketIndex <= nav.buckets.lastIndex -> {
                loadingEdge = true
                viewModelScope.launch {
                    runCatching { repository.getBucketAssets(nav.buckets[olderBucketIndex]) }
                        .onSuccess { bucket ->
                            olderBucketIndex++
                            if (bucket.isNotEmpty()) _assets.value = _assets.value + bucket
                        }
                    loadingEdge = false
                }
            }
            page <= EDGE_PREFETCH && newerBucketIndex >= 0 -> {
                loadingEdge = true
                viewModelScope.launch {
                    runCatching { repository.getBucketAssets(nav.buckets[newerBucketIndex]) }
                        .onSuccess { bucket ->
                            newerBucketIndex--
                            if (bucket.isNotEmpty()) {
                                _assets.value = bucket + _assets.value
                                _prepended.tryEmit(bucket.size)
                            }
                        }
                    loadingEdge = false
                }
            }
        }
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
