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
package com.immichtv.ui.timeline

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Card
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.immichtv.ui.common.AssetThumbnail
import com.immichtv.ui.common.CardFocus
import com.immichtv.ui.common.EmptyState
import com.immichtv.ui.common.ErrorState
import com.immichtv.ui.common.LoadingState
import com.immichtv.ui.theme.AppColors
import com.immichtv.domain.Asset
import com.immichtv.domain.BucketMeta
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val COLUMNS = 6

// Focused row rests this far down from the top of the viewport (pivot feel). The year scrubber
// samples the same line so its highlight matches where you're actually looking.
private const val PIVOT_FRACTION = 0.35f

@Composable
fun TimelineScreen(
    onOpenViewer: () -> Unit = {},
    viewModel: TimelineViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val str = com.immichtv.ui.i18n.LocalStrings.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when (val s = state) {
            is TimelineUiState.Loading -> LoadingState(str.timelineLoading)
            is TimelineUiState.Error -> ErrorState(s.message, onRetry = viewModel::load)
            is TimelineUiState.Ready ->
                if (s.buckets.isEmpty()) {
                    EmptyState(title = str.timelineEmptyTitle, subtitle = str.emptyLibrarySubtitle)
                } else {
                    TimelineContent(s.buckets, viewModel, onOpenViewer)
                }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun TimelineContent(
    buckets: List<BucketMeta>,
    viewModel: TimelineViewModel,
    onOpenViewer: () -> Unit,
) {
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val firstCell = remember { FocusRequester() }
    val targetCell = remember { FocusRequester() }

    // Where focus should land when stepping LEFT out of the year scrubber back into the grid: the
    // visible cell nearest the pivot line (the year you just scrubbed to). Without this, the grid's
    // focusRestorer would restore the stale previously-focused cell — jumping to another year.
    val enterCell = remember { FocusRequester() }
    var enterCellKey by remember { mutableStateOf<String?>(null) }
    // The cell that opened the viewer, re-focused on return (replaces focusRestorer, which would
    // fight the scrubber-entry redirect above by restoring its own remembered cell instead).
    val restoreCell = remember { FocusRequester() }
    var restoreKey by remember { mutableStateOf<String?>(null) }
    val onEnterGrid: () -> Unit = {
        val info = gridState.layoutInfo
        val pivotY = info.viewportStartOffset +
            (info.viewportEndOffset - info.viewportStartOffset) * PIVOT_FRACTION
        enterCellKey = info.visibleItemsInfo
            .filter { (it.key as? String)?.startsWith("c:") == true }
            .minByOrNull { kotlin.math.abs((it.offset.y + it.size.height / 2f) - pivotY) }
            ?.key as? String
    }
    LaunchedEffect(enterCellKey) {
        if (enterCellKey != null) {
            runCatching { enterCell.requestFocus() }
            enterCellKey = null
        }
    }
    // Returning from the viewer: put focus back on the cell it was opened from.
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        if (restoreKey != null) scope.launch { runCatching { restoreCell.requestFocus() } }
    }

    // When arriving from the viewer's "Go to this day", resolve the month bucket to land on.
    val targetBucket = remember(buckets) {
        viewModel.targetMonth.takeIf { it.isNotBlank() }
            ?.let { m -> buckets.firstOrNull { it.timeBucket.take(7) == m } }
    }

    // Keep the focused row ~35% from the top instead of just barely on-screen (pivot feel).
    val pivotSpec = remember {
        object : BringIntoViewSpec {
            override fun calculateScrollDistance(
                offset: Float,
                size: Float,
                containerSize: Float,
            ): Float = offset - containerSize * PIVOT_FRACTION
        }
    }

    // Report which month buckets are visible so the ViewModel can load/prefetch/evict.
    LaunchedEffect(gridState) {
        snapshotFlow {
            gridState.layoutInfo.visibleItemsInfo.mapNotNull { bucketOfKey(it.key) }.toSet()
        }.distinctUntilChanged().collect { viewModel.onVisibleBuckets(it) }
    }

    // Track the year the user is actually looking at so the scrubber can indicate where you are.
    // Sample the item nearest the focus pivot line (~35% down), NOT the top edge: the focused row
    // is pivoted 35% down, so the top of the viewport shows the previous (newer) year's tail —
    // reading that would light up the year *above* the one you're on.
    var currentYear by remember { androidx.compose.runtime.mutableIntStateOf(buckets.firstOrNull()?.year ?: 0) }
    LaunchedEffect(gridState, buckets) {
        snapshotFlow {
            val info = gridState.layoutInfo
            val pivotY = info.viewportStartOffset +
                (info.viewportEndOffset - info.viewportStartOffset) * PIVOT_FRACTION
            info.visibleItemsInfo
                .minByOrNull { kotlin.math.abs((it.offset.y + it.size.height / 2f) - pivotY) }
                ?.key?.let(::bucketOfKey)
        }
            .distinctUntilChanged()
            .collect { tb ->
                buckets.firstOrNull { it.timeBucket == tb }?.let { currentYear = it.year }
            }
    }

    Row(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            CompositionLocalProvider(LocalBringIntoViewSpec provides pivotSpec) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(COLUMNS),
                    state = gridState,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(start = 48.dp, end = 16.dp, top = 27.dp, bottom = 80.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    buckets.forEachIndexed { bucketIndex, bucket ->
                        item(
                            span = { GridItemSpan(maxLineSpan) },
                            key = "h:${bucket.timeBucket}",
                            contentType = "header",
                        ) {
                            MonthHeader(bucket.label)
                        }
                        items(
                            count = bucket.count,
                            key = { i -> "c:${bucket.timeBucket}:$i" },
                            contentType = { "cell" },
                        ) { i ->
                            val asset = viewModel.assetAt(bucket.timeBucket, i)
                            val isTargetCell = targetBucket?.timeBucket == bucket.timeBucket &&
                                viewModel.targetAssetId.isNotBlank() && asset?.id == viewModel.targetAssetId
                            val cellKey = "c:${bucket.timeBucket}:$i"
                            val cellModifier = when {
                                enterCellKey == cellKey -> Modifier.focusRequester(enterCell)
                                restoreKey == cellKey -> Modifier.focusRequester(restoreCell)
                                isTargetCell -> Modifier.focusRequester(targetCell)
                                bucketIndex == 0 && i == 0 -> Modifier.focusRequester(firstCell)
                                else -> Modifier
                            }
                            ThumbnailCell(
                                asset = asset,
                                modifier = cellModifier,
                                onClick = {
                                    restoreKey = cellKey
                                    if (viewModel.prepareViewer(bucket.timeBucket, i)) onOpenViewer()
                                },
                            )
                        }
                    }
                }
            }
        }

        YearScrubber(
            buckets = buckets,
            currentYear = currentYear,
            onScrubToIndex = { flatIndex -> scope.launch { gridState.scrollToItem(flatIndex) } },
            onEnterGrid = onEnterGrid,
            modifier = Modifier.fillMaxHeight(),
        )
    }

    if (targetBucket == null) {
        LaunchedEffect(Unit) {
            // First cell is a focusable placeholder even before its image loads.
            runCatching { firstCell.requestFocus() }
        }
    } else {
        // Arrived from "Go to this day": jump to the month, wait for it to load, then land on the
        // exact asset. Scrolling to the header makes the bucket visible so the ViewModel loads it.
        LaunchedEffect(targetBucket.timeBucket) {
            runCatching { gridState.scrollToItem(targetBucket.flatIndex) }
            val idx = withTimeoutOrNull(6_000) {
                snapshotFlow { viewModel.assetIndex(targetBucket.timeBucket, viewModel.targetAssetId) }
                    .first { it >= 0 }
            }
            // Compose the exact cell (it may be far into a large month), then focus it.
            runCatching { gridState.scrollToItem(targetBucket.flatIndex + 1 + (idx ?: 0)) }
            runCatching { targetCell.requestFocus() }
                .onFailure { runCatching { firstCell.requestFocus() } }
        }
    }
}

