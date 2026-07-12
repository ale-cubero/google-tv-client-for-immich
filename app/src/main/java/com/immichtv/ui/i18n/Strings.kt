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
package com.immichtv.ui.i18n

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The full UI string catalog. A single source of truth used both from Compose (via [LocalStrings])
 * and from non-Compose code (repository, view models — via [stringsFor]), so switching language
 * updates everything live without an Android resource-qualifier / activity-recreation dance.
 */
interface Strings {
    // Home
    val timeline: String
    val searchHint: String
    val settings: String
    val railOnThisDay: String
    val railRecent: String
    val railAlbums: String
    val railPeople: String
    val railVideos: String
    val homeLoading: String
    val homeEmptyTitle: String
    val emptyLibrarySubtitle: String
    fun items(n: Int): String

    // Settings menu
    val menuChangeServer: String
    val menuRefresh: String
    val menuAppearance: String
    val menuLanguage: String
    val menuSlideshow: String
    val menuLogout: String
    val menuAbout: String

    // Appearance
    val appearanceTitle: String
    val themeLabel: String
    val themeDark: String
    val themeLight: String
    val actionDone: String

    // Language
    val languageTitle: String

    // Slideshow
    val slideshowTitle: String
    val slideshowInterval: String
    val slideshowOrder: String
    val slideshowInOrder: String
    val slideshowShuffle: String
    val slideshowNote: String

    // About
    fun version(name: String, code: Int): String
    val aboutDeveloper: String
    val aboutLicense: String
    val aboutGithub: String
    val aboutDescription: String
    val aboutClose: String
    val aboutScan: String

    // Setup
    val setupTitle: String
    val setupServerUrl: String
    val setupApiKey: String
    val setupShowKey: String
    val setupInsecure: String
    val setupInsecureSubtitle: String
    val setupConnect: String
    val setupValidating: String
    val setupErrorHttps: String
    val setupErrorBoth: String

    // Timeline
    val timelineLoading: String
    val timelineEmptyTitle: String

    // Collection
    val collectionLoading: String
    val collectionEmptyTitle: String
    val collectionEmptySubtitle: String

    // Search
    val searchIdleTitle: String
    val searchIdleSubtitle: String
    val searchLoading: String
    val searchNoResultsTitle: String
    fun searchNoResultsSubtitle(query: String): String
    fun results(n: Int): String

    // Viewer
    val viewerHints: String
    val viewerSlideshow: String
    val viewerLive: String
    val viewerLiveHint: String
    val viewerVideoError: String
    val metaLoading: String
    val metaUnavailable: String
    val metaDynamicRange: String
    val metaDate: String
    val metaLocation: String
    val metaResolution: String
    val metaCamera: String
    val metaLens: String
    val metaExposure: String
    val metaPeople: String
    val kindPhoto: String
    fun kindVideo(playing: Boolean): String
    // Dynamic-range output state (source labels HDR10/HLG/SDR stay as-is — technical terms)
    val hdrShown: String
    val hdrToneMapped: String
    val hdrSdr: String

    // Errors (Throwable.toUserMessage)
    val errUnknownHost: String
    val errTimeout: String
    val errUnreachable: String
    val errTls: String
    val errApiKey: String
    fun errServer(code: Int): String
    fun errServerGeneric(code: Int): String
    val errGeneric: String

    // Memories ("On this day" titles)
    val memoryThisYear: String
    fun memoryYearsAgo(years: Int): String
}

object En : Strings {
    override val timeline = "Timeline"
    override val searchHint = "Search your photos and videos"
    override val settings = "Settings"
    override val railOnThisDay = "On this day"
    override val railRecent = "Recent"
    override val railAlbums = "Albums"
    override val railPeople = "People"
    override val railVideos = "Videos"
    override val homeLoading = "Loading your library…"
    override val homeEmptyTitle = "Your library is empty"
    override val emptyLibrarySubtitle = "Photos and videos added in Immich will show up here."
    override fun items(n: Int) = if (n == 1) "1 item" else "$n items"

    override val menuChangeServer = "Change server"
    override val menuRefresh = "Refresh"
    override val menuAppearance = "Appearance"
    override val menuLanguage = "Language"
    override val menuSlideshow = "Slideshow settings"
    override val menuLogout = "Logout"
    override val menuAbout = "About"

    override val appearanceTitle = "Appearance"
    override val themeLabel = "Theme"
    override val themeDark = "Dark"
    override val themeLight = "Light"
    override val actionDone = "Done"

    override val languageTitle = "Language"

    override val slideshowTitle = "Slideshow"
    override val slideshowInterval = "Photo interval"
    override val slideshowOrder = "Order"
    override val slideshowInOrder = "In order"
    override val slideshowShuffle = "Shuffle"
    override val slideshowNote = "Applies to the viewer slideshow (⏯) and the screensaver."

    override fun version(name: String, code: Int) = "Version $name ($code)"
    override val aboutDeveloper = "Developer"
    override val aboutLicense = "License"
    override val aboutGithub = "GitHub"
    override val aboutDescription =
        "A read-only Android TV client for a self-hosted Immich server. " +
            "Unofficial — not affiliated with the Immich project."
    override val aboutClose = "Close"
    override val aboutScan = "Scan for the repo"

