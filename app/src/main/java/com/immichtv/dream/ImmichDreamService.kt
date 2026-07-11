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
package com.immichtv.dream

import android.graphics.Color
import android.service.dreams.DreamService
import android.widget.FrameLayout
import android.widget.ImageView
import coil3.SingletonImageLoader
import coil3.asDrawable
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.immichtv.data.ImmichRepository
import com.immichtv.data.prefs.SettingsRepository
import com.immichtv.domain.Asset
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val CROSSFADE_MS = 800L
private const val SAMPLED_BUCKETS = 8
private const val MAX_PHOTOS = 240

/**
 * System screensaver ("Daydream"): a photo slideshow sampled from across the whole library —
 * a handful of random month buckets, shuffled — reusing the same repository and shared OkHttp/Coil
 * stack as the app. Two stacked ImageViews crossfade between slides (a single view would flash
 * black between images). Plain views (not Compose) to keep the Dream window lifecycle simple.
 */
@AndroidEntryPoint
class ImmichDreamService : DreamService() {

    @Inject
    lateinit var repository: ImmichRepository

    @Inject
    lateinit var settings: SettingsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var front: ImageView
    private lateinit var back: ImageView

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true
        isScreenBright = false // dim like a screensaver

        fun makeImageView() = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = FrameLayout.LayoutParams(MATCH, MATCH)
        }
        back = makeImageView()
        front = makeImageView()
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            addView(back)
            addView(front)
        }
        setContentView(root)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        scope.launch {
            // Honor the app's slideshow interval (but never below 4s for a screensaver).
            val intervalMs = runCatching { settings.slideshowPrefs.first().intervalSeconds }
                .getOrDefault(8)
                .coerceAtLeast(4) * 1_000L
            val photos = loadPhotos()
            if (photos.isEmpty()) return@launch
            var index = 0
            while (isActive) {
                show(photos[index % photos.size])
                index++
                delay(intervalMs)
            }
        }
    }

    /** Sample a few random month buckets across the whole library, then shuffle the photos. */
    private suspend fun loadPhotos(): List<Asset> {
        val buckets = runCatching { repository.getTimelineBuckets() }.getOrDefault(emptyList())
        if (buckets.isEmpty()) return emptyList()
        val sampled = buckets.shuffled().take(SAMPLED_BUCKETS)
        val photos = sampled.flatMap { bucket ->
            runCatching { repository.getBucketAssets(bucket.timeBucket) }.getOrDefault(emptyList())
        }.filter { !it.isVideo }
        return photos.shuffled().take(MAX_PHOTOS)
    }

    /** Crossfade the next image in over the current one. */
    private suspend fun show(asset: Asset) {
        val loader = SingletonImageLoader.get(this)
        val request = ImageRequest.Builder(this).data(asset.previewUrl).build()
        val result = loader.execute(request)
        val image = (result as? SuccessResult)?.image ?: return
        // Load into the back view, fade it in over the front, then swap roles.
        back.setImageDrawable(image.asDrawable(resources))
        back.bringToFront()
        back.alpha = 0f
        back.animate().alpha(1f).setDuration(CROSSFADE_MS).start()
        val previousFront = front
        front = back
        back = previousFront
    }

    override fun onDreamingStopped() {
        super.onDreamingStopped()
        scope.coroutineContext[kotlinx.coroutines.Job]?.let {
            it.children.forEach { child -> child.cancel() }
        }
    }

    override fun onDetachedFromWindow() {
        scope.cancel()
        super.onDetachedFromWindow()
    }

    private companion object {
        const val MATCH = FrameLayout.LayoutParams.MATCH_PARENT
    }
}
