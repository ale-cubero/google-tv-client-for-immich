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
package com.immichtv.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

/**
 * Hand-written DTOs for the ~handful of Immich endpoints we use (deliberately NOT codegen'd
 * from the OpenAPI spec). Only fields we consume are declared; unknown JSON keys are ignored
 * by the Json config in NetworkModule.
 */

@Serializable
data class PingDto(
    val res: String = "",
)

@Serializable
data class ValidateTokenDto(
    val authStatus: Boolean = false,
)

@Serializable
data class AlbumDto(
    val id: String,
    val albumName: String = "",
    val assetCount: Int = 0,
    val albumThumbnailAssetId: String? = null,
)

@Serializable
data class MemoryDto(
    val id: String,
    val type: String = "",
    val memoryAt: String = "",
    val data: MemoryDataDto = MemoryDataDto(),
    val assets: List<AssetDto> = emptyList(),
)

@Serializable
data class MemoryDataDto(val year: Int = 0)

@Serializable
data class PeopleResponseDto(
    val people: List<PersonDto> = emptyList(),
    val total: Int = 0,
)

@Serializable
data class PersonDto(
    val id: String,
    val name: String = "",
    val thumbnailPath: String = "",
)

@Serializable
data class SearchMetadataRequest(
    val type: String? = null,
    val personIds: List<String>? = null,
    // "timeline" = only normally-visible assets; excludes hidden Motion Photo video clips
    // (Samsung/Apple/Google live photos), which have no thumbnails of their own.
    val visibility: String? = null,
    val size: Int = 100,
    val page: Int = 1,
)

/**
 * Immich smart (CLIP) search — semantic "context" search + object/scene recognition. Same response
 * shape as metadata search. `visibility="timeline"` excludes hidden Motion Photo video clips.
 */
@Serializable
data class SmartSearchRequest(
    val query: String,
    val visibility: String? = "timeline",
    val size: Int = 100,
    val page: Int = 1,
)

@Serializable
data class SearchResponseDto(val assets: SearchAssetsDto = SearchAssetsDto())

@Serializable
data class SearchAssetsDto(val items: List<AssetDto> = emptyList())

@Serializable
data class AssetDetailDto(
    val id: String,
    val type: String = "IMAGE",
    val originalFileName: String = "",
    val fileCreatedAt: String = "",
    // NOTE: `duration` is intentionally omitted — Immich returns it as null for photos but as a
    // number for videos, so a typed field mismatches and fails parsing (it's ignored via
    // ignoreUnknownKeys). It's unused by the metadata panel anyway.
    val exifInfo: ExifDto? = null,
    val people: List<PersonRefDto> = emptyList(),
)

@Serializable
data class ExifDto(
    val make: String? = null,
    val model: String? = null,
    val lensModel: String? = null,
    val fNumber: Double? = null,
    val focalLength: Double? = null,
    val iso: Int? = null,
    val exposureTime: String? = null,
    val exifImageWidth: Int? = null,
    val exifImageHeight: Int? = null,
    val fileSizeInByte: Long? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
    val dateTimeOriginal: String? = null,
)

@Serializable
data class PersonRefDto(val id: String, val name: String = "")

@Serializable
data class AlbumDetailDto(
    val id: String,
    val albumName: String = "",
    val assets: List<AssetDto> = emptyList(),
)

@Serializable
data class AssetDto(
    val id: String,
    val type: String = "IMAGE", // "IMAGE" | "VIDEO"
    @SerialName("originalFileName") val originalFileName: String = "",
    val livePhotoVideoId: String? = null, // set on motion-photo stills → the hidden motion clip
    // Immich formats duration inconsistently ("H:MM:SS.mmm" string, number of seconds, or null) —
    // take the raw primitive and normalize in the repository. (A typed String? failed parsing before.)
    val duration: JsonPrimitive? = null,
)

/** One entry from `GET /api/timeline/buckets` — a month and its asset count. */
@Serializable
data class TimeBucketDto(
    val timeBucket: String,
    val count: Int = 0,
)

/**
 * `GET /api/timeline/bucket` in Immich 3.x returns a COLUMNAR payload: each field is an array,
 * indexed in parallel (id[i] ↔ isImage[i] ↔ …). Only the columns we use are declared; the rest
 * (visibility, city, country, …) are ignored. This SoA layout is what keeps the timeline light.
 */
@Serializable
data class TimeBucketAssetsDto(
    val id: List<String> = emptyList(),
    val isImage: List<Boolean> = emptyList(),
    val ratio: List<Double> = emptyList(),
    val thumbhash: List<String?> = emptyList(),
    val livePhotoVideoId: List<String?> = emptyList(),
    val duration: List<JsonPrimitive?> = emptyList(), // per-asset video duration (see AssetDto.duration)
)
