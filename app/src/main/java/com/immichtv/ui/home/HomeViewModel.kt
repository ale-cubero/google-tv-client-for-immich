package com.immichtv.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.immichtv.data.ImmichRepository
import com.immichtv.data.ViewerQueue
import com.immichtv.data.prefs.SettingsRepository
import com.immichtv.domain.Album
import com.immichtv.domain.Asset
import com.immichtv.domain.Memory
import com.immichtv.domain.Person
import com.immichtv.ui.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeData(
    val memories: List<Memory> = emptyList(),
    val recent: List<Asset> = emptyList(),
    val albums: List<Album> = emptyList(),
    val people: List<Person> = emptyList(),
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
) : ViewModel() {

    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = HomeUiState.Loading
        viewModelScope.launch {
            // Fetch every rail in parallel; a single rail failing just leaves it empty.
            val memories = async { runCatching { repository.getMemories() } }
            val recent = async { runCatching { repository.getRecentAssets() } }
            val albums = async { runCatching { repository.getAlbums() } }
            val people = async { runCatching { repository.getPeople() } }
            val videos = async { runCatching { repository.getVideos() } }

            val memoriesR = memories.await()
            val recentR = recent.await()
            val albumsR = albums.await()
            val peopleR = people.await()
            val videosR = videos.await()
            val results = listOf(memoriesR, recentR, albumsR, peopleR, videosR)

            // Every rail failing means the server is unreachable (offline / network) or the key is
            // bad — show an actionable error with Retry. A partial failure just leaves that rail empty;
            // a reachable-but-empty library shows an (empty) Home rather than a false error.
            val firstFailure = results.firstOrNull { it.isFailure }?.exceptionOrNull()
            _state.value = if (results.all { it.isFailure } && firstFailure != null) {
                HomeUiState.Error(firstFailure.toUserMessage())
            } else {
                HomeUiState.Ready(
                    HomeData(
                        memories = memoriesR.getOrDefault(emptyList()),
                        recent = recentR.getOrDefault(emptyList()),
                        albums = albumsR.getOrDefault(emptyList()),
                        people = peopleR.getOrDefault(emptyList()),
                        videos = videosR.getOrDefault(emptyList()),
                    ),
                )
            }
        }
    }

    /** Stage an asset list for the viewer before navigating to it. */
    fun openAssets(assets: List<Asset>, index: Int) = viewerQueue.set(assets, index)

    /** Clear stored server URL + API key, then invoke [onDone] to navigate back to setup. */
    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            settings.clear()
            onDone()
        }
    }
}
