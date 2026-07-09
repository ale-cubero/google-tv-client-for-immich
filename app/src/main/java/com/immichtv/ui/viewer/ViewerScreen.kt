package com.immichtv.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.immichtv.domain.Asset
import com.immichtv.domain.AssetDetail
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SEEK_STEP_MS = 10_000L
private const val SLIDESHOW_PHOTO_MS = 6_000L

@Composable
fun ViewerScreen(
    viewModel: ViewerViewModel = hiltViewModel(),
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        when (val s = viewModel.state) {
            is ViewerUiState.Error -> Text(
                s.message,
                color = Color(0xFFFF6E6E),
                modifier = Modifier.align(Alignment.Center),
            )

            is ViewerUiState.Ready -> ViewerPager(
                assets = s.assets,
                startIndex = s.startIndex,
                player = rememberExoPlayer(viewModel),
                viewModel = viewModel,
            )
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun rememberExoPlayer(viewModel: ViewerViewModel): ExoPlayer {
    val context = LocalContext.current
    val player = remember {
        val dataSourceFactory = OkHttpDataSource.Factory(viewModel.okHttpClient)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(dataSourceFactory))
            .build()
            .apply { repeatMode = Player.REPEAT_MODE_ONE }
    }
    DisposableEffect(Unit) {
        onDispose { player.release() }
    }
    return player
}

@Composable
private fun ViewerPager(
    assets: List<Asset>,
    startIndex: Int,
    player: ExoPlayer,
    viewModel: ViewerViewModel,
) {
    val pagerState = rememberPagerState(initialPage = startIndex) { assets.size }
    val scope = rememberCoroutineScope()
    val focus = remember { FocusRequester() }
    val detail by viewModel.detail.collectAsStateWithLifecycle()

    var isPlaying by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var slideshow by remember { mutableStateOf(false) }
    // Per-direction tap vs hold tracking (hold = seek on video, tap = prev/next asset).
    var pendingTap by remember { mutableIntStateOf(0) } // -1 left, +1 right, 0 none

    fun advance() {
        val target = if (pagerState.currentPage >= assets.lastIndex) 0 else pagerState.currentPage + 1
        scope.launch { pagerState.animateScrollToPage(target) }
    }

    // Video end advances the slideshow; keep latest slideshow flag readable inside the listener.
    val slideshowNow by rememberUpdatedState(slideshow)
    val advanceNow by rememberUpdatedState(::advance)
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED && slideshowNow) advanceNow()
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    // In slideshow, videos play once then advance; normally they loop.
    LaunchedEffect(slideshow) {
        player.repeatMode = if (slideshow) Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ONE
    }

    // Auto-play the video when its page settles; stop audio on photo pages.
    LaunchedEffect(pagerState.settledPage) {
        val asset = assets.getOrNull(pagerState.settledPage) ?: return@LaunchedEffect
        if (asset.isVideo) {
            player.setMediaItem(MediaItem.fromUri(asset.videoUrl))
            player.prepare()
            player.playWhenReady = true
        } else {
            player.playWhenReady = false
            player.clearMediaItems()
        }
    }

    // Slideshow photo timer (videos advance on end via the listener above).
    LaunchedEffect(slideshow, pagerState.currentPage) {
        if (!slideshow) return@LaunchedEffect
        val asset = assets.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
        if (!asset.isVideo) {
            delay(SLIDESHOW_PHOTO_MS)
            if (slideshow) advance()
        }
    }

    // Load metadata for the panel when it's shown / the page changes under it.
    LaunchedEffect(showInfo, pagerState.currentPage) {
        if (showInfo) assets.getOrNull(pagerState.currentPage)?.let { viewModel.loadDetail(it.id) }
    }

    val current = assets.getOrNull(pagerState.currentPage)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focus)
            .focusable()
            .onPreviewKeyEvent { event ->
                val onVideo = current?.isVideo == true
                when (event.type) {
                    KeyEventType.KeyDown -> when (event.key) {
                        Key.MediaPlayPause, Key.MediaPlay -> {
                            slideshow = !slideshow
                            if (slideshow) showInfo = false
                            true
                        }
                        Key.DirectionRight -> {
                            slideshow = false
                            handleHorizontal(+1, event.nativeKeyEvent.repeatCount, onVideo, player) { pendingTap = it }
                        }
                        Key.DirectionLeft -> {
                            slideshow = false
                            handleHorizontal(-1, event.nativeKeyEvent.repeatCount, onVideo, player) { pendingTap = it }
                        }
                        Key.DirectionCenter, Key.Enter -> {
                            slideshow = false
                            if (onVideo) {
                                if (player.isPlaying) player.pause() else player.play()
                            }
                            true
                        }
                        Key.DirectionDown -> { slideshow = false; showInfo = !showInfo; true }
                        Key.DirectionUp -> { showInfo = false; true }
                        else -> false
                    }

                    KeyEventType.KeyUp -> when (event.key) {
                        Key.DirectionRight, Key.DirectionLeft -> {
                            // A tap (no key repeats fired) navigates to prev/next asset.
                            if (pendingTap != 0) {
                                val target = (pagerState.currentPage + pendingTap)
                                    .coerceIn(0, assets.lastIndex)
                                scope.launch { pagerState.animateScrollToPage(target) }
                                pendingTap = 0
                            }
                            true
                        }
                        else -> false
                    }

                    else -> false
                }
            },
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
        ) { page ->
            val asset = assets[page]
            when {
                asset.isVideo && page == pagerState.currentPage ->
                    VideoPage(player, poster = asset)
                else -> PhotoPage(asset)
            }
        }

        if (slideshow) {
            Text(
                text = "▶ Slideshow",
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(24.dp)
                    .background(Color(0x88000000))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }

        if (showInfo) {
            MetadataPanel(
                detail = detail,
                asset = current,
                position = pagerState.currentPage + 1,
                total = assets.size,
                isPlaying = isPlaying,
            )
        }
    }

    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
}

