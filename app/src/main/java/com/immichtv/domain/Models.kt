package com.immichtv.domain

/** Server connection settings, persisted in DataStore. */
data class ServerSettings(
    val baseUrl: String = "",
    val apiKey: String = "",
    // Opt-in (off by default): trust self-signed/invalid TLS certificates and allow http://.
    // For self-hosted Immich that often lacks a valid certificate or DNS record.
    val allowInsecure: Boolean = false,
) {
    val isConfigured: Boolean get() = baseUrl.isNotBlank() && apiKey.isNotBlank()
}

/** An album as shown in a list (no assets loaded). */
data class Album(
    val id: String,
    val name: String,
    val assetCount: Int,
    val thumbnailUrl: String,
)

/** A person (face) for the People rail. */
data class Person(
    val id: String,
    val name: String,
    val thumbnailUrl: String,
)

/** An "on this day" memory: a titled set of assets. */
data class Memory(
    val id: String,
    val title: String,
    val assets: List<Asset>,
)

/** Rich detail for the viewer's metadata panel. Blank fields are simply omitted from the UI. */
data class AssetDetail(
    val fileName: String,
    val dateTaken: String,
    val camera: String,
    val lens: String,
    val exposure: String,
    val dimensions: String,
    val location: String,
    val people: List<String>,
)

/** The contents of one album. */
data class AlbumContent(
    val id: String,
    val name: String,
    val assets: List<Asset>,
)

/** A single photo or video, with its display URLs already resolved. */
data class Asset(
    val id: String,
    val isVideo: Boolean,
    val fileName: String,
    val thumbnailUrl: String,    // size=thumbnail — grid cell + viewer placeholder
    val previewUrl: String,      // size=preview — full-screen photo
    val videoUrl: String,        // /video/playback — empty for images
    val motionVideoUrl: String = "", // motion-photo clip playback URL — empty unless a live/motion photo
) {
    /** A still photo that carries an embedded motion clip (Samsung/Apple/Google live photo). */
    val hasMotion: Boolean get() = !isVideo && motionVideoUrl.isNotEmpty()
}

/**
 * Metadata for one month bucket in the timeline. Loaded up front for the whole library so the
 * grid's total extent and month boundaries are known before any asset is fetched.
 *
 * @param flatIndex the LazyGrid item index of this bucket's header row (header + [count] cells
 *   per bucket), used by the scrubber to jump directly to a month.
 */
data class BucketMeta(
    val timeBucket: String, // "YYYY-MM-01"
    val count: Int,
    val flatIndex: Int,
    val year: Int,
    val label: String, // e.g. "July 2026"
)
