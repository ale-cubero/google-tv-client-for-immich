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

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for Immich. Paths are relative; the real host is swapped in at request
 * time by [HostSelectionInterceptor], and the `x-api-key` header is added by [AuthInterceptor].
 */
interface ImmichApi {

    @GET("api/server/ping")
    suspend fun ping(): PingDto

    @POST("api/auth/validateToken")
    suspend fun validateToken(): ValidateTokenDto

    @GET("api/albums")
    suspend fun getAlbums(): List<AlbumDto>

    @GET("api/albums/{id}")
    suspend fun getAlbum(@Path("id") id: String): AlbumDetailDto

    // Immich 3.x: album/timeline assets come from the bucket API, not the album detail.
    @GET("api/timeline/buckets")
    suspend fun getBuckets(@Query("albumId") albumId: String? = null): List<TimeBucketDto>

    @GET("api/timeline/bucket")
    suspend fun getBucket(
        @Query("timeBucket") timeBucket: String,
        @Query("albumId") albumId: String? = null,
    ): TimeBucketAssetsDto

    @GET("api/assets/{id}")
    suspend fun getAssetDetail(@Path("id") id: String): AssetDetailDto

    @GET("api/memories")
    suspend fun getMemories(): List<MemoryDto>

    @GET("api/people")
    suspend fun getPeople(@Query("size") size: Int = 200): PeopleResponseDto

    @POST("api/search/metadata")
    suspend fun searchMetadata(@Body body: SearchMetadataRequest): SearchResponseDto

    // Smart/CLIP search — semantic context + object recognition. Non-mutating read (allowlisted).
    @POST("api/search/smart")
    suspend fun searchSmart(@Body body: SmartSearchRequest): SearchResponseDto

    // Grouped explore data (Places = the exifInfo.city group).
    @GET("api/search/explore")
    suspend fun getExplore(): List<ExploreGroupDto>
}