/**
 * On key-down for left/right: on a video, a *held* key (repeatCount > 0) seeks; the initial press
 * is deferred to key-up so we can tell a tap from a hold. On a photo, tap navigates immediately.
 * Returns whether the event was consumed; [markTap] records a pending tap direction.
 */
private fun handleHorizontal(
    direction: Int,
    repeatCount: Int,
    onVideo: Boolean,
    player: ExoPlayer,
    markTap: (Int) -> Unit,
): Boolean {
    if (onVideo) {
        if (repeatCount == 0) {
            markTap(direction) // decide tap vs hold on key-up / first repeat
        } else {
            markTap(0) // it became a hold → seek, don't navigate
            val target = (player.currentPosition + direction * SEEK_STEP_MS)
                .coerceIn(0, if (player.duration > 0) player.duration else Long.MAX_VALUE)
            player.seekTo(target)
        }
        return true
    }
    // Photo: navigate on the initial press only.
    if (repeatCount == 0) markTap(direction) else markTap(0)
    return true
}

@Composable
private fun PhotoPage(asset: Asset) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Cached grid thumbnail shows instantly as a placeholder behind the preview.
        AsyncImage(
            model = asset.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
        AsyncImage(
            model = asset.previewUrl,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun VideoPage(player: ExoPlayer, poster: Asset) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AsyncImage(
            model = poster.previewUrl,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    this.player = player
                }
            },
            update = { it.player = player },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.MetadataPanel(
    detail: AssetDetail?,
    asset: Asset?,
    position: Int,
    total: Int,
    isPlaying: Boolean,
) {
    Column(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .fillMaxWidth()
            .background(Color(0xDD000000))
            .padding(horizontal = 48.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val kind = when {
            asset?.isVideo == true -> "Video · " + if (isPlaying) "playing" else "paused"
            else -> "Photo"
        }
        Text(
            text = detail?.fileName?.takeIf { it.isNotBlank() } ?: kind,
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "$position / $total   ·   $kind",
            color = Color(0xFFBFC7CE),
            style = MaterialTheme.typography.bodyMedium,
        )
        if (detail == null) {
            Text("Loading details…", color = Color(0xFF8A929A), style = MaterialTheme.typography.bodyMedium)
        } else {
            InfoLine(detail.dateTaken)
            InfoLine(detail.camera)
            InfoLine(detail.exposure)
            InfoLine(detail.dimensions)
            InfoLine(detail.location)
            InfoLine(detail.people.joinToString(", ").takeIf { it.isNotBlank() }?.let { "People: $it" } ?: "")
        }
    }
}

@Composable
private fun InfoLine(text: String) {
    if (text.isBlank()) return
    Text(text = text, color = Color.White, style = MaterialTheme.typography.bodyLarge)
}
