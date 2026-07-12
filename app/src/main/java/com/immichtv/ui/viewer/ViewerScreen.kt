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
package com.immichtv.ui.viewer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.immichtv.R
import com.immichtv.domain.Asset
import com.immichtv.ui.common.Spinner
import com.immichtv.ui.theme.AppColors
import com.immichtv.ui.theme.ImmichTvTheme
import com.immichtv.ui.theme.ThemeMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SEEK_STEP_MS = 10_000L
private const val HINTS_MS = 6_000L
private const val SCRUB_HIDE_MS = 3_000L
private const val POSITION_CHIP_MS = 2_000L

@Composable
fun ViewerScreen(
    onOpenTimelineAt: (month: String, assetId: String) -> Unit = { _, _ -> },
    viewModel: ViewerViewModel = hiltViewModel(),
) {
    // Full-screen media viewer: always dark, regardless of the app's light/dark setting.
    ImmichTvTheme(mode = ThemeMode.DARK) {
        ViewerContent(viewModel, onOpenTimelineAt)
    }
}

@Composable
private fun ViewerContent(
    viewModel: ViewerViewModel,
    onOpenTimelineAt: (month: String, assetId: String) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        when (val s = viewModel.state) {
            is ViewerUiState.Error -> Text(
                s.message,
                color = AppColors.error,
                modifier = Modifier.align(Alignment.Center),
            )

            is ViewerUiState.Ready -> ViewerPager(
                startIndex = s.startIndex,
                player = rememberExoPlayer(viewModel),
                viewModel = viewModel,
                onOpenTimelineAt = onOpenTimelineAt,
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
    startIndex: Int,
    player: ExoPlayer,
    viewModel: ViewerViewModel,
    onOpenTimelineAt: (month: String, assetId: String) -> Unit,
) {
    // The asset list can grow while paging (cross-month continuation from the timeline).
    val assets by viewModel.assets.collectAsStateWithLifecycle()
    val slideshowPrefs by viewModel.slideshowPrefs.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(initialPage = startIndex) { assets.size }
    val scope = rememberCoroutineScope()
    val focus = remember { FocusRequester() }
    val detail by viewModel.detail.collectAsStateWithLifecycle()

    var isPlaying by remember { mutableStateOf(false) }
    var buffering by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var slideshow by remember { mutableStateOf(false) }
    // A motion photo (live photo) is playing its clip over the still.
    var motionPlaying by remember { mutableStateOf(false) }
    // Decoded video color info (for the HDR indicator); updated when a video renders.
    var videoColorInfo by remember { mutableStateOf<androidx.media3.common.ColorInfo?>(null) }
    // Latched true when a video carries a rotation a SurfaceView can't apply (portrait clips), so it
    // must render on a TextureView instead. One-way per page: a TextureView reports 0 unapplied
    // rotation, which must not flip it back (that would loop). Reset on page change.
    var videoNeedsTexture by remember { mutableStateOf(false) }
    // Set when ExoPlayer can't load/play a video (e.g. server went offline mid-session).
    var videoError by remember { mutableStateOf(false) }
    // Per-direction tap vs hold tracking (hold = seek on video, tap = prev/next asset).
    var pendingTap by remember { mutableIntStateOf(0) } // -1 left, +1 right, 0 none
    // Control hints shown briefly when the viewer opens; any key dismisses early.
    var showHints by remember { mutableStateOf(true) }
    // Transient scrub bar state (videos): visible while paused, or briefly after a seek.
    var scrubKick by remember { mutableIntStateOf(0) }
    var showScrub by remember { mutableStateOf(false) }
    var scrubPositionMs by remember { mutableLongStateOf(0L) }
    var scrubDurationMs by remember { mutableLongStateOf(0L) }
    // Transient "n / total" chip on page change.
    var showPosition by remember { mutableStateOf(false) }
    val panelScroll = rememberScrollState()

    fun advance() {
        val target = when {
            slideshowPrefs.shuffle && assets.size > 1 ->
                generateSequence { assets.indices.random() }.first { it != pagerState.currentPage }
            pagerState.currentPage >= assets.lastIndex -> 0
            else -> pagerState.currentPage + 1
        }
        scope.launch { pagerState.animateScrollToPage(target) }
    }

    // Playback ended: advance the slideshow (real video) or settle a motion photo onto its still.
    val slideshowNow by rememberUpdatedState(slideshow)
    val advanceNow by rememberUpdatedState(::advance)
    val currentAssetNow by rememberUpdatedState(assets.getOrNull(pagerState.currentPage))
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                // Format (incl. color/HDR info) is available once the video renders.
                val format = player.videoFormat
                format?.colorInfo?.let { videoColorInfo = it }
                // Route any video with source rotation metadata to a TextureView (which applies the
                // rotation matrix reliably). We use the source Format.rotationDegrees rather than
                // VideoSize.unappliedRotationDegrees because some decoders report 0 unapplied while
                // still outputting the un-rotated buffer (portrait shown stretched in a landscape
                // frame). Latch only — never unlatch here (reset happens on page change).
                if ((format?.rotationDegrees ?: 0) != 0 || videoSize.unappliedRotationDegrees != 0) {
                    videoNeedsTexture = true
                }
            }

            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                // The selected video track carries colour info and source rotation before the first
                // frame renders — pick the surface (SurfaceView vs TextureView) as early as possible.
                val format = tracks.groups
                    .firstOrNull { it.type == C.TRACK_TYPE_VIDEO && it.isSelected }
                    ?.let { group -> (0 until group.length).firstOrNull { group.isTrackSelected(it) }?.let { group.getTrackFormat(it) } }
                format?.colorInfo?.let { videoColorInfo = it }
                if ((format?.rotationDegrees ?: 0) != 0) videoNeedsTexture = true
            }

            override fun onPlaybackStateChanged(state: Int) {
                buffering = state == Player.STATE_BUFFERING
                if (state != Player.STATE_ENDED) return
                val asset = currentAssetNow
                when {
                    asset?.isVideo == true && slideshowNow -> advanceNow()
                    asset?.hasMotion == true -> motionPlaying = false // show the still
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                // Server offline / network drop / undecodable stream: surface it instead of a frozen
                // poster. In a slideshow, keep moving so one bad video doesn't stall the show.
                videoError = true
                motionPlaying = false
                if (slideshowNow) advanceNow()
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    // A prepend (older pager index shift) grew the list at the front: jump the pager by the same
    // amount, without animation, so the same asset stays on screen.
    LaunchedEffect(Unit) {
        viewModel.prepended.collect { n ->
            pagerState.scrollToPage(pagerState.currentPage + n)
        }
    }

    // Loop a real video (unless in slideshow); motion clips and slideshow videos play once.
    LaunchedEffect(slideshow, pagerState.settledPage) {
        val asset = assets.getOrNull(pagerState.settledPage)
        player.repeatMode =
            if (asset?.isVideo == true && !slideshow) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    // On page settle: auto-play a real video; photos (incl. motion photos) show the still.
    // A motion photo's clip plays on demand (OK), not automatically.
    var preparedAssetId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(pagerState.settledPage) {
        val asset = assets.getOrNull(pagerState.settledPage) ?: return@LaunchedEffect
        viewModel.onPageSettled(pagerState.settledPage)
        // An index shift from a prepend settles on the SAME asset — don't restart its playback.
        if (asset.id == preparedAssetId) return@LaunchedEffect
        preparedAssetId = asset.id
        motionPlaying = false
        videoError = false
        videoColorInfo = null // reset; the listener fills it in when the new video renders
        videoNeedsTexture = false // start every video on a SurfaceView (HDR); latch if rotation needs it
        scrubKick = 0
        panelScroll.scrollTo(0)
        if (asset.isVideo) {
            player.setMediaItem(MediaItem.fromUri(asset.videoUrl))
            player.prepare()
            player.playWhenReady = true
        } else {
            player.playWhenReady = false
            player.clearMediaItems()
        }
    }

    fun playMotion() {
        val asset = assets.getOrNull(pagerState.currentPage) ?: return
        if (!asset.hasMotion) return
        player.setMediaItem(MediaItem.fromUri(asset.motionVideoUrl))
        player.prepare()
        player.playWhenReady = true
        motionPlaying = true
    }

    // Slideshow photo timer (videos advance on end via the listener above).
    LaunchedEffect(slideshow, pagerState.currentPage, slideshowPrefs.intervalSeconds) {
        if (!slideshow) return@LaunchedEffect
        val asset = assets.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
        if (!asset.isVideo) {
            delay(slideshowPrefs.intervalSeconds * 1_000L)
            if (slideshow) advance()
        }
    }

    // Load metadata for the panel when it's shown / the page changes under it.
    LaunchedEffect(showInfo, pagerState.currentPage) {
        if (showInfo) assets.getOrNull(pagerState.currentPage)?.let { viewModel.loadDetail(it.id) }
    }

    val current = assets.getOrNull(pagerState.currentPage)
    val context = LocalContext.current
    val str = com.immichtv.ui.i18n.LocalStrings.current
    val hdrLabel = if (current?.isVideo == true) VideoHdr.label(videoColorInfo, context, str) else null

    // Hints fade out on their own; any key press dismisses them early (handled in the key listener).
    LaunchedEffect(Unit) {
        delay(HINTS_MS)
        showHints = false
    }

    // Scrub bar visibility: pinned while a video is paused; else briefly after seek activity.
    LaunchedEffect(current?.id, isPlaying, scrubKick, motionPlaying) {
        val onVideoNow = current?.isVideo == true || motionPlaying
        when {
            !onVideoNow -> showScrub = false
            !isPlaying -> showScrub = true // paused: keep it up
            scrubKick > 0 -> {
                showScrub = true
                delay(SCRUB_HIDE_MS)
                showScrub = false
            }
            else -> showScrub = false
        }
    }
    // While visible, poll the player position.
    LaunchedEffect(showScrub) {
        while (showScrub) {
            scrubPositionMs = player.currentPosition.coerceAtLeast(0)
            scrubDurationMs = player.duration.coerceAtLeast(0)
            delay(250)
        }
    }

    // Transient position chip on page change (skipped when the change was a prepend index shift).
    var chipAssetId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(pagerState.currentPage) {
        val asset = assets.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
        if (chipAssetId == null) { chipAssetId = asset.id; return@LaunchedEffect } // initial page: no chip
        if (asset.id == chipAssetId) return@LaunchedEffect
        chipAssetId = asset.id
        showPosition = true
        delay(POSITION_CHIP_MS)
        showPosition = false
    }

    // Back closes the metadata panel before it exits the viewer.
    BackHandler(enabled = showInfo) { showInfo = false }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focus)
            .focusable()
            .onPreviewKeyEvent { event ->
                val onVideo = current?.isVideo == true
                if (event.type == KeyEventType.KeyDown) showHints = false
                when (event.type) {
                    KeyEventType.KeyDown -> when (event.key) {
                        Key.MediaPlayPause, Key.MediaPlay -> {
                            slideshow = !slideshow
                            if (slideshow) showInfo = false
                            true
                        }
                        Key.DirectionRight -> {
                            slideshow = false
                            handleHorizontal(+1, event.nativeKeyEvent.repeatCount, onVideo, player,
                                onSeek = { scrubKick++ }) { pendingTap = it }
                        }
                        Key.DirectionLeft -> {
                            slideshow = false
                            handleHorizontal(-1, event.nativeKeyEvent.repeatCount, onVideo, player,
                                onSeek = { scrubKick++ }) { pendingTap = it }
                        }
                        Key.DirectionCenter, Key.Enter -> {
                            slideshow = false
                            val cur = current
                            when {
                                // Detail panel open on a dated asset: OK jumps to that asset's day in
                                // the timeline (the "Go to this day" button shown in the panel).
                                showInfo && cur != null && cur.dateTime.isNotBlank() ->
                                    onOpenTimelineAt(cur.dateTime.take(7), cur.id)
                                // Otherwise OK: play/pause a video, play a motion photo's clip on
                                // demand, or toggle the detail panel on a plain photo. (Details = Down.)
                                onVideo -> if (player.isPlaying) player.pause() else player.play()
                                cur?.hasMotion == true -> { showInfo = false; playMotion() }
                                else -> showInfo = !showInfo
                            }
                            true
                        }
                        Key.DirectionDown -> {
                            slideshow = false
                            // Panel open: scroll it. Closed: open it.
                            if (showInfo) scope.launch { panelScroll.animateScrollBy(220f) }
                            else showInfo = true
                            true
                        }
                        Key.DirectionUp -> {
                            // Panel open & scrolled: scroll back. At top: close it.
                            if (showInfo && panelScroll.value > 0) {
                                scope.launch { panelScroll.animateScrollBy(-220f) }
                            } else {
                                showInfo = false
                            }
                            true
                        }
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
        Row(modifier = Modifier.fillMaxSize()) {
            // Photo/video shrinks to the left to make room for the panel on the right.
            Box(modifier = Modifier.weight(if (showInfo) 0.64f else 1f).fillMaxHeight()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1,
                ) { page ->
                    val asset = assets[page]
                    val isCurrent = page == pagerState.currentPage
                    when {
                        asset.isVideo && isCurrent -> VideoPage(player, poster = asset, needsTexture = videoNeedsTexture)
                        asset.hasMotion && isCurrent && motionPlaying -> VideoPage(player, poster = asset, needsTexture = videoNeedsTexture)
                        else -> PhotoPage(asset)
                    }
                }

                if (buffering && current?.isVideo == true && !videoError) {
                    Spinner(modifier = Modifier.align(Alignment.Center))
                }

                // Fully qualified: the enclosing Row's RowScope.AnimatedVisibility would otherwise
                // shadow the top-level overload inside this Box.
                androidx.compose.animation.AnimatedVisibility(
                    visible = showScrub && !videoError,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter),
                ) {
                    ScrubBar(
                        positionMs = scrubPositionMs,
                        durationMs = scrubDurationMs,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 48.dp, vertical = 24.dp),
                    )
                }
            }
            if (showInfo) {
                MetadataSidePanel(
                    detail = detail,
                    asset = current,
                    position = pagerState.currentPage + 1,
                    total = assets.size,
                    isPlaying = isPlaying,
                    hdr = hdrLabel,
                    canGoToDay = current?.dateTime?.isNotBlank() == true,
                    scroll = panelScroll,
                    modifier = Modifier.weight(0.36f).fillMaxHeight(),
                )
            }
        }

        if (slideshow) {
            OverlayChip(modifier = Modifier.align(Alignment.TopEnd).padding(24.dp)) {
                Icon(Icons.Filled.Slideshow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Text(str.viewerSlideshow, color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
        }

        if (current?.hasMotion == true && !videoError) {
            OverlayChip(modifier = Modifier.align(Alignment.TopStart).padding(24.dp)) {
                Icon(Icons.Filled.MotionPhotosOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Text(
                    text = if (motionPlaying) str.viewerLive else str.viewerLiveHint,
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

        AnimatedVisibility(
            visible = showPosition && !showInfo,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            OverlayChip(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${assets.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

        AnimatedVisibility(
            visible = showHints,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            OverlayChip(modifier = Modifier.padding(bottom = 64.dp)) {
                Text(
                    text = str.viewerHints,
                    color = Color(0xFFCDD3D9),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

        if (videoError && current?.isVideo == true) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(AppColors.scrim, RoundedCornerShape(12.dp))
                    .padding(horizontal = 28.dp, vertical = 20.dp),
            ) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                Text(
                    text = str.viewerVideoError,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
}

/** Small rounded translucent chip used for viewer overlays (badges, hints, position). */
@Composable
private fun OverlayChip(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(AppColors.chip, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        content()
    }
}

/** Transient video scrub bar: elapsed · track · duration. */
@Composable
private fun ScrubBar(positionMs: Long, durationMs: Long, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .background(AppColors.chip, RoundedCornerShape(10.dp))
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Text(
            text = formatTime(positionMs),
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .background(Color(0x4DFFFFFF), RoundedCornerShape(2.dp)),
        ) {
            val fraction = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)),
            )
        }
        Text(
            text = if (durationMs > 0) formatTime(durationMs) else "--:--",
            color = Color(0xFFCDD3D9),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

private fun formatTime(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
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
    onSeek: () -> Unit,
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
            onSeek()
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
            contentDescription = com.immichtv.ui.i18n.LocalStrings.current.kindPhoto,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun VideoPage(player: ExoPlayer, poster: Asset, needsTexture: Boolean) {
    // Ordinary videos render on a SurfaceView for true HDR (HLG/PQ) passthrough; motion-photo clips
    // and rotated videos (portrait clips a SurfaceView can't rotate) render on a TextureView, which
    // applies the rotation matrix. A TextureView tone-maps HDR to SDR, so it's used only where the
    // geometry requires it.
    val useTextureView = poster.hasMotion || needsTexture
    val layout = if (useTextureView) R.layout.video_surface else R.layout.video_surface_hdr
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AsyncImage(
            model = poster.previewUrl,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
        // key() so flipping surface type (video <-> motion clip) re-inflates the PlayerView, since a
        // PlayerView's surface type is fixed at inflation and can't change afterwards.
        key(useTextureView) {
            AndroidView(
                factory = { ctx ->
                    (android.view.LayoutInflater.from(ctx)
                        .inflate(layout, null) as PlayerView).apply {
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        this.player = player
                    }
                },
                update = { it.player = player },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun MetadataSidePanel(
    detail: DetailState,
    asset: Asset?,
    position: Int,
    total: Int,
    isPlaying: Boolean,
    hdr: String?,
    canGoToDay: Boolean,
    scroll: androidx.compose.foundation.ScrollState,
    modifier: Modifier = Modifier,
) {
    val str = com.immichtv.ui.i18n.LocalStrings.current
    val kind = if (asset?.isVideo == true) str.kindVideo(isPlaying) else str.kindPhoto
    val loaded = (detail as? DetailState.Loaded)?.detail
    Column(
        modifier = modifier
            .background(AppColors.panelTranslucent)
            .verticalScroll(scroll)
            .padding(horizontal = 36.dp, vertical = 54.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Activated by OK while the panel is open (see the viewer's key handler). Not separately
        // focusable — the viewer owns D-pad focus — so it's styled as a button with an OK affordance.
        if (canGoToDay) {
            GoToDayButton(str.viewerGoToDay)
        }
        Text(
            text = loaded?.fileName?.takeIf { it.isNotBlank() } ?: kind,
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "$position / $total   ·   $kind",
            color = AppColors.textSecondary,
            style = MaterialTheme.typography.bodyMedium,
        )
        // From the decoded video format + display state — independent of the Immich detail fetch.
        LabeledRow(str.metaDynamicRange, hdr.orEmpty())
        when (detail) {
            is DetailState.Loading ->
                Text(str.metaLoading, color = AppColors.textSecondary, style = MaterialTheme.typography.bodyMedium)
            is DetailState.Error ->
                Text(str.metaUnavailable, color = AppColors.textSecondary, style = MaterialTheme.typography.bodyMedium)
            is DetailState.Loaded -> {
                LabeledRow(str.metaDate, detail.detail.dateTaken)
                LabeledRow(str.metaLocation, detail.detail.location)
                LabeledRow(str.metaResolution, detail.detail.dimensions)
                LabeledRow(str.metaCamera, detail.detail.camera)
                LabeledRow(str.metaLens, detail.detail.lens)
                LabeledRow(str.metaExposure, detail.detail.exposure)
                LabeledRow(str.metaPeople, detail.detail.people.joinToString(", "))
            }
        }
    }
}

/**
 * "Go to this day" call-to-action at the top of the metadata panel. It isn't independently
 * focusable (the viewer owns D-pad focus for paging), so it's presented as a filled button with an
 * OK badge; pressing OK while the panel is open triggers the jump (handled in the viewer key logic).
 */
@Composable
private fun GoToDayButton(label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Icon(
            Icons.Filled.CalendarMonth,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "OK",
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .background(Color(0x33FFFFFF), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun LabeledRow(label: String, value: String) {
    if (value.isBlank()) return
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label.uppercase(),
            color = AppColors.textTertiary,
            style = MaterialTheme.typography.labelMedium,
        )
        Text(text = value, color = Color.White, style = MaterialTheme.typography.bodyLarge)
    }
}
