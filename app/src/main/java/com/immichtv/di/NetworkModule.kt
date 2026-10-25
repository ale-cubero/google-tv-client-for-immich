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
package com.immichtv.di

import com.immichtv.BuildConfig
import com.immichtv.data.prefs.SettingsRepository
import com.immichtv.data.remote.AuthInterceptor
import com.immichtv.data.remote.CertPins
import com.immichtv.data.remote.HostSelectionInterceptor
import com.immichtv.data.remote.ImmichApi
import com.immichtv.data.remote.PinningHostnameVerifier
import com.immichtv.data.remote.PinningTrustManager
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
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

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
        settings: SettingsRepository,
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
            .applyCertificatePinning(settings) // user-trusted cert for self-hosted servers; never a blanket bypass
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Installs certificate pinning for self-hosted servers the platform doesn't trust. Normal
     * certificate + hostname validation always applies; the only addition is that the configured
     * server may present the one certificate the user explicitly trusted during setup (matched by
     * SHA-256 fingerprint, scoped to that host). Any other certificate — or the pinned one on a
     * different host — is rejected by the platform as usual. See [PinningTrustManager].
     *
     * The pin is read from the live settings snapshot on each handshake, so the single shared
     * client follows setup changes without being rebuilt.
     */
    private fun OkHttpClient.Builder.applyCertificatePinning(settings: SettingsRepository): OkHttpClient.Builder {
        val platformTrust = run {
            val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            tmf.init(null as java.security.KeyStore?)
            tmf.trustManagers.filterIsInstance<X509TrustManager>().first()
        }
        val currentPin = { settings.snapshot.let { CertPins.pinFor(it.baseUrl, it.pinnedCertSha256) } }
        val trustManager = PinningTrustManager(platformTrust, currentPin)
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<javax.net.ssl.TrustManager>(trustManager), SecureRandom())
        }
        val hostnameVerifier = PinningHostnameVerifier(
            javax.net.ssl.HttpsURLConnection.getDefaultHostnameVerifier(),
            currentPin,
        )
        return sslSocketFactory(sslContext.socketFactory, trustManager).hostnameVerifier(hostnameVerifier)
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
