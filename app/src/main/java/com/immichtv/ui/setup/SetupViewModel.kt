package com.immichtv.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.immichtv.data.ImmichRepository
import com.immichtv.data.prefs.SettingsRepository
import com.immichtv.domain.ServerSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SetupUiState(
    val url: String = "",
    val apiKey: String = "",
    val allowInsecure: Boolean = false,
    val validating: Boolean = false,
    val error: String? = null,
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
            allowInsecure = settings.snapshot.allowInsecure,
        ),
    )
    val state: StateFlow<SetupUiState> = _state.asStateFlow()

    fun onUrlChange(value: String) = _state.update { it.copy(url = value, error = null) }
    fun onApiKeyChange(value: String) = _state.update { it.copy(apiKey = value, error = null) }
    fun onInsecureChange(value: Boolean) = _state.update { it.copy(allowInsecure = value, error = null) }

    fun validateAndSave(onConfigured: () -> Unit) {
        val current = _state.value
        // The API key is a bearer credential: by default it only travels over TLS, so reject
        // http:// up front. The insecure opt-in (self-hosted servers without a valid cert) waives
        // this — the user has explicitly accepted an unencrypted/untrusted connection.
        if (!current.allowInsecure && current.url.trim().startsWith("http://")) {
            _state.update { it.copy(error = "Only https:// servers are supported unless you enable an insecure connection below") }
            return
        }
        val normalized = ServerSettings(
            baseUrl = normalizeBaseUrl(current.url, current.allowInsecure),
            apiKey = current.apiKey.trim(),
            allowInsecure = current.allowInsecure,
        )
        if (!normalized.isConfigured) {
            _state.update { it.copy(error = "Enter both a server URL and an API key") }
            return
        }

        _state.update { it.copy(validating = true, error = null) }
        // Stage so the interceptors use the entered creds during validation.
        settings.stageForValidation(normalized)

        viewModelScope.launch {
            repository.validateConnection().fold(
                onSuccess = {
                    settings.save(normalized)
                    _state.update { it.copy(validating = false) }
                    onConfigured()
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(
                            validating = false,
                            error = e.message?.let { m -> "Could not connect: $m" }
                                ?: "Could not connect to server",
                        )
                    }
                },
            )
        }
    }

    private fun normalizeBaseUrl(input: String, allowInsecure: Boolean): String {
        var s = input.trim()
        if (s.isEmpty()) return s
        // Respect an explicit scheme; otherwise default to http:// for insecure (self-hosted, likely
        // no TLS) and https:// otherwise.
        if (!s.startsWith("http://") && !s.startsWith("https://")) {
            s = if (allowInsecure) "http://$s" else "https://$s"
        }
        return s.trimEnd('/')
    }
}
