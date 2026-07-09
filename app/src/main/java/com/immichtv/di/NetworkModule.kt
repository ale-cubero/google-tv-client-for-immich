package com.immichtv.di

import com.immichtv.BuildConfig
import com.immichtv.data.remote.AuthInterceptor
import com.immichtv.data.remote.HostSelectionInterceptor
import com.immichtv.data.remote.ImmichApi
import com.immichtv.data.remote.ReadOnlyInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false // omit null request fields (e.g. search filters) instead of sending null
    }

    /**
     * The ONE OkHttp client shared by Retrofit, Coil, and (later) the ExoPlayer data source
     * factory, so all three carry the `x-api-key` header.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        readOnly: ReadOnlyInterceptor,
        hostSelection: HostSelectionInterceptor,
        auth: AuthInterceptor,
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
            redactHeader("x-api-key") // never log the credential even if the level is raised
        }
        return OkHttpClient.Builder()
            .addInterceptor(readOnly)      // veto any non-read request before anything else (read-only app)
            .addInterceptor(hostSelection) // application: rewrite placeholder host to the real server
            .addNetworkInterceptor(auth)   // network: attach the key only to the configured origin, per hop
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            // Placeholder host; HostSelectionInterceptor swaps in the real one at request time.
            .baseUrl("http://localhost/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideImmichApi(retrofit: Retrofit): ImmichApi = retrofit.create(ImmichApi::class.java)
}
