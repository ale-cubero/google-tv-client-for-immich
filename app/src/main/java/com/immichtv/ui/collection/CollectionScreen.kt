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
package com.immichtv.ui.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import com.immichtv.domain.Asset

@Composable
fun CollectionScreen(
    onOpenViewer: () -> Unit,
    viewModel: CollectionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val str = com.immichtv.ui.i18n.LocalStrings.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when (val s = state) {
            is CollectionUiState.Loading -> LoadingState(str.collectionLoading)
            is CollectionUiState.Error -> ErrorState(s.message, onRetry = viewModel::load)
            is CollectionUiState.Ready ->
                if (s.assets.isEmpty()) {
                    EmptyState(
                        title = s.title.ifBlank { str.collectionEmptyTitle },
                        subtitle = str.collectionEmptySubtitle,
                    )
                } else {
                    CollectionGrid(
                        title = s.title,
                        assets = s.assets,
                        onOpen = { index ->
                            viewModel.prepareViewer(index)
                            onOpenViewer()
                        },
                    )
                }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun CollectionGrid(title: String, assets: List<Asset>, onOpen: (Int) -> Unit) {
    val gridState = rememberLazyGridState()
    val firstCell = remember { FocusRequester() }
    val str = com.immichtv.ui.i18n.LocalStrings.current

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "$title  ·  ${str.items(assets.size)}",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 48.dp, top = 40.dp, bottom = 16.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            state = gridState,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 20.dp),
            modifier = Modifier.focusRestorer().fillMaxSize(),
        ) {
            itemsIndexed(assets, key = { _, a -> a.id }) { index, asset ->
                Cell(
                    asset = asset,
                    onClick = { onOpen(index) },
                    modifier = if (index == 0) Modifier.focusRequester(firstCell) else Modifier,
                )
            }
        }
    }

    LaunchedEffect(assets.isNotEmpty()) {
        if (assets.isNotEmpty()) runCatching { firstCell.requestFocus() }
    }
}

@Composable
private fun Cell(asset: Asset, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        border = CardFocus.border(),
        scale = CardFocus.scale(),
        modifier = modifier.aspectRatio(1f),
    ) {
        AssetThumbnail(asset, modifier = Modifier.fillMaxSize())
    }
}

