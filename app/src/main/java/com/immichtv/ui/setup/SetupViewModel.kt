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
package com.immichtv.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.immichtv.data.ImmichRepository
import com.immichtv.data.prefs.SettingsRepository
import com.immichtv.data.remote.CertPins
import com.immichtv.data.remote.CertificateProbe
import com.immichtv.domain.ServerSettings
import com.immichtv.ui.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.text.DateFormat
import javax.inject.Inject
import javax.net.ssl.SSLException
import javax.security.auth.x500.X500Principal

/** A server certificate the platform rejected, awaiting the user's decision to trust it. */
data class CertPrompt(
    val host: String,
    val subject: String,
    val issuer: String,
    val validUntil: String,
    val fingerprint: String, // display form, "AB:CD:…"
    val sha256: String,      // what gets pinned if the user trusts it
)

data class SetupUiState(
    val url: String = "",
    val apiKey: String = "",
    val allowHttp: Boolean = false,
    val pinnedCertSha256: String = "",
    val validating: Boolean = false,
    val error: String? = null,
    val certPrompt: CertPrompt? = null,
)

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val repository: ImmichRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(
        SetupUiState(
            url = settings.snapshot.baseUrl,
            apiKey = settings.snapshot.apiKey,
            allowHttp = settings.snapshot.allowHttp,
            pinnedCertSha256 = settings.snapshot.pinnedCertSha256,
        ),
    )
    val state: StateFlow<SetupUiState> = _state.asStateFlow()

    // A trusted certificate belongs to the server it was approved for: editing the URL drops it.
    fun onUrlChange(value: String) = _state.update { it.copy(url = value, pinnedCertSha256 = "", error = null) }
    fun onApiKeyChange(value: String) = _state.update { it.copy(apiKey = value, error = null) }
    fun onAllowHttpChange(value: Boolean) = _state.update { it.copy(allowHttp = value, error = null) }

    fun validateAndSave(onConfigured: () -> Unit) {
        val current = _state.value
        // The API key is a bearer credential: by default it only travels over TLS, so reject
        // http:// up front unless the user has explicitly allowed unencrypted connections.
        val strings = settings.currentStrings()
        if (!current.allowHttp && current.url.trim().startsWith("http://")) {
            _state.update { it.copy(error = strings.setupErrorHttps) }
            return
        }
        val normalized = ServerSettings(
            baseUrl = normalizeBaseUrl(current.url, current.allowHttp),
            apiKey = current.apiKey.trim(),
            allowHttp = current.allowHttp,
            pinnedCertSha256 = current.pinnedCertSha256,
        )
        if (!normalized.isConfigured) {
            _state.update { it.copy(error = strings.setupErrorBoth) }
            return
        }

        _state.update { it.copy(validating = true, error = null) }
        // Stage so the interceptors (and the pinning trust manager) use the entered settings.
        settings.stageForValidation(normalized)

        viewModelScope.launch {
            repository.validateConnection().fold(
                onSuccess = {
                    settings.save(normalized)
                    _state.update { it.copy(validating = false) }
                    onConfigured()
                },
                onFailure = { e ->
                    // The platform rejected the server's certificate (self-signed, private CA, or a
                    // name it doesn't cover): show that certificate and let the user decide.
                    val prompt = if (e is SSLException) certPromptFor(normalized) else null
                    _state.update {
                        it.copy(
                            validating = false,
                            certPrompt = prompt,
                            error = if (prompt == null) e.toUserMessage(settings.currentStrings()) else null,
                        )
                    }
                },
            )
        }
    }

    /** Trust the prompted certificate for this server only, then connect again. */
    fun trustCertificate(onConfigured: () -> Unit) {
        val prompt = _state.value.certPrompt ?: return
        _state.update { it.copy(certPrompt = null, pinnedCertSha256 = prompt.sha256) }
        validateAndSave(onConfigured)
    }

    fun rejectCertificate() = _state.update {
        it.copy(certPrompt = null, error = settings.currentStrings().setupCertRejected)
    }

    private suspend fun certPromptFor(server: ServerSettings): CertPrompt? {
        val leaf = CertificateProbe.fetchLeaf(server.baseUrl) ?: return null
        val sha256 = CertPins.sha256(leaf)
        // Already trusted and still failing: some other TLS problem — report it, don't re-prompt.
        if (sha256.equals(server.pinnedCertSha256, ignoreCase = true)) return null
        val locale = settings.languageSnapshot.toLocale()
        return CertPrompt(
            host = server.baseUrl.toHttpUrlOrNull()?.host.orEmpty(),
            subject = leaf.subjectX500Principal.commonName(),
            issuer = leaf.issuerX500Principal.commonName(),
            validUntil = DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(leaf.notAfter),
            fingerprint = CertPins.display(sha256),
            sha256 = sha256,
        )
    }

    private fun X500Principal.commonName(): String {
        val dn = getName(X500Principal.RFC2253)
        return Regex("""(?:^|,)CN=((?:\\.|[^,])*)""").find(dn)?.groupValues?.get(1) ?: dn
    }

    private fun normalizeBaseUrl(input: String, allowHttp: Boolean): String {
        var s = input.trim()
        if (s.isEmpty()) return s
        // Respect an explicit scheme; otherwise default to http:// when the user allowed unencrypted
        // connections (home-network server without TLS) and https:// otherwise.
        if (!s.startsWith("http://") && !s.startsWith("https://")) {
            s = if (allowHttp) "http://$s" else "https://$s"
        }
        return s.trimEnd('/')
    }
}
