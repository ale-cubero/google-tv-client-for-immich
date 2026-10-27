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
import com.immichtv.ui.i18n.Language
import com.immichtv.ui.theme.HomeTileSize
import com.immichtv.ui.theme.TextSize
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
    private val keyAllowHttp = booleanPreferencesKey("allow_http")
    private val keyPinnedCert = stringPreferencesKey("pinned_cert_sha256")
    // Legacy (≤ v1.46) single "insecure" switch: trusted any certificate AND allowed http://.
    // Read once to keep http:// working for those users; certificates now need explicit pinning.
    private val keyLegacyInsecure = booleanPreferencesKey("allow_insecure")
    private val keySlideshowInterval = intPreferencesKey("slideshow_interval_s")
    private val keySlideshowShuffle = booleanPreferencesKey("slideshow_shuffle")
    private val keyThemeMode = stringPreferencesKey("theme_mode")
    private val keyTextSize = stringPreferencesKey("text_size")
    private val keyHomeTileSize = stringPreferencesKey("home_tile_size")
    private val keyLanguage = stringPreferencesKey("language")

    /** Latest settings, readable synchronously from an interceptor. */
    @Volatile
    var snapshot: ServerSettings = ServerSettings()
        private set

    /** Latest UI language, readable synchronously from the repository (locale-aware formatting). */
    @Volatile
    var languageSnapshot: Language = Language.ENGLISH
        private set

    val settings: Flow<ServerSettings> = dataStore.data.map { it.toSettings() }

    // Declared before init so the collector below doesn't read an uninitialized property.
    val language: Flow<Language> = dataStore.data.map { Language.fromTag(it[keyLanguage]) }

    init {
        // Seed synchronously so the very first request has credentials.
        val seeded = runBlocking { dataStore.data.first() }
        snapshot = seeded.toSettings()
        languageSnapshot = Language.fromTag(seeded[keyLanguage])
        // Keep the snapshots fresh on every change.
        scope.launch { settings.collect { snapshot = it } }
        scope.launch { language.collect { languageSnapshot = it } }
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

    val textSize: Flow<TextSize> = dataStore.data.map {
        when (it[keyTextSize]) {
            TextSize.SMALL.name -> TextSize.SMALL
            TextSize.LARGE.name -> TextSize.LARGE
            else -> TextSize.NORMAL // default & any legacy/unknown value
        }
    }

    suspend fun saveTextSize(size: TextSize) {
        dataStore.edit { it[keyTextSize] = size.name }
    }

    val homeTileSize: Flow<HomeTileSize> = dataStore.data.map {
        when (it[keyHomeTileSize]) {
            HomeTileSize.SMALL.name -> HomeTileSize.SMALL
            HomeTileSize.LARGE.name -> HomeTileSize.LARGE
            else -> HomeTileSize.NORMAL // default & any unknown value
        }
    }

    suspend fun saveHomeTileSize(size: HomeTileSize) {
        dataStore.edit { it[keyHomeTileSize] = size.name }
    }

    suspend fun saveLanguage(language: Language) {
        dataStore.edit { it[keyLanguage] = language.tag }
    }

    /** String catalog for the current language — for non-Compose code (view models, repository). */
    fun currentStrings(): com.immichtv.ui.i18n.Strings =
        com.immichtv.ui.i18n.stringsFor(languageSnapshot)

    /** Wipe all persisted server settings (logout) and reset the in-memory snapshot. */
    suspend fun clear() {
        dataStore.edit { it.clear() }
        snapshot = ServerSettings()
    }

    suspend fun save(settings: ServerSettings) {
        dataStore.edit {
            it[keyUrl] = settings.baseUrl
            it[keyApiKey] = KeystoreCrypto.encrypt(settings.apiKey)
            it[keyAllowHttp] = settings.allowHttp
            it[keyPinnedCert] = settings.pinnedCertSha256
            it.remove(keyLegacyInsecure)
        }
        snapshot = settings
    }

    private fun androidx.datastore.preferences.core.Preferences.toSettings() = ServerSettings(
        baseUrl = this[keyUrl].orEmpty(),
        // Stored encrypted; a decrypt failure (corrupt/legacy value, missing key) reads as
        // "not configured" so the app falls back to the setup screen rather than crashing.
        apiKey = this[keyApiKey]?.let { runCatching { KeystoreCrypto.decrypt(it) }.getOrDefault("") }.orEmpty(),
        allowHttp = this[keyAllowHttp] ?: this[keyLegacyInsecure] ?: false,
        pinnedCertSha256 = this[keyPinnedCert].orEmpty(),
    )
}
