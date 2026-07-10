package com.immichtv.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.immichtv.ui.common.AssetThumbnail
import com.immichtv.ui.common.CardFocus
import com.immichtv.ui.common.ErrorState
import com.immichtv.domain.Album
import com.immichtv.domain.Asset
import com.immichtv.domain.Memory
import com.immichtv.domain.Person

@Composable
fun HomeScreen(
    onOpenTimeline: () -> Unit,
    onOpenViewer: () -> Unit,
    onOpenAlbum: (id: String, title: String) -> Unit,
    onOpenPerson: (id: String, name: String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when (val s = state) {
            is HomeUiState.Loading -> Center("Loading…")
            is HomeUiState.Error -> ErrorState(s.message, onRetry = viewModel::load)
            is HomeUiState.Ready -> HomeContent(
                data = s.data,
                openAssets = viewModel::openAssets,
                onOpenTimeline = onOpenTimeline,
                onOpenViewer = onOpenViewer,
                onOpenAlbum = onOpenAlbum,
                onOpenPerson = onOpenPerson,
            )
        }
    }
}

@Composable
private fun HomeContent(
    data: HomeData,
    openAssets: (List<Asset>, Int) -> Unit,
    onOpenTimeline: () -> Unit,
    onOpenViewer: () -> Unit,
    onOpenAlbum: (String, String) -> Unit,
    onOpenPerson: (String, String) -> Unit,
) {
    val timelineFocus = remember { FocusRequester() }
    LazyColumn(
        contentPadding = PaddingValues(top = 40.dp, bottom = 60.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row0 {
                Button(
                    onClick = onOpenTimeline,
                    modifier = Modifier.focusRequester(timelineFocus),
                ) { Text("📅  Timeline") }
            }
        }

        if (data.memories.isNotEmpty()) item {
            Rail("On this day") {
                itemsIndexed(data.memories) { _, memory ->
                    MemoryTile(memory) {
                        openAssets(memory.assets, 0)
                        onOpenViewer()
                    }
                }
            }
        }

        if (data.recent.isNotEmpty()) item {
            Rail("Recent") {
                itemsIndexed(data.recent) { index, asset ->
                    AssetTile(asset) {
                        openAssets(data.recent, index)
                        onOpenViewer()
                    }
                }
            }
        }

        if (data.albums.isNotEmpty()) item {
            Rail("Albums") {
                itemsIndexed(data.albums) { _, album ->
                    AlbumTile(album) { onOpenAlbum(album.id, album.name) }
                }
            }
        }

        if (data.people.isNotEmpty()) item {
            Rail("People") {
                itemsIndexed(data.people) { _, person ->
                    PersonTile(person) { onOpenPerson(person.id, person.name) }
                }
            }
        }

        if (data.videos.isNotEmpty()) item {
            Rail("Videos") {
                itemsIndexed(data.videos) { index, asset ->
                    AssetTile(asset) {
                        openAssets(data.videos, index)
                        onOpenViewer()
                    }
                }
            }
        }
    }

    // Deterministic initial focus so the app always opens on the Timeline button.
    LaunchedEffect(Unit) { runCatching { timelineFocus.requestFocus() } }
}

@Composable
private fun Row0(content: @Composable () -> Unit) {
    Box(modifier = Modifier.padding(start = 48.dp, bottom = 8.dp)) { content() }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun Rail(
    title: String,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 48.dp, bottom = 10.dp),
        )
        LazyRow(
            // Restore the last-focused tile when returning to this rail.
            modifier = Modifier.focusRestorer(),
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}

@Composable
private fun AssetTile(asset: Asset, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        border = CardFocus.border(),
        scale = CardFocus.scale(),
        modifier = Modifier.size(180.dp),
    ) {
        AssetThumbnail(asset, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun AlbumTile(album: Album, onClick: () -> Unit) {
    Column(modifier = Modifier.width(200.dp)) {
        Card(
            onClick = onClick,
            border = CardFocus.border(),
            scale = CardFocus.scale(),
            modifier = Modifier.size(200.dp),
        ) {
            AsyncImage(
                model = album.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().background(Color(0xFF20262C)),
            )
        }
        Text(
            text = album.name,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            text = "${album.assetCount} items",
            color = Color(0xFF9AA4AD),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun PersonTile(person: Person, onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(140.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            onClick = onClick,
            border = CardFocus.border(),
            scale = CardFocus.scale(),
            modifier = Modifier.size(140.dp),
        ) {
            AsyncImage(
                model = person.thumbnailUrl,
                contentDescription = person.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().background(Color(0xFF20262C)),
            )
        }
        Text(
            text = person.name,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun MemoryTile(memory: Memory, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        border = CardFocus.border(),
        scale = CardFocus.scale(),
        modifier = Modifier.width(280.dp).height(160.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF20262C))) {
            memory.assets.firstOrNull()?.let { cover ->
                AssetThumbnail(cover, modifier = Modifier.fillMaxSize())
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000))),
                    ),
            )
            Text(
                text = memory.title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.BottomStart).padding(14.dp),
            )
        }
    }
}

@Composable
private fun Center(text: String, color: Color = Color.White) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = text, color = color, style = MaterialTheme.typography.headlineSmall)
    }
}
