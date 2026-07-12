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
package com.immichtv.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Card
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.immichtv.domain.Asset
import com.immichtv.ui.common.AssetThumbnail
import com.immichtv.ui.common.CardFocus
import com.immichtv.ui.common.EmptyState
import com.immichtv.ui.common.ErrorState
import com.immichtv.ui.common.LoadingState
import com.immichtv.ui.theme.AppColors

@Composable
fun SearchScreen(
    onOpenViewer: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val str = com.immichtv.ui.i18n.LocalStrings.current
    val fieldFocus = remember { FocusRequester() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 48.dp, vertical = 36.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        SearchField(
            onSubmit = viewModel::search,
            modifier = Modifier.focusRequester(fieldFocus),
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (val s = state) {
                is SearchUiState.Idle ->
                    EmptyState(title = str.searchIdleTitle, subtitle = str.searchIdleSubtitle)
                is SearchUiState.Loading -> LoadingState(str.searchLoading)
                is SearchUiState.Error -> ErrorState(s.message, onRetry = viewModel::retry)
                is SearchUiState.Results ->
                    if (s.assets.isEmpty()) {
                        EmptyState(
                            title = str.searchNoResultsTitle,
                            subtitle = str.searchNoResultsSubtitle(s.query),
                        )
                    } else {
                        ResultsGrid(
                            assets = s.assets,
                            onOpen = { index -> viewModel.prepareViewer(index); onOpenViewer() },
                        )
                    }
            }
        }
    }

    // Open the keyboard straight away so the user can type immediately.
    LaunchedEffect(Unit) { runCatching { fieldFocus.requestFocus() } }
}

/** D-pad-focusable single-line search box; the IME "search" action runs the query. */
@Composable
private fun SearchField(onSubmit: (String) -> Unit, modifier: Modifier = Modifier) {
    var text by remember { mutableStateOf("") }
    var focused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val borderColor = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.border

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
            .border(2.dp, borderColor, RoundedCornerShape(10.dp))
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = AppColors.textSecondary,
            modifier = Modifier.size(24.dp),
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            if (text.isEmpty()) {
                Text(
                    text = com.immichtv.ui.i18n.LocalStrings.current.searchHint,
                    color = AppColors.textSecondary,
                    style = TextStyle(fontSize = 20.sp),
                )
            }
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    keyboard?.hide()
                    onSubmit(text)
                    focusManager.moveFocus(FocusDirection.Down) // move to the results
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focused = it.isFocused }
                    // A focused text field swallows D-pad down; release it so the remote reaches
                    // the results grid below.
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when (event.key) {
                            Key.DirectionDown -> focusManager.moveFocus(FocusDirection.Down)
                            else -> false
                        }
                    },
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ResultsGrid(assets: List<Asset>, onOpen: (Int) -> Unit) {
    val gridState = rememberLazyGridState()
    val firstCell = remember { FocusRequester() }
    Column {
        Text(
            text = com.immichtv.ui.i18n.LocalStrings.current.results(assets.size),
            color = AppColors.textSecondary,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            state = gridState,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier.focusRestorer().fillMaxSize(),
        ) {
            itemsIndexed(assets, key = { _, a -> a.id }) { index, asset ->
                Card(
                    onClick = { onOpen(index) },
                    border = CardFocus.border(),
                    scale = CardFocus.scale(),
                    modifier = (if (index == 0) Modifier.focusRequester(firstCell) else Modifier)
                        .aspectRatio(1f),
                ) {
                    AssetThumbnail(asset, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}