    override val setupTitle = "Connect to Immich"
    override val setupServerUrl = "Server URL"
    override val setupApiKey = "API Key"
    override val setupShowKey = "Show API key"
    override val setupInsecure = "Allow insecure connection"
    override val setupInsecureSubtitle =
        "Trust self-signed certificates and allow http:// — for self-hosted servers without a valid " +
            "certificate. The API key may travel unencrypted."
    override val setupConnect = "Connect"
    override val setupValidating = "Validating…"
    override val setupErrorHttps =
        "Only https:// servers are supported unless you enable an insecure connection below"
    override val setupErrorBoth = "Enter both a server URL and an API key"

    override val timelineLoading = "Loading timeline…"
    override val timelineEmptyTitle = "Your timeline is empty"

    override val collectionLoading = "Loading…"
    override val collectionEmptyTitle = "Nothing here yet"
    override val collectionEmptySubtitle = "This collection has no photos or videos."

    override val searchIdleTitle = "Search your library"
    override val searchIdleSubtitle =
        "Try “beach”, “birthday cake”, “dog”, “sunset” — " +
            "Immich matches by meaning, not filenames."
    override val searchLoading = "Searching…"
    override val searchNoResultsTitle = "No results"
    override fun searchNoResultsSubtitle(query: String) =
        "Nothing matched “$query”. Try different words."
    override fun results(n: Int) = if (n == 1) "1 result" else "$n results"

    override val viewerHints =
        "OK play/pause · ◀ ▶ prev/next · hold ◀ ▶ seek · ▼ details · ⏯ slideshow"
    override val viewerSlideshow = "Slideshow"
    override val viewerLive = "LIVE"
    override val viewerLiveHint = "LIVE  ·  OK to play"
    override val viewerVideoError = "Can't play this video.\nThe server may be offline or unreachable."
    override val metaLoading = "Loading details…"
    override val metaUnavailable = "Details unavailable"
    override val metaDynamicRange = "Dynamic range"
    override val metaDate = "Date"
    override val metaLocation = "Location"
    override val metaResolution = "Resolution"
    override val metaCamera = "Camera"
    override val metaLens = "Lens"
    override val metaExposure = "Exposure"
    override val metaPeople = "People"
    override val kindPhoto = "Photo"
    override fun kindVideo(playing: Boolean) = "Video · " + if (playing) "playing" else "paused"
    override val hdrShown = "shown in HDR"
    override val hdrToneMapped = "displayed as SDR"
    override val hdrSdr = "SDR"

    override val errUnknownHost =
        "Can't find your Immich server.\nCheck the server address and your network connection."
    override val errTimeout =
        "Your Immich server isn't responding.\nIt may be offline or unreachable on your network."
    override val errUnreachable =
        "Can't reach your Immich server.\nMake sure it's online and on the same network."
    override val errTls =
        "Secure connection failed.\nIf your server uses a self-signed certificate, enable " +
            "\"Allow insecure connection\" in Settings → Change server."
    override val errApiKey = "Your API key was rejected.\nUpdate it in Settings → Change server."
    override fun errServer(code: Int) =
        "Your Immich server reported an error (HTTP $code).\nTry again shortly."
    override fun errServerGeneric(code: Int) = "Your Immich server returned an error (HTTP $code)."
    override val errGeneric = "Something went wrong. Please try again."

    override val memoryThisYear = "This year"
    override fun memoryYearsAgo(years: Int) = if (years == 1) "1 year ago" else "$years years ago"
}

object Es : Strings {
    override val timeline = "Línea de tiempo"
    override val searchHint = "Busca tus fotos y videos"
    override val settings = "Ajustes"
    override val railOnThisDay = "En este día"
    override val railRecent = "Recientes"
    override val railAlbums = "Álbumes"
    override val railPeople = "Personas"
    override val railVideos = "Videos"
    override val homeLoading = "Cargando tu biblioteca…"
    override val homeEmptyTitle = "Tu biblioteca está vacía"
    override val emptyLibrarySubtitle = "Las fotos y videos que añadas en Immich aparecerán aquí."
    override fun items(n: Int) = if (n == 1) "1 elemento" else "$n elementos"

    override val menuChangeServer = "Cambiar servidor"
    override val menuRefresh = "Actualizar"
    override val menuAppearance = "Apariencia"
    override val menuLanguage = "Idioma"
    override val menuSlideshow = "Ajustes de presentación"
    override val menuLogout = "Cerrar sesión"
    override val menuAbout = "Acerca de"

    override val appearanceTitle = "Apariencia"
    override val themeLabel = "Tema"
    override val themeDark = "Oscuro"
    override val themeLight = "Claro"
    override val actionDone = "Listo"

    override val languageTitle = "Idioma"

