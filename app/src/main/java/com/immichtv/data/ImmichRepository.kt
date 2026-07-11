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
package com.immichtv.data

import com.immichtv.data.prefs.SettingsRepository
import com.immichtv.data.remote.AssetDto
import com.immichtv.data.remote.ExifDto
import com.immichtv.data.remote.ImmichApi
import com.immichtv.data.remote.SearchMetadataRequest
import com.immichtv.data.remote.SmartSearchRequest
import com.immichtv.data.remote.TimeBucketAssetsDto
import com.immichtv.domain.Album
import com.immichtv.domain.AlbumContent
import com.immichtv.domain.Asset
import com.immichtv.domain.AssetDetail
import com.immichtv.domain.BucketMeta
import com.immichtv.domain.Memory
import com.immichtv.domain.Person
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.time.OffsetDateTime
import java.time.Year
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private val MONTH_NAMES = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)

/**
 * Repository layer: returns domain models, never lets DTOs leak upward.
 */
@Singleton
class ImmichRepository @Inject constructor(
    private val api: ImmichApi,
    private val settings: SettingsRepository,
) {
    /** True if ping + token validation both succeed against the currently-staged settings. */
    suspend fun validateConnection(): Result<Unit> = runCatching {
        api.ping()
        val token = api.validateToken()
        check(token.authStatus) { "API key rejected by server" }
    }

    suspend fun getAlbums(): List<Album> =
        api.getAlbums().map {
            Album(
                id = it.id,
                name = it.albumName,
                assetCount = it.assetCount,
                thumbnailUrl = it.albumThumbnailAssetId?.let(::thumbnailUrl).orEmpty(),
            )
        }

    /** "On this day" memories, newest first, titled by how long ago. */
    suspend fun getMemories(): List<Memory> {
        val thisYear = Year.now().value
        return api.getMemories()
            .filter { it.assets.isNotEmpty() }
            .map { dto ->
                val yearsAgo = (thisYear - dto.data.year).coerceAtLeast(0)
                Memory(
                    id = dto.id,
                    title = when (yearsAgo) {
                        0 -> "This year"
                        1 -> "1 year ago"
                        else -> "$yearsAgo years ago"
                    },
                    assets = dto.assets.map { it.toDomain() },
                )
            }
    }

    /** Named people for the People rail (with face thumbnails). */
    suspend fun getPeople(): List<Person> =
        api.getPeople().people
            .filter { it.name.isNotBlank() }
            .map { Person(id = it.id, name = it.name, thumbnailUrl = personThumbnailUrl(it.id)) }

    /** Newest month's assets, for the Recent rail. */
    suspend fun getRecentAssets(): List<Asset> {
        val newest = api.getBuckets(albumId = null).firstOrNull() ?: return emptyList()
        return getBucketAssets(newest.timeBucket)
    }

    /** Most recent real videos for the Videos rail (excludes hidden Motion Photo clips). */
    suspend fun getVideos(limit: Int = 40): List<Asset> =
        api.searchMetadata(
            SearchMetadataRequest(type = "VIDEO", visibility = "timeline", size = limit),
        ).assets.items.map { it.toDomain() }

    /** All of one person's normally-visible assets (excludes hidden Motion Photo clips). */
    suspend fun getPersonAssets(personId: String): List<Asset> =
        api.searchMetadata(
            SearchMetadataRequest(personIds = listOf(personId), visibility = "timeline", size = 1000),
        ).assets.items.map { it.toDomain() }

    /** Immich smart (CLIP) search: semantic "context" search + object/scene recognition. */
    suspend fun searchSmart(query: String, limit: Int = 100): List<Asset> =
        api.searchSmart(SmartSearchRequest(query = query.trim(), size = limit))
            .assets.items.map { it.toDomain() }

    /** Full EXIF + people detail for the viewer's metadata panel. */
    suspend fun getAssetDetail(assetId: String): AssetDetail {
        val dto = api.getAssetDetail(assetId)
        val exif = dto.exifInfo
        return AssetDetail(
            fileName = dto.originalFileName,
            dateTaken = formatDate(exif?.dateTimeOriginal ?: dto.fileCreatedAt),
            camera = listOfNotNull(exif?.make, exif?.model).joinToString(" ").trim(),
            lens = exif?.lensModel?.trim().orEmpty(),
            exposure = formatExposure(exif),
            dimensions = formatDimensions(exif),
            location = listOfNotNull(exif?.city, exif?.state, exif?.country)
                .filter { it.isNotBlank() }.joinToString(", "),
            people = dto.people.map { it.name.trim() }.filter { it.isNotBlank() },
        )
    }

    private fun formatDate(iso: String): String = runCatching {
        OffsetDateTime.parse(iso).format(
            DateTimeFormatter.ofPattern("MMM d, yyyy · HH:mm", Locale.getDefault()),
        )
    }.getOrDefault("")

    private fun formatExposure(exif: ExifDto?): String {
        if (exif == null) return ""
        return listOfNotNull(
            exif.fNumber?.let { "ƒ/$it" },
            exif.exposureTime?.let { "$it s" },
            exif.iso?.let { "ISO $it" },
            exif.focalLength?.let { "${it}mm" },
        ).joinToString("  ·  ")
    }

    private fun formatDimensions(exif: ExifDto?): String {
        if (exif == null) return ""
        val wh = if (exif.exifImageWidth != null && exif.exifImageHeight != null) {
            "${exif.exifImageWidth} × ${exif.exifImageHeight}"
        } else {
            ""
        }
        val mb = exif.fileSizeInByte?.let { "%.1f MB".format(it / 1_048_576.0) } ?: ""
        return listOf(wh, mb).filter { it.isNotBlank() }.joinToString("  ·  ")
    }

    suspend fun getAlbumContent(albumId: String): AlbumContent {
        // Album detail carries the name; its assets live in the timeline bucket API (Immich 3.x).
        val meta = api.getAlbum(albumId)
        val buckets = api.getBuckets(albumId = albumId)
        val assets = buckets.flatMap { bucket ->
            api.getBucket(timeBucket = bucket.timeBucket, albumId = albumId).toAssets()
        }
        return AlbumContent(id = meta.id, name = meta.albumName, assets = assets)
    }

    /**
     * All month buckets for the whole library (newest first), with flat grid indices precomputed.
     * A few KB even for 200k assets — this defines the timeline's scroll extent up front.
     */
    suspend fun getTimelineBuckets(): List<BucketMeta> {
        var flatIndex = 0
        return api.getBuckets(albumId = null).map { dto ->
            val year = dto.timeBucket.take(4).toIntOrNull() ?: 0
            val monthIdx = dto.timeBucket.drop(5).take(2).toIntOrNull() ?: 1
            val meta = BucketMeta(
                timeBucket = dto.timeBucket,
                count = dto.count,
                flatIndex = flatIndex,
                year = year,
                label = "${MONTH_NAMES.getOrElse(monthIdx - 1) { "" }} $year",
            )
            flatIndex += 1 + dto.count // 1 header row + one item per asset
            meta
        }
    }

    /** Lazily fetch one month's assets (columnar bucket → domain assets). */
    suspend fun getBucketAssets(timeBucket: String): List<Asset> =
        api.getBucket(timeBucket = timeBucket, albumId = null).toAssets()

    private fun TimeBucketAssetsDto.toAssets(): List<Asset> =
        id.mapIndexed { i, assetId ->
            val video = !isImage.getOrElse(i) { true }
            val motionClipId = livePhotoVideoId.getOrNull(i)
            Asset(
                id = assetId,
                isVideo = video,
                fileName = "",
                thumbnailUrl = thumbnailUrl(assetId),
                previewUrl = previewUrl(assetId),
                videoUrl = if (video) videoPlaybackUrl(assetId) else "",
                motionVideoUrl = if (!video && motionClipId != null) videoPlaybackUrl(motionClipId) else "",
                duration = if (video) formatDuration(duration.getOrNull(i)) else "",
            )
        }

    private fun AssetDto.toDomain(): Asset {
        val video = type.equals("VIDEO", ignoreCase = true)
        return Asset(
            id = id,
            isVideo = video,
            fileName = originalFileName,
            thumbnailUrl = thumbnailUrl(id),
            previewUrl = previewUrl(id),
            videoUrl = if (video) videoPlaybackUrl(id) else "",
            motionVideoUrl = if (!video && livePhotoVideoId != null) videoPlaybackUrl(livePhotoVideoId) else "",
            duration = if (video) formatDuration(duration) else "",
        )
    }

    private fun personThumbnailUrl(personId: String): String =
        assetUrl("api/people/$personId/thumbnail") { this }

    /** `{base}/api/assets/{id}/thumbnail?size=thumbnail` — small WebP grid cell. */
    private fun thumbnailUrl(assetId: String): String =
        assetUrl("api/assets/$assetId/thumbnail") { addQueryParameter("size", "thumbnail") }

    /** `{base}/api/assets/{id}/thumbnail?size=preview` — ~1440p JPEG for the full-screen viewer. */
    private fun previewUrl(assetId: String): String =
        assetUrl("api/assets/$assetId/thumbnail") { addQueryParameter("size", "preview") }

    /** `{base}/api/assets/{id}/video/playback` — ExoPlayer media source (server picks direct/transcode). */
    private fun videoPlaybackUrl(assetId: String): String =
        assetUrl("api/assets/$assetId/video/playback") { this }

    private inline fun assetUrl(
        path: String,
        query: okhttp3.HttpUrl.Builder.() -> okhttp3.HttpUrl.Builder,
    ): String {
        val base = settings.snapshot.baseUrl.toHttpUrlOrNull() ?: return ""
        return base.newBuilder().addPathSegments(path).query().build().toString()
    }
}

/**
 * Normalize Immich's inconsistent `duration` — a "H:MM:SS.mmm" string in some payloads, a number
 * of MILLISECONDS in others (verified against a live v3 server), or null — into a compact badge
 * label: "2:34" or "1:02:34". Empty when unknown/zero.
 */
internal fun formatDuration(raw: kotlinx.serialization.json.JsonPrimitive?): String {
    raw ?: return ""
    val totalSeconds: Long = if (raw.isString) {
        val parts = raw.content.substringBefore('.').split(':')
        if (parts.size != 3) return ""
        val (h, m, s) = parts.map { it.toLongOrNull() ?: return "" }
        h * 3600 + m * 60 + s
    } else {
        val ms = raw.content.toDoubleOrNull() ?: return ""
        (ms / 1000).toLong()
    }
    if (totalSeconds <= 0) return ""
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
