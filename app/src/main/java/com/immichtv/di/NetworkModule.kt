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
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.net.Socket
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509ExtendedTrustManager
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
     * self-signed cert or no DNS record), certificate and hostname checks are skipped — but ONLY
     * for the configured server's host: a redirect or any other request to a different host still
     * gets full platform validation, so the opt-in cannot be leveraged to MITM third-party hosts.
     *
     * Host scoping uses the [X509ExtendedTrustManager] socket/engine callbacks (which carry the
     * peer host — Android's Conscrypt always invokes these). The host-less legacy callback keeps
     * the unscoped behaviour as a fallback for stacks that provide no peer information.
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
        val platformExtended = platformTrust as? X509ExtendedTrustManager

        // Bypass only when opted in AND the connection is to the configured server's host.
        fun bypass(peerHost: String?): Boolean {
            if (!settings.snapshot.allowInsecure) return false
            val configured = settings.snapshot.baseUrl.toHttpUrlOrNull()?.host ?: return false
            return peerHost != null && peerHost.equals(configured, ignoreCase = true)
        }

        val flexibleTrust = object : X509ExtendedTrustManager() {
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String, socket: Socket) {
                val peerHost = (socket as? SSLSocket)?.handshakeSession?.peerHost
                if (bypass(peerHost)) return
                platformExtended?.checkServerTrusted(chain, authType, socket)
                    ?: platformTrust.checkServerTrusted(chain, authType)
            }

            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String, engine: SSLEngine) {
                if (bypass(engine.peerHost)) return
                platformExtended?.checkServerTrusted(chain, authType, engine)
                    ?: platformTrust.checkServerTrusted(chain, authType)
            }

            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                // No peer information available on this legacy path (not used by Conscrypt) —
                // fall back to the unscoped toggle so insecure mode keeps working if it's ever hit.
                if (settings.snapshot.allowInsecure) return
                platformTrust.checkServerTrusted(chain, authType)
            }

            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String, socket: Socket) {
                platformExtended?.checkClientTrusted(chain, authType, socket)
                    ?: platformTrust.checkClientTrusted(chain, authType)
            }

            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String, engine: SSLEngine) {
                platformExtended?.checkClientTrusted(chain, authType, engine)
                    ?: platformTrust.checkClientTrusted(chain, authType)
            }

            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) =
                platformTrust.checkClientTrusted(chain, authType)

            override fun getAcceptedIssuers(): Array<X509Certificate> = platformTrust.acceptedIssuers
        }
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<javax.net.ssl.TrustManager>(flexibleTrust), SecureRandom())
        }
        val defaultHostnameVerifier = javax.net.ssl.HttpsURLConnection.getDefaultHostnameVerifier()
        return sslSocketFactory(sslContext.socketFactory, flexibleTrust)
            .hostnameVerifier(HostnameVerifier { hostname, session ->
                bypass(hostname) || defaultHostnameVerifier.verify(hostname, session)
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
