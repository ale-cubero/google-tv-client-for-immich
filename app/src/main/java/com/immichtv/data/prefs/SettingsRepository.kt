package com.immichtv.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.immichtv.data.crypto.KeystoreCrypto
import com.immichtv.domain.ServerSettings
import com.immichtv.domain.SlideshowPrefs
import com.immichtv.ui.theme.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "server_settings")

/**
 * Persists server URL + API key (DataStore Preferences).
 *
 * Also exposes a synchronous [snapshot] of the current settings for the OkHttp interceptors,
 * which run on a blocking thread and cannot suspend. The snapshot is seeded once at startup
 * and kept up to date by collecting the DataStore flow.
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val dataStore = context.dataStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val keyUrl = stringPreferencesKey("server_url")
    private val keyApiKey = stringPreferencesKey("api_key")
    private val keyInsecure = booleanPreferencesKey("allow_insecure")
    private val keySlideshowInterval = intPreferencesKey("slideshow_interval_s")
    private val keySlideshowShuffle = booleanPreferencesKey("slideshow_shuffle")
    private val keyThemeMode = stringPreferencesKey("theme_mode")

    /** Latest settings, readable synchronously from an interceptor. */
    @Volatile
    var snapshot: ServerSettings = ServerSettings()
        private set

    val settings: Flow<ServerSettings> = dataStore.data.map { it.toSettings() }

    init {
        // Seed synchronously so the very first request has credentials.
        snapshot = runBlocking { dataStore.data.first().toSettings() }
        // Keep the snapshot fresh on every change.
        scope.launch { settings.collect { snapshot = it } }
    }

    /** Update the in-memory snapshot only — used to validate entered creds before persisting. */
    fun stageForValidation(settings: ServerSettings) {
        snapshot = settings
    }

    val slideshowPrefs: Flow<SlideshowPrefs> = dataStore.data.map {
        SlideshowPrefs(
            intervalSeconds = it[keySlideshowInterval] ?: 6,
            shuffle = it[keySlideshowShuffle] ?: false,
        )
    }

    suspend fun saveSlideshowPrefs(prefs: SlideshowPrefs) {
        dataStore.edit {
            it[keySlideshowInterval] = prefs.intervalSeconds
            it[keySlideshowShuffle] = prefs.shuffle
        }
    }

    val themeMode: Flow<ThemeMode> = dataStore.data.map {
        when (it[keyThemeMode]) {
            ThemeMode.LIGHT.name -> ThemeMode.LIGHT
            else -> ThemeMode.DARK // default & any legacy/unknown value
        }
    }

    suspend fun saveThemeMode(mode: ThemeMode) {
        dataStore.edit { it[keyThemeMode] = mode.name }
    }

    /** Wipe all persisted server settings (logout) and reset the in-memory snapshot. */
    suspend fun clear() {
        dataStore.edit { it.clear() }
        snapshot = ServerSettings()
    }

    suspend fun save(settings: ServerSettings) {
        dataStore.edit {
            it[keyUrl] = settings.baseUrl
            it[keyApiKey] = KeystoreCrypto.encrypt(settings.apiKey)
            it[keyInsecure] = settings.allowInsecure
        }
        snapshot = settings
    }

    private fun androidx.datastore.preferences.core.Preferences.toSettings() = ServerSettings(
        baseUrl = this[keyUrl].orEmpty(),
        // Stored encrypted; a decrypt failure (corrupt/legacy value, missing key) reads as
        // "not configured" so the app falls back to the setup screen rather than crashing.
        apiKey = this[keyApiKey]?.let { runCatching { KeystoreCrypto.decrypt(it) }.getOrDefault("") }.orEmpty(),
        allowInsecure = this[keyInsecure] ?: false,
    )
}
