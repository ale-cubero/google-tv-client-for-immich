package com.immichtv.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.immichtv.BuildConfig
import com.immichtv.ui.common.AssetThumbnail
import com.immichtv.ui.common.CardFocus
import com.immichtv.ui.common.EmptyState
import com.immichtv.ui.common.ErrorState
import com.immichtv.ui.common.LoadingState
import com.immichtv.ui.common.QrCode
import com.immichtv.ui.common.brandButtonColors
import com.immichtv.ui.theme.AppColors
import com.immichtv.ui.theme.ThemeMode
import com.immichtv.domain.Album
import com.immichtv.domain.Asset
import com.immichtv.domain.Memory
import com.immichtv.domain.Person
import com.immichtv.domain.SlideshowPrefs

private const val REPO_URL = "https://github.com/ale-cubero/google-tv-client-for-immich"

@Composable
fun HomeScreen(
    onOpenTimeline: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenViewer: () -> Unit,
    onOpenAlbum: (id: String, title: String) -> Unit,
    onOpenPerson: (id: String, name: String) -> Unit,
    onChangeServer: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val slideshowPrefs by viewModel.slideshowPrefs.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    var showMenu by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showSlideshowSettings by remember { mutableStateOf(false) }
    var showAppearance by remember { mutableStateOf(false) }

    // Home stays on the back stack for the whole session — refresh stale data when it resurfaces.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshIfStale()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when (val s = state) {
            is HomeUiState.Loading -> LoadingState("Loading your library…")
            is HomeUiState.Error -> ErrorState(s.message, onRetry = viewModel::load)
            is HomeUiState.Ready -> HomeContent(
                data = s.data,
                openAssets = viewModel::openAssets,
                onOpenTimeline = onOpenTimeline,
                onOpenSearch = onOpenSearch,
                onOpenViewer = onOpenViewer,
                onOpenAlbum = onOpenAlbum,
                onOpenPerson = onOpenPerson,
                onOpenSettings = { showMenu = true },
            )
        }

        if (showMenu) {
            SettingsMenu(
                onChangeServer = { showMenu = false; onChangeServer() },
                onRefresh = { showMenu = false; viewModel.load() },
                onAppearance = { showMenu = false; showAppearance = true },
                onSlideshow = { showMenu = false; showSlideshowSettings = true },
                onLogout = { showMenu = false; viewModel.logout(onLoggedOut) },
                onAbout = { showMenu = false; showAbout = true },
                onDismiss = { showMenu = false },
            )
        }
        if (showAppearance) {
            AppearanceDialog(
                mode = themeMode,
                onSelect = viewModel::setThemeMode,
                onDismiss = { showAppearance = false },
            )
        }
        if (showSlideshowSettings) {
            SlideshowSettingsDialog(
                prefs = slideshowPrefs,
                onSave = viewModel::saveSlideshowPrefs,
                onDismiss = { showSlideshowSettings = false },
            )
        }
        if (showAbout) AboutDialog(onDismiss = { showAbout = false })
    }
}