@Composable
private fun MonthHeader(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun ThumbnailCell(asset: Asset?, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Card(
        onClick = onClick,
        border = CardFocus.border(),
        scale = CardFocus.scale(),
        modifier = modifier.aspectRatio(1f),
    ) {
        if (asset != null) {
            AssetThumbnail(asset, modifier = Modifier.fillMaxSize())
        } else {
            Box(modifier = Modifier.fillMaxSize().background(AppColors.placeholder))
        }
    }
}

@Composable
private fun YearScrubber(
    buckets: List<BucketMeta>,
    currentYear: Int,
    onScrubToIndex: (Int) -> Unit,
    onEnterGrid: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // First bucket (flat index) of each distinct year, newest first.
    val years = remember(buckets) {
        buckets.groupBy { it.year }
            .map { (year, group) -> year to group.minByOrNull { it.flatIndex }!!.flatIndex }
            .sortedByDescending { it.first }
    }

    LazyColumn(
        modifier = modifier
            .width(88.dp)
            .background(AppColors.rail),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 27.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items(years, key = { it.first }) { (year, flatIndex) ->
            var focused by remember { mutableStateOf(false) }
            val isCurrent = year == currentYear
            Text(
                text = year.toString(),
                // Focused = inverted chip; current-but-unfocused = accent so you can always see
                // where you are in the library while scrolling the grid.
                color = when {
                    focused -> MaterialTheme.colorScheme.onPrimary
                    isCurrent -> MaterialTheme.colorScheme.primary
                    else -> AppColors.textSecondary
                },
                fontWeight = if (isCurrent || focused) FontWeight.Bold else FontWeight.Normal,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
                    .background(
                        if (focused) MaterialTheme.colorScheme.primary else Color.Transparent,
                    )
                    // Stepping LEFT into the grid: land on the year now on screen, not the stale cell
                    // the grid's focusRestorer would otherwise restore (which jumps to another year).
                    // Preview phase: intercept before the focus system runs its default LEFT traversal.
                    .onPreviewKeyEvent { e ->
                        if (e.type == KeyEventType.KeyDown && e.key == Key.DirectionLeft) {
                            onEnterGrid(); true
                        } else {
                            false
                        }
                    }
                    .onFocusChanged {
                        focused = it.isFocused
                        if (it.isFocused) onScrubToIndex(flatIndex)
                    }
                    .focusable(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

/** timeBucket embedded in a LazyGrid item key ("h:tb" or "c:tb:idx"). */
private fun bucketOfKey(key: Any?): String? {
    val s = key as? String ?: return null
    return when {
        s.startsWith("h:") -> s.removePrefix("h:")
        s.startsWith("c:") -> s.substring(2, s.lastIndexOf(':'))
        else -> null
    }
}