    override val slideshowTitle = "Presentación"
    override val slideshowInterval = "Intervalo de fotos"
    override val slideshowOrder = "Orden"
    override val slideshowInOrder = "En orden"
    override val slideshowShuffle = "Aleatorio"
    override val slideshowNote = "Se aplica a la presentación del visor (⏯) y al protector de pantalla."

    override fun version(name: String, code: Int) = "Versión $name ($code)"
    override val aboutDeveloper = "Desarrollador"
    override val aboutLicense = "Licencia"
    override val aboutGithub = "GitHub"
    override val aboutDescription =
        "Un cliente de Android TV de solo lectura para un servidor Immich autoalojado. " +
            "No oficial — sin afiliación con el proyecto Immich."
    override val aboutClose = "Cerrar"
    override val aboutScan = "Escanea para ver el repositorio"

    override val setupTitle = "Conéctate a Immich"
    override val setupServerUrl = "URL del servidor"
    override val setupApiKey = "Clave API"
    override val setupShowKey = "Mostrar clave API"
    override val setupInsecure = "Permitir conexión insegura"
    override val setupInsecureSubtitle =
        "Confía en certificados autofirmados y permite http:// — para servidores autoalojados sin un " +
            "certificado válido. La clave API podría viajar sin cifrar."
    override val setupConnect = "Conectar"
    override val setupValidating = "Validando…"
    override val setupErrorHttps =
        "Solo se admiten servidores https:// a menos que actives una conexión insegura más abajo"
    override val setupErrorBoth = "Introduce la URL del servidor y la clave API"

    override val timelineLoading = "Cargando línea de tiempo…"
    override val timelineEmptyTitle = "Tu línea de tiempo está vacía"

    override val collectionLoading = "Cargando…"
    override val collectionEmptyTitle = "Aún no hay nada aquí"
    override val collectionEmptySubtitle = "Esta colección no tiene fotos ni videos."

    override val searchIdleTitle = "Busca en tu biblioteca"
    override val searchIdleSubtitle =
        "Prueba “playa”, “pastel de cumpleaños”, “perro”, “atardecer” — " +
            "Immich busca por significado, no por nombre de archivo."
    override val searchLoading = "Buscando…"
    override val searchNoResultsTitle = "Sin resultados"
    override fun searchNoResultsSubtitle(query: String) =
        "Nada coincidió con “$query”. Prueba con otras palabras."
    override fun results(n: Int) = if (n == 1) "1 resultado" else "$n resultados"

    override val viewerHints =
        "OK reproducir/pausar · ◀ ▶ anterior/siguiente · mantén ◀ ▶ avanzar · ▼ detalles · ⏯ presentación"
    override val viewerSlideshow = "Presentación"
    override val viewerLive = "EN VIVO"
    override val viewerLiveHint = "EN VIVO  ·  OK para reproducir"
    override val viewerVideoError =
        "No se puede reproducir este video.\nEl servidor puede estar apagado o inaccesible."
    override val metaLoading = "Cargando detalles…"
    override val metaUnavailable = "Detalles no disponibles"
    override val metaDynamicRange = "Rango dinámico"
    override val metaDate = "Fecha"
    override val metaLocation = "Ubicación"
    override val metaResolution = "Resolución"
    override val metaCamera = "Cámara"
    override val metaLens = "Objetivo"
    override val metaExposure = "Exposición"
    override val metaPeople = "Personas"
    override val kindPhoto = "Foto"
    override fun kindVideo(playing: Boolean) = "Video · " + if (playing) "reproduciendo" else "pausado"
    override val hdrShown = "mostrado en HDR"
    override val hdrToneMapped = "mostrado como SDR"
    override val hdrSdr = "SDR"

    override val errUnknownHost =
        "No se encuentra tu servidor Immich.\nRevisa la dirección del servidor y tu conexión de red."
    override val errTimeout =
        "Tu servidor Immich no responde.\nPuede estar apagado o inaccesible en tu red."
    override val errUnreachable =
        "No se puede acceder a tu servidor Immich.\nAsegúrate de que esté encendido y en la misma red."
    override val errTls =
        "La conexión segura falló.\nSi tu servidor usa un certificado autofirmado, activa " +
            "\"Permitir conexión insegura\" en Ajustes → Cambiar servidor."
    override val errApiKey = "Tu clave API fue rechazada.\nActualízala en Ajustes → Cambiar servidor."
    override fun errServer(code: Int) =
        "Tu servidor Immich informó un error (HTTP $code).\nInténtalo de nuevo en un momento."
    override fun errServerGeneric(code: Int) = "Tu servidor Immich devolvió un error (HTTP $code)."
    override val errGeneric = "Algo salió mal. Inténtalo de nuevo."

    override val memoryThisYear = "Este año"
    override fun memoryYearsAgo(years: Int) = if (years == 1) "Hace 1 año" else "Hace $years años"
}

/** The catalog for a given language — for non-Compose code that has a [Language] but no composition. */
fun stringsFor(language: Language): Strings = when (language) {
    Language.ENGLISH -> En
    Language.SPANISH -> Es
}

/** Provided by the app root; every composable reads UI text from here. */
val LocalStrings = staticCompositionLocalOf<Strings> { En }
