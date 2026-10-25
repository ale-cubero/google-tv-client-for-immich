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
package com.immichtv.domain

/** Server connection settings, persisted in DataStore. */
data class ServerSettings(
    val baseUrl: String = "",
    val apiKey: String = "",
    // Opt-in (off by default): allow plain http:// for servers without TLS, e.g. on a home network.
    val allowHttp: Boolean = false,
    // SHA-256 fingerprint of a server certificate the user explicitly trusted during setup (a
    // self-signed or private-CA certificate). Empty = normal platform validation only.
    val pinnedCertSha256: String = "",
) {
    val isConfigured: Boolean get() = baseUrl.isNotBlank() && apiKey.isNotBlank()
}

/** Slideshow preferences (viewer slideshow + screensaver), persisted in DataStore. */
data class SlideshowPrefs(
    val intervalSeconds: Int = 6,
    val shuffle: Boolean = false,
)

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

/** A geographic place (city) for the Places rail, with a representative thumbnail. */
data class Place(
    val city: String,
    val thumbnailUrl: String,
)

/** An "on this day" memory: a titled set of assets. */
data class Memory(
    val id: String,
    val title: String,
    val assets: List<Asset>,
    val subtitle: String = "", // "Oct 24, 2025 · 14 items" — tells same-year memories apart
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
    val duration: String = "",   // formatted video duration ("2:34", "1:02:34"); empty for images/unknown
    val dateTime: String = "",   // ISO local capture time ("YYYY-MM-DD…"); empty if unknown. Drives "go to this day".
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
