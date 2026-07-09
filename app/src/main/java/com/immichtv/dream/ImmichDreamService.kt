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
import com.immichtv.domain.Asset
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val SLIDE_MS = 8_000L

/**
 * System screensaver ("Daydream"): a slideshow of recent photos, reusing the same repository and
 * shared OkHttp/Coil stack as the app. Plain ImageView + Coil (not Compose) to keep the Dream
 * window lifecycle simple.
 */
@AndroidEntryPoint
class ImmichDreamService : DreamService() {

    @Inject
    lateinit var repository: ImmichRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var imageView: ImageView

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true
        isScreenBright = false // dim like a screensaver

        imageView = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = FrameLayout.LayoutParams(MATCH, MATCH)
        }
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            addView(imageView)
        }
        setContentView(root)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        scope.launch {
            val photos = loadPhotos()
            if (photos.isEmpty()) return@launch
            var index = 0
            while (isActive) {
                show(photos[index % photos.size])
                index++
                delay(SLIDE_MS)
            }
        }
    }

    private suspend fun loadPhotos(): List<Asset> {
        val recent = runCatching { repository.getRecentAssets() }.getOrDefault(emptyList())
        return recent.filter { !it.isVideo }
    }

    private suspend fun show(asset: Asset) {
        val loader = SingletonImageLoader.get(this)
        val request = ImageRequest.Builder(this).data(asset.previewUrl).build()
        val result = loader.execute(request)
        val image = (result as? SuccessResult)?.image ?: return
        imageView.setImageDrawable(image.asDrawable(resources))
        imageView.alpha = 0f
        imageView.animate().alpha(1f).setDuration(600).start()
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
