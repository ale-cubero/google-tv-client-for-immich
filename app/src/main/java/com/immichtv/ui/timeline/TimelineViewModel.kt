package com.immichtv.ui.timeline

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.immichtv.data.ImmichRepository
import com.immichtv.data.TimelineNav
import com.immichtv.data.ViewerQueue
import com.immichtv.domain.Asset
import com.immichtv.domain.BucketMeta
import com.immichtv.ui.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface TimelineUiState {
    data object Loading : TimelineUiState
    data class Error(val message: String) : TimelineUiState
    data class Ready(val buckets: List<BucketMeta>) : TimelineUiState
}

/**
 * Drives the timeline: loads all month buckets up front (extent known immediately), then fetches
 * each month's assets lazily as it scrolls into view, prefetching one bucket ahead in each
 * direction and evicting far-off buckets via an LRU so memory stays bounded on the 4 GB device.
 */
@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val repository: ImmichRepository,
    private val viewerQueue: ViewerQueue,
) : ViewModel() {

    private val _state = MutableStateFlow<TimelineUiState>(TimelineUiState.Loading)
    val state: StateFlow<TimelineUiState> = _state.asStateFlow()

    /** timeBucket -> its assets. Observable so cells recompose when their month arrives. */
    val loaded: SnapshotStateMap<String, List<Asset>> = mutableStateMapOf()

    private var order: Map<String, Int> = emptyMap()
    private var orderList: List<String> = emptyList()
    private val inFlight = mutableSetOf<String>()
    private val recency = LinkedHashSet<String>() // iteration order = LRU (oldest first)
    private var visible: Set<String> = emptySet()

    private val maxLoadedBuckets = 16

    init {
        load()
    }

    fun load() {
        _state.value = TimelineUiState.Loading
        viewModelScope.launch {
            runCatching { repository.getTimelineBuckets() }.fold(
                onSuccess = { buckets ->
                    order = buckets.mapIndexed { i, b -> b.timeBucket to i }.toMap()
                    orderList = buckets.map { it.timeBucket }
                    _state.value = TimelineUiState.Ready(buckets)
                },
                onFailure = { e ->
                    _state.value = TimelineUiState.Error(e.toUserMessage())
                },
            )
        }
    }

    fun assetAt(timeBucket: String, index: Int): Asset? = loaded[timeBucket]?.getOrNull(index)

    /** Stage the clicked month's assets for the viewer. Returns false if not loaded yet. */
    fun prepareViewer(timeBucket: String, index: Int): Boolean {
        val assets = loaded[timeBucket] ?: return false
        val bucketIndex = order[timeBucket] ?: return false
        // Timeline context lets the viewer keep paging into adjacent months past this bucket's edges.
        viewerQueue.set(assets, index, TimelineNav(orderList, bucketIndex))
        return true
    }

    /** Called as the visible set of month buckets changes during scroll. */
    fun onVisibleBuckets(visibleBuckets: Set<String>) {
        visible = visibleBuckets
        if (visibleBuckets.isEmpty()) return

        val indices = visibleBuckets.mapNotNull { order[it] }
        val toLoad = LinkedHashSet(visibleBuckets)
        // Prefetch one bucket ahead in each scroll direction.
        indices.maxOrNull()?.let { orderList.getOrNull(it + 1)?.let(toLoad::add) }
        indices.minOrNull()?.let { orderList.getOrNull(it - 1)?.let(toLoad::add) }

        toLoad.forEach(::ensureLoaded)
        evict()
    }

    private fun ensureLoaded(timeBucket: String) {
        if (timeBucket in loaded) {
            touch(timeBucket)
            return
        }
        if (timeBucket in inFlight) return
        inFlight += timeBucket
        viewModelScope.launch {
            runCatching { repository.getBucketAssets(timeBucket) }
                .onSuccess { loaded[timeBucket] = it; touch(timeBucket) }
            inFlight -= timeBucket
            evict()
        }
    }

    private fun touch(timeBucket: String) {
        recency.remove(timeBucket)
        recency.add(timeBucket) // most-recently used at the end
    }

    private fun evict() {
        if (loaded.size <= maxLoadedBuckets) return
        val iterator = recency.iterator()
        while (loaded.size > maxLoadedBuckets && iterator.hasNext()) {
            val candidate = iterator.next()
            if (candidate in visible) continue // never drop what's on screen
            iterator.remove()
            loaded.remove(candidate)
        }
    }
}