@Composable
private fun HomeContent(
    data: HomeData,
    openAssets: (List<Asset>, Int) -> Unit,
    onOpenTimeline: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenViewer: () -> Unit,
    onOpenAlbum: (String, String) -> Unit,
    onOpenPerson: (String, String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val isEmpty = data == HomeData()
    val timelineFocus = remember { FocusRequester() }
    LazyColumn(
        contentPadding = PaddingValues(top = 40.dp, bottom = 60.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            // Header row: Timeline on the left, Settings on the right. Part of the scrolling
            // content so nothing floats over the rails.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 48.dp, end = 48.dp, bottom = 8.dp),
            ) {
                Button(
                    onClick = onOpenTimeline,
                    modifier = Modifier.focusRequester(timelineFocus),
                    colors = brandButtonColors(),
                ) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Timeline")
                }
                Spacer(modifier = Modifier.weight(1f))
                SearchBar(onClick = onOpenSearch, modifier = Modifier.width(420.dp))
                Spacer(modifier = Modifier.weight(1f))
                SettingsButton(onClick = onOpenSettings)
            }
        }

        if (isEmpty) {
            item {
                EmptyState(
                    title = "Your library is empty",
                    subtitle = "Photos and videos added in Immich will show up here.",
                    modifier = Modifier.fillMaxWidth().height(400.dp),
                )
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
private fun SearchBar(onClick: () -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    val border = if (focused) MaterialTheme.colorScheme.primary else AppColors.chromeBorder
    val fg = if (focused) MaterialTheme.colorScheme.onBackground else AppColors.onChrome
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .background(AppColors.chromeBg, RoundedCornerShape(22.dp))
            .border(if (focused) 2.dp else 1.dp, border, RoundedCornerShape(22.dp))
            .padding(horizontal = 20.dp, vertical = 11.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = "Search",
            tint = fg,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = "Search your photos and videos",
            color = fg,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun SettingsButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    val border = if (focused) MaterialTheme.colorScheme.primary else AppColors.chromeBorder
    val fg = if (focused) MaterialTheme.colorScheme.onBackground else AppColors.onChrome
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .background(AppColors.chromeBg, RoundedCornerShape(20.dp))
            .border(if (focused) 2.dp else 1.dp, border, RoundedCornerShape(20.dp))
            .padding(horizontal = 18.dp, vertical = 8.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Settings,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = "Settings",
            color = fg,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun SettingsMenu(
    onChangeServer: () -> Unit,
    onRefresh: () -> Unit,
    onAppearance: () -> Unit,
    onSlideshow: () -> Unit,
    onLogout: () -> Unit,
    onAbout: () -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(enabled = true, onBack = onDismiss)
    val firstItem = remember { FocusRequester() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.scrim)
            .clickable(onClick = onDismiss), // OK/click outside the panel dismisses
        contentAlignment = Alignment.TopEnd,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .padding(top = 84.dp, end = 48.dp)
                .width(360.dp)
                .background(AppColors.panel, RoundedCornerShape(14.dp))
                .border(1.dp, AppColors.outline, RoundedCornerShape(14.dp))
                .padding(10.dp),
        ) {
            MenuItem("Change server", Icons.Filled.Dns, Modifier.focusRequester(firstItem), onChangeServer)
            MenuItem("Refresh", Icons.Filled.Refresh, onClick = onRefresh)
            MenuItem("Appearance", Icons.Filled.Palette, onClick = onAppearance)
            MenuItem("Slideshow settings", Icons.Filled.Slideshow, onClick = onSlideshow)
            MenuItem("Logout", Icons.AutoMirrored.Filled.Logout, onClick = onLogout)
            MenuItem("About", Icons.Filled.Info, onClick = onAbout)
        }
    }
    LaunchedEffect(Unit) { runCatching { firstItem.requestFocus() } }
}

@Composable
private fun MenuItem(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .background(
                if (focused) AppColors.menuFocus else Color.Transparent,
                RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        val fg = if (focused) MaterialTheme.colorScheme.onBackground else AppColors.textSecondary
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = text,
            color = fg,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun SlideshowSettingsDialog(
    prefs: SlideshowPrefs,
    onSave: (SlideshowPrefs) -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(enabled = true, onBack = onDismiss)
    val firstFocus = remember { FocusRequester() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.scrim)
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .widthIn(max = 520.dp)
                .background(AppColors.panel, RoundedCornerShape(16.dp))
                .border(1.dp, AppColors.outline, RoundedCornerShape(16.dp))
                .padding(horizontal = 36.dp, vertical = 30.dp),
        ) {
            Text(
                text = "Slideshow",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Photo interval",
                color = AppColors.textSecondary,
                style = MaterialTheme.typography.labelLarge,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(3, 6, 10).forEachIndexed { i, seconds ->
                    OptionChip(
                        label = "${seconds}s",
                        selected = prefs.intervalSeconds == seconds,
                        modifier = if (i == 0) Modifier.focusRequester(firstFocus) else Modifier,
                        onClick = { onSave(prefs.copy(intervalSeconds = seconds)) },
                    )
                }
            }
            Text(
                text = "Order",
                color = AppColors.textSecondary,
                style = MaterialTheme.typography.labelLarge,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OptionChip(
                    label = "In order",
                    selected = !prefs.shuffle,
                    onClick = { onSave(prefs.copy(shuffle = false)) },
                )
                OptionChip(
                    label = "Shuffle",
                    selected = prefs.shuffle,
                    onClick = { onSave(prefs.copy(shuffle = true)) },
                )
            }
            Text(
                text = "Applies to the viewer slideshow (⏯) and the screensaver.",
                color = AppColors.textTertiary,
                style = MaterialTheme.typography.bodySmall,
            )
            Button(onClick = onDismiss, colors = brandButtonColors()) { Text("Done") }
        }
    }
    LaunchedEffect(Unit) { runCatching { firstFocus.requestFocus() } }
}

@Composable
private fun AppearanceDialog(
    mode: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(enabled = true, onBack = onDismiss)
    val firstFocus = remember { FocusRequester() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.scrim)
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .widthIn(max = 520.dp)
                .background(AppColors.panel, RoundedCornerShape(16.dp))
                .border(1.dp, AppColors.outline, RoundedCornerShape(16.dp))
                .padding(horizontal = 36.dp, vertical = 30.dp),
        ) {
            Text(
                text = "Appearance",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Theme",
                color = AppColors.textSecondary,
                style = MaterialTheme.typography.labelLarge,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconOptionChip(
                    label = "Dark",
                    icon = Icons.Filled.DarkMode,
                    selected = mode == ThemeMode.DARK,
                    modifier = Modifier.focusRequester(firstFocus),
                    onClick = { onSelect(ThemeMode.DARK) },
                )
                IconOptionChip(
                    label = "Light",
                    icon = Icons.Filled.LightMode,
                    selected = mode == ThemeMode.LIGHT,
                    onClick = { onSelect(ThemeMode.LIGHT) },
                )
            }
            Button(onClick = onDismiss, colors = brandButtonColors()) { Text("Done") }
        }
    }
    LaunchedEffect(Unit) { runCatching { firstFocus.requestFocus() } }
}

@Composable
private fun IconOptionChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val bg = when {
        selected -> MaterialTheme.colorScheme.primary
        focused -> AppColors.menuFocus
        else -> Color.Transparent
    }
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .background(bg, RoundedCornerShape(18.dp))
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) MaterialTheme.colorScheme.primary else AppColors.outline,
                shape = RoundedCornerShape(18.dp),
            )
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
        Text(text = label, color = fg, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun OptionChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val bg = when {
        selected -> MaterialTheme.colorScheme.primary
        focused -> AppColors.menuFocus
        else -> Color.Transparent
    }
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Box(
        modifier = modifier
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .background(bg, RoundedCornerShape(18.dp))
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) MaterialTheme.colorScheme.primary else AppColors.outline,
                shape = RoundedCornerShape(18.dp),
            )
            .padding(horizontal = 18.dp, vertical = 8.dp),
    ) {
        Text(text = label, color = fg, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    BackHandler(enabled = true, onBack = onDismiss)
    val closeFocus = remember { FocusRequester() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.scrim)
            .clickable(onClick = onDismiss), // click/OK outside the card dismisses
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .widthIn(max = 680.dp)
                .background(AppColors.panel, RoundedCornerShape(16.dp))
                .border(1.dp, AppColors.outline, RoundedCornerShape(16.dp))
                .padding(horizontal = 40.dp, vertical = 36.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "TV client for Immich",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    color = AppColors.textSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
                AboutRow("Developer", "Alejandro Cubero")
                AboutRow("GitHub", REPO_URL.removePrefix("https://"))
                Text(
                    text = "A read-only Android TV client for a self-hosted Immich server. " +
                        "Unofficial — not affiliated with the Immich project.",
                    color = AppColors.textSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.padding(top = 10.dp).focusRequester(closeFocus),
                    colors = brandButtonColors(),
                ) { Text("Close") }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                QrCode(content = REPO_URL)
                Text(
                    text = "Scan for the repo",
                    color = AppColors.textTertiary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
    // Focus the Close button so the dialog is immediately dismissable with OK or Back.
    LaunchedEffect(Unit) { runCatching { closeFocus.requestFocus() } }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row {
        Text(
            text = "$label:  ",
            color = AppColors.textTertiary,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
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
                contentDescription = "Album: ${album.name}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().background(AppColors.placeholder),
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
            color = AppColors.textSecondary,
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
                modifier = Modifier.fillMaxSize().background(AppColors.placeholder),
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
        Box(modifier = Modifier.fillMaxSize().background(AppColors.placeholder)) {
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
