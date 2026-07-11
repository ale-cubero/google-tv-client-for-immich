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
}
