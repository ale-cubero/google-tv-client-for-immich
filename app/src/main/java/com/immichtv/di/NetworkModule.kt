package com.immichtv.di

import com.immichtv.BuildConfig
import com.immichtv.data.prefs.SettingsRepository
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
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import javax.net.ssl.HostnameVerifier
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
            .applyInsecureTrust(settings)  // opt-in TLS bypass for self-hosted servers without valid certs
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Installs a TLS trust manager + hostname verifier that consult the *live* `allowInsecure`
     * setting on each connection. When the user has NOT opted in, both delegate to the platform
     * default (normal certificate + hostname validation). When they have (self-hosted Immich with a
     * self-signed cert or no DNS record), certificate and hostname checks are skipped.
     *
     * Reading `settings.snapshot` at connection time means the single shared client honours a change
     * to the toggle without being rebuilt. Off by default, so the secure path is unchanged.
     */
    private fun OkHttpClient.Builder.applyInsecureTrust(settings: SettingsRepository): OkHttpClient.Builder {
        val platformTrust = run {
            val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            tmf.init(null as java.security.KeyStore?)
            tmf.trustManagers.filterIsInstance<X509TrustManager>().first()
        }
        val flexibleTrust = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) =
                platformTrust.checkClientTrusted(chain, authType)

            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                if (settings.snapshot.allowInsecure) return // trust any server cert when opted in
                platformTrust.checkServerTrusted(chain, authType)
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> = platformTrust.acceptedIssuers
        }
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<javax.net.ssl.TrustManager>(flexibleTrust), SecureRandom())
        }
        val defaultHostnameVerifier = javax.net.ssl.HttpsURLConnection.getDefaultHostnameVerifier()
        return sslSocketFactory(sslContext.socketFactory, flexibleTrust)
            .hostnameVerifier(HostnameVerifier { hostname, session ->
                settings.snapshot.allowInsecure || defaultHostnameVerifier.verify(hostname, session)
            })
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
