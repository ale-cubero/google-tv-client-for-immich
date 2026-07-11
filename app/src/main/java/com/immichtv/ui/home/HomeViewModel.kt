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
            val memories = async { runCatching { repository.getMemories() }.getOrDefault(emptyList()) }
            val recent = async { runCatching { repository.getRecentAssets() }.getOrDefault(emptyList()) }
            val albums = async { runCatching { repository.getAlbums() }.getOrDefault(emptyList()) }
            val people = async { runCatching { repository.getPeople() }.getOrDefault(emptyList()) }
            val videos = async { runCatching { repository.getVideos() }.getOrDefault(emptyList()) }

            val data = HomeData(
                memories = memories.await(),
                recent = recent.await(),
                albums = albums.await(),
                people = people.await(),
                videos = videos.await(),
            )
            _state.value =
                if (data == HomeData()) HomeUiState.Error("Could not load your library")
                else HomeUiState.Ready(data)
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
