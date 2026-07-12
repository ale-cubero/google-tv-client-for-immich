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
    private val settings: com.immichtv.data.prefs.SettingsRepository,
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
                onFailure = { _state.value = SearchUiState.Error(it.toUserMessage(settings.currentStrings())) },
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
