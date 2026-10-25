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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.InetSocketAddress
import java.net.Socket
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine
import javax.net.ssl.SSLSession
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509ExtendedTrustManager
import javax.net.ssl.X509TrustManager

/**
 * Certificate pinning for self-hosted servers whose certificate the platform doesn't trust
 * (self-signed, private CA, or a name/IP the certificate doesn't cover).
 *
 * Instead of switching certificate checks off, the user is shown the server's certificate once and
 * may trust that exact certificate (by its SHA-256 fingerprint) for that one host. From then on a
 * connection to the configured host is accepted only if it presents that very certificate; anything
 * else goes through normal platform validation, whose [CertificateException] propagates unchanged.
 */
object CertPins {
    /** Lowercase hex SHA-256 of the certificate's DER encoding — the browser-style fingerprint. */
    fun sha256(cert: X509Certificate): String =
        MessageDigest.getInstance("SHA-256").digest(cert.encoded).joinToString("") { "%02x".format(it) }

    /** "AB:CD:…" — how browsers and `openssl x509 -fingerprint` print it, for comparison by eye. */
    fun display(fingerprint: String): String = fingerprint.uppercase().chunked(2).joinToString(":")

    /** True only when [host] is the configured host and [leaf] is exactly the pinned certificate. */
    fun matches(pin: Pin?, host: String?, leaf: X509Certificate?): Boolean {
        if (pin == null || host == null || leaf == null) return false
        if (pin.sha256.isBlank() || !host.equals(pin.host, ignoreCase = true)) return false
        return MessageDigest.isEqual(sha256(leaf).toByteArray(), pin.sha256.lowercase().toByteArray())
    }

    /** The pin that applies right now: the trusted fingerprint, scoped to the configured server host. */
    data class Pin(val host: String, val sha256: String)

    fun pinFor(baseUrl: String, sha256: String): Pin? {
        if (sha256.isBlank()) return null
        val host = baseUrl.toHttpUrlOrNull()?.host ?: return null
        return Pin(host, sha256)
    }
}

/**
 * Accepts the pinned certificate for the configured host; everything else is delegated to the
 * platform trust manager, which throws for any certificate it doesn't trust.
 */
class PinningTrustManager(
    private val platform: X509TrustManager,
    private val currentPin: () -> CertPins.Pin?,
) : X509ExtendedTrustManager() {

    private val platformExtended = platform as? X509ExtendedTrustManager

    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String, socket: Socket) {
        val host = (socket as? SSLSocket)?.handshakeSession?.peerHost
        if (CertPins.matches(currentPin(), host, chain.firstOrNull())) return
        platformExtended?.checkServerTrusted(chain, authType, socket)
            ?: platform.checkServerTrusted(chain, authType)
    }

    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String, engine: SSLEngine) {
        if (CertPins.matches(currentPin(), engine.peerHost, chain.firstOrNull())) return
        platformExtended?.checkServerTrusted(chain, authType, engine)
            ?: platform.checkServerTrusted(chain, authType)
    }

    // No peer host available here, so a pin can't be scoped: always use full platform validation.
    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) =
        platform.checkServerTrusted(chain, authType)

    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String, socket: Socket) {
        platformExtended?.checkClientTrusted(chain, authType, socket)
            ?: platform.checkClientTrusted(chain, authType)
    }

    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String, engine: SSLEngine) {
        platformExtended?.checkClientTrusted(chain, authType, engine)
            ?: platform.checkClientTrusted(chain, authType)
    }

    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) =
        platform.checkClientTrusted(chain, authType)

    override fun getAcceptedIssuers(): Array<X509Certificate> = platform.acceptedIssuers
}

/**
 * Standard hostname verification, except that the pinned certificate is also accepted for the
 * configured host (self-signed certificates often don't name the host or IP they're served on).
 */
class PinningHostnameVerifier(
    private val platform: HostnameVerifier,
    private val currentPin: () -> CertPins.Pin?,
) : HostnameVerifier {
    override fun verify(hostname: String, session: SSLSession): Boolean {
        if (platform.verify(hostname, session)) return true
        val leaf = runCatching { session.peerCertificates.firstOrNull() as? X509Certificate }.getOrNull()
        return CertPins.matches(currentPin(), hostname, leaf)
    }
}

/**
 * Reads the certificate a server presents, so the user can decide whether to trust it. The probe's
 * trust manager records the chain and then always rejects it — the probe never completes a trusted
 * connection and sends nothing to the server.
 */
object CertificateProbe {
    suspend fun fetchLeaf(baseUrl: String): X509Certificate? = withContext(Dispatchers.IO) {
        val url = baseUrl.toHttpUrlOrNull()?.takeIf { it.isHttps } ?: return@withContext null
        var leaf: X509Certificate? = null
        val recorder = object : X509TrustManager {
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                leaf = chain.firstOrNull()
                throw CertificateException("Certificate probe: recorded for review, never trusted")
            }

            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) =
                throw CertificateException("Client certificates are not used")

            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }
        val context = SSLContext.getInstance("TLS").apply { init(null, arrayOf(recorder), SecureRandom()) }
        runCatching {
            Socket().use { raw ->
                raw.connect(InetSocketAddress(url.host, url.port), PROBE_TIMEOUT_MS)
                raw.soTimeout = PROBE_TIMEOUT_MS
                (context.socketFactory.createSocket(raw, url.host, url.port, true) as SSLSocket).use {
                    it.startHandshake() // fails by design once the chain is recorded
                }
            }
        }
        leaf
    }

    private const val PROBE_TIMEOUT_MS = 10_000
}
