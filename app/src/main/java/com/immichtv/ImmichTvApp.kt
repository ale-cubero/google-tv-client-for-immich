package com.immichtv

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import javax.inject.Inject

/**
 * Application entry point.
 *
 * Also serves as Coil's [SingletonImageLoader.Factory] so that every [coil3.compose.AsyncImage]
 * in the app is backed by the SAME [OkHttpClient] Retrofit uses — this is what injects the
 * `x-api-key` header into thumbnail/preview requests (one shared OkHttp client).
 */
@HiltAndroidApp
class ImmichTvApp : Application(), SingletonImageLoader.Factory {

    @Inject
    lateinit var okHttpClient: OkHttpClient

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient }))
            }
            .crossfade(true)
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(512L * 1024 * 1024) // 512 MB — generous
                    .build()
            }
            .build()
}
