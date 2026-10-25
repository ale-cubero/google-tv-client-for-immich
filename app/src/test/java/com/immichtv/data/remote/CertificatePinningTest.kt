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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * Pinning must accept exactly one thing — the user-trusted certificate on the configured host —
 * and leave every other case to normal platform validation (which rejects self-signed certs).
 * Uses two real self-signed certificates (test resources), not mocks.
 */
class CertificatePinningTest {

    private val pinned = load("pinned.pem")
    private val other = load("other.pem")
    private val host = "immich.lan"
    private val pin = CertPins.Pin(host, CertPins.sha256(pinned))

    private val platform: X509TrustManager = TrustManagerFactory
        .getInstance(TrustManagerFactory.getDefaultAlgorithm())
        .apply { init(null as java.security.KeyStore?) }
        .trustManagers.filterIsInstance<X509TrustManager>().first()

    @Test
    fun fingerprint_matchesOpenssl() {
        // `openssl x509 -in pinned.pem -noout -fingerprint -sha256`
        assertEquals(
            "16:1D:C1:E5:15:07:9D:FA:C9:CB:28:2B:E4:DA:BD:5A:74:54:8E:28:B0:DD:FB:26:AF:F9:13:36:67:CD:41:D7",
            CertPins.display(CertPins.sha256(pinned)),
        )
    }

    @Test
    fun matches_onlyPinnedCertOnPinnedHost() {
        assertTrue(CertPins.matches(pin, host, pinned))
        assertTrue(CertPins.matches(pin, "IMMICH.LAN", pinned)) // hosts are case-insensitive
        assertFalse(CertPins.matches(pin, host, other))
        assertFalse(CertPins.matches(pin, "evil.example", pinned))
        assertFalse(CertPins.matches(null, host, pinned))
        assertFalse(CertPins.matches(pin.copy(sha256 = ""), host, pinned))
        assertFalse(CertPins.matches(pin, null, pinned))
        assertFalse(CertPins.matches(pin, host, null))
    }

    @Test
    fun pinFor_scopesToConfiguredHost_andIgnoresBlank() {
        assertEquals(CertPins.Pin("immich.lan", "ab"), CertPins.pinFor("https://immich.lan:2283/sub", "ab"))
        assertEquals(null, CertPins.pinFor("https://immich.lan", ""))
        assertEquals(null, CertPins.pinFor("not a url", "ab"))
    }

    @Test
    fun trustManager_acceptsPinnedCertOnPinnedHost() {
        PinningTrustManager(platform) { pin }.checkServerTrusted(arrayOf(pinned), "ECDHE_ECDSA", engine(host))
    }

    @Test(expected = CertificateException::class)
    fun trustManager_rejectsOtherCertOnPinnedHost() {
        PinningTrustManager(platform) { pin }.checkServerTrusted(arrayOf(other), "ECDHE_ECDSA", engine(host))
    }

    @Test(expected = CertificateException::class)
    fun trustManager_rejectsPinnedCertOnAnotherHost() {
        PinningTrustManager(platform) { pin }.checkServerTrusted(arrayOf(pinned), "ECDHE_ECDSA", engine("evil.example"))
    }

    @Test(expected = CertificateException::class)
    fun trustManager_withoutPin_isPlainPlatformValidation() {
        PinningTrustManager(platform) { null }.checkServerTrusted(arrayOf(pinned), "ECDHE_ECDSA", engine(host))
    }

    @Test(expected = CertificateException::class)
    fun trustManager_hostlessCallback_neverUsesPin() {
        PinningTrustManager(platform) { pin }.checkServerTrusted(arrayOf(pinned), "ECDHE_ECDSA")
    }

    @Test
    fun hostnameVerifier_acceptsPinnedCertOnlyForPinnedHost() {
        val rejectAll = HostnameVerifier { _, _ -> false }
        val verifier = PinningHostnameVerifier(rejectAll) { pin }
        assertTrue(verifier.verify(host, session(pinned)))
        assertFalse(verifier.verify(host, session(other)))
        assertFalse(verifier.verify("evil.example", session(pinned)))
        assertFalse(PinningHostnameVerifier(rejectAll) { null }.verify(host, session(pinned)))
    }

    @Test
    fun hostnameVerifier_keepsPlatformAcceptance() {
        val acceptAll = HostnameVerifier { _, _ -> true }
        assertTrue(PinningHostnameVerifier(acceptAll) { null }.verify("anything", session(other)))
    }

    private fun load(name: String): X509Certificate =
        javaClass.classLoader!!.getResourceAsStream("certs/$name")!!.use {
            CertificateFactory.getInstance("X.509").generateCertificate(it) as X509Certificate
        }

    private fun engine(peerHost: String) = SSLContext.getDefault().createSSLEngine(peerHost, 443)

    private fun session(leaf: X509Certificate): SSLSession =
        Proxy.newProxyInstance(javaClass.classLoader, arrayOf(SSLSession::class.java)) { _, method, _ ->
            when (method.name) {
                "getPeerCertificates" -> arrayOf<java.security.cert.Certificate>(leaf)
                else -> null
            }
        } as SSLSession
}
