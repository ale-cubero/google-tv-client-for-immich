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
package com.immichtv.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.SingletonImageLoader
import coil3.PlatformContext
import com.immichtv.data.ImmichRepository
import com.immichtv.data.ViewerQueue
import com.immichtv.data.prefs.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import com.immichtv.domain.Album
import com.immichtv.domain.Asset
import com.immichtv.domain.Memory
import com.immichtv.domain.Person
import com.immichtv.domain.Place
import com.immichtv.domain.SlideshowPrefs
import com.immichtv.ui.common.toUserMessage
import com.immichtv.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Home data older than this is re-fetched when the screen comes back to the foreground. */
private const val STALE_AFTER_MS = 5 * 60_000L

data class HomeData(
    val memories: List<Memory> = emptyList(),
    val recent: List<Asset> = emptyList(),
    val albums: List<Album> = emptyList(),
    val people: List<Person> = emptyList(),
    val places: List<Place> = emptyList(),
    val videos: List<Asset> = emptyList(),
)

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Error(val message: String) : HomeUiState
    data class Ready(val data: HomeData) : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ImmichRepository,
    private val viewerQueue: ViewerQueue,
    private val settings: SettingsRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    val slideshowPrefs: StateFlow<SlideshowPrefs> =
        settings.slideshowPrefs.stateIn(viewModelScope, SharingStarted.Eagerly, SlideshowPrefs())

    val themeMode: StateFlow<ThemeMode> =
        settings.themeMode.stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.DARK)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settings.saveThemeMode(mode) }
    }

    val language: StateFlow<com.immichtv.ui.i18n.Language> =
        settings.language.stateIn(viewModelScope, SharingStarted.Eagerly, com.immichtv.ui.i18n.Language.ENGLISH)

    fun setLanguage(language: com.immichtv.ui.i18n.Language) {
        viewModelScope.launch { settings.saveLanguage(language) }
    }

    private var loadedAt = 0L

    init {
        load()
        // Re-fetch when the language changes so server-derived text (memory titles like
        // "1 year ago", month labels) is re-localized, not just the static UI chrome.
        viewModelScope.launch {
            settings.language
                .drop(1)
                .distinctUntilChanged()
                .collect { load() }
        }
    }

    /** Home survives on the back stack; re-fetch when it resurfaces with stale data. */
    fun refreshIfStale() {
        if (_state.value is HomeUiState.Ready && System.currentTimeMillis() - loadedAt > STALE_AFTER_MS) {
            load()
        }
    }

    fun saveSlideshowPrefs(prefs: SlideshowPrefs) {
        viewModelScope.launch { settings.saveSlideshowPrefs(prefs) }
    }

    fun load() {
        _state.value = HomeUiState.Loading
        viewModelScope.launch {
            // Fetch every rail in parallel; a single rail failing just leaves it empty.
            val memories = async { runCatching { repository.getMemories() } }
            val recent = async { runCatching { repository.getRecentAssets() } }
            val albums = async { runCatching { repository.getAlbums() } }
            val people = async { runCatching { repository.getPeople() } }
            val places = async { runCatching { repository.getPlaces() } }
            val videos = async { runCatching { repository.getVideos() } }

            val memoriesR = memories.await()
            val recentR = recent.await()
            val albumsR = albums.await()
            val peopleR = people.await()
            val placesR = places.await()
            val videosR = videos.await()
            val results = listOf(memoriesR, recentR, albumsR, peopleR, placesR, videosR)

            // Every rail failing means the server is unreachable (offline / network) or the key is
            // bad — show an actionable error with Retry. A partial failure just leaves that rail empty;
            // a reachable-but-empty library shows an (empty) Home rather than a false error.
            val firstFailure = results.firstOrNull { it.isFailure }?.exceptionOrNull()
            loadedAt = System.currentTimeMillis()
            _state.value = if (results.all { it.isFailure } && firstFailure != null) {
                HomeUiState.Error(firstFailure.toUserMessage(settings.currentStrings()))
            } else {
                HomeUiState.Ready(
                    HomeData(
                        memories = memoriesR.getOrDefault(emptyList()),
                        recent = recentR.getOrDefault(emptyList()),
                        albums = albumsR.getOrDefault(emptyList()),
                        people = peopleR.getOrDefault(emptyList()),
                        places = placesR.getOrDefault(emptyList()),
                        videos = videosR.getOrDefault(emptyList()),
                    ),
                )
            }
        }
    }

    /** Stage an asset list for the viewer before navigating to it. */
    fun openAssets(assets: List<Asset>, index: Int) = viewerQueue.set(assets, index)

    /**
     * Clear stored server URL + API key AND all cached media (Coil memory + disk cache), so
     * thumbnails/previews of the library don't outlive the session on disk. Then [onDone]
     * navigates back to setup.
     */
    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            settings.clear()
            viewerQueue.set(emptyList(), 0) // drop any staged asset list
            withContext(Dispatchers.IO) {
                runCatching {
                    val loader = SingletonImageLoader.get(appContext as PlatformContext)
                    loader.memoryCache?.clear()
                    loader.diskCache?.clear()
                }
            }
            onDone()
        }
    }
}
