package com.immichtv.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    val duration: String = "",
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
)
