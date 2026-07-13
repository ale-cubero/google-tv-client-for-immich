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
    val retry: String
    val railOnThisDay: String
    val railRecent: String
    val railAlbums: String
    val railPeople: String
    val railPlaces: String
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
    val textSizeLabel: String
    val textSizeSmall: String
    val textSizeNormal: String
    val textSizeLarge: String
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
    val viewerGoToDay: String
    val viewerActionPlayMotion: String
    val viewerActionPlay: String
    val viewerActionPause: String
    val viewerActionDetails: String
    val viewerActionSlideshow: String
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
    override val retry = "Retry"
    override val railOnThisDay = "On this day"
    override val railRecent = "Recent"
    override val railAlbums = "Albums"
    override val railPeople = "People"
    override val railPlaces = "Places"
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
    override val textSizeLabel = "Text size"
    override val textSizeSmall = "Small"
    override val textSizeNormal = "Normal"
    override val textSizeLarge = "Large"
    override val actionDone = "Done"

    override val languageTitle = "Language"

    override val slideshowTitle = "Slideshow"
    override val slideshowInterval = "Photo interval"
    override val slideshowOrder = "Order"
    override val slideshowInOrder = "In order"
    override val slideshowShuffle = "Shuffle"
    override val slideshowNote = "Applies to the viewer slideshow and the screensaver."

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
        "OK options · ◀ ▶ prev/next · hold ◀ ▶ seek"
    override val viewerSlideshow = "Slideshow"
    override val viewerLive = "LIVE"
    override val viewerLiveHint = "LIVE"
    override val viewerVideoError = "Can't play this video.\nThe server may be offline or unreachable."
    override val viewerGoToDay = "Go to this day"
    override val viewerActionPlayMotion = "Play motion photo"
    override val viewerActionPlay = "Play"
    override val viewerActionPause = "Pause"
    override val viewerActionDetails = "Show details"
    override val viewerActionSlideshow = "Start slideshow"
    override val metaLoading ="Loading details…"
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
    override val retry = "Reintentar"
    override val railOnThisDay = "En este día"
    override val railRecent = "Recientes"
    override val railAlbums = "Álbumes"
    override val railPeople = "Personas"
    override val railPlaces = "Lugares"
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
    override val textSizeLabel = "Tamaño del texto"
    override val textSizeSmall = "Pequeño"
    override val textSizeNormal = "Normal"
    override val textSizeLarge = "Grande"
    override val actionDone = "Listo"

    override val languageTitle = "Idioma"

    override val slideshowTitle = "Presentación"
    override val slideshowInterval = "Intervalo de fotos"
    override val slideshowOrder = "Orden"
    override val slideshowInOrder = "En orden"
    override val slideshowShuffle = "Aleatorio"
    override val slideshowNote = "Se aplica a la presentación del visor y al protector de pantalla."

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
        "OK opciones · ◀ ▶ anterior/siguiente · mantén ◀ ▶ avanzar"
    override val viewerSlideshow = "Presentación"
    override val viewerLive = "EN VIVO"
    override val viewerLiveHint = "EN VIVO"
    override val viewerVideoError =
        "No se puede reproducir este video.\nEl servidor puede estar apagado o inaccesible."
    override val viewerGoToDay = "Ir a este día"
    override val viewerActionPlayMotion = "Reproducir foto en movimiento"
    override val viewerActionPlay = "Reproducir"
    override val viewerActionPause = "Pausar"
    override val viewerActionDetails = "Ver detalles"
    override val viewerActionSlideshow = "Iniciar presentación"
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

object Pt : Strings {
    override val timeline = "Linha do tempo"
    override val searchHint = "Busque suas fotos e vídeos"
    override val settings = "Configurações"
    override val retry = "Tentar novamente"
    override val railOnThisDay = "Neste dia"
    override val railRecent = "Recentes"
    override val railAlbums = "Álbuns"
    override val railPeople = "Pessoas"
    override val railPlaces = "Lugares"
    override val railVideos = "Vídeos"
    override val homeLoading = "Carregando sua biblioteca…"
    override val homeEmptyTitle = "Sua biblioteca está vazia"
    override val emptyLibrarySubtitle = "As fotos e vídeos que você adicionar no Immich aparecerão aqui."
    override fun items(n: Int) = if (n == 1) "1 item" else "$n itens"

    override val menuChangeServer = "Alterar servidor"
    override val menuRefresh = "Atualizar"
    override val menuAppearance = "Aparência"
    override val menuLanguage = "Idioma"
    override val menuSlideshow = "Configurações de apresentação"
    override val menuLogout = "Sair"
    override val menuAbout = "Sobre"

    override val appearanceTitle = "Aparência"
    override val themeLabel = "Tema"
    override val themeDark = "Escuro"
    override val themeLight = "Claro"
    override val textSizeLabel = "Tamanho do texto"
    override val textSizeSmall = "Pequeno"
    override val textSizeNormal = "Normal"
    override val textSizeLarge = "Grande"
    override val actionDone = "Concluído"

    override val languageTitle = "Idioma"

    override val slideshowTitle = "Apresentação"
    override val slideshowInterval = "Intervalo das fotos"
    override val slideshowOrder = "Ordem"
    override val slideshowInOrder = "Em ordem"
    override val slideshowShuffle = "Aleatório"
    override val slideshowNote = "Aplica-se à apresentação do visualizador e à proteção de tela."

    override fun version(name: String, code: Int) = "Versão $name ($code)"
    override val aboutDeveloper = "Desenvolvedor"
    override val aboutLicense = "Licença"
    override val aboutGithub = "GitHub"
    override val aboutDescription =
        "Um cliente de Android TV somente leitura para um servidor Immich auto-hospedado. " +
            "Não oficial — sem afiliação com o projeto Immich."
    override val aboutClose = "Fechar"
    override val aboutScan = "Escaneie para ver o repositório"

    override val setupTitle = "Conectar ao Immich"
    override val setupServerUrl = "URL do servidor"
    override val setupApiKey = "Chave de API"
    override val setupShowKey = "Mostrar chave de API"
    override val setupInsecure = "Permitir conexão insegura"
    override val setupInsecureSubtitle =
        "Confie em certificados autoassinados e permita http:// — para servidores auto-hospedados sem um " +
            "certificado válido. A chave de API pode trafegar sem criptografia."
    override val setupConnect = "Conectar"
    override val setupValidating = "Validando…"
    override val setupErrorHttps =
        "Só há suporte para servidores https:// a menos que você ative uma conexão insegura abaixo"
    override val setupErrorBoth = "Informe a URL do servidor e a chave de API"

    override val timelineLoading = "Carregando linha do tempo…"
    override val timelineEmptyTitle = "Sua linha do tempo está vazia"

    override val collectionLoading = "Carregando…"
    override val collectionEmptyTitle = "Ainda não há nada aqui"
    override val collectionEmptySubtitle = "Esta coleção não tem fotos nem vídeos."

    override val searchIdleTitle = "Busque na sua biblioteca"
    override val searchIdleSubtitle =
        "Tente “praia”, “bolo de aniversário”, “cachorro”, “pôr do sol” — " +
            "o Immich busca por significado, não por nome de arquivo."
    override val searchLoading = "Buscando…"
    override val searchNoResultsTitle = "Sem resultados"
    override fun searchNoResultsSubtitle(query: String) =
        "Nada correspondeu a “$query”. Tente outras palavras."
    override fun results(n: Int) = if (n == 1) "1 resultado" else "$n resultados"

    override val viewerHints =
        "OK opções · ◀ ▶ anterior/próximo · segure ◀ ▶ avançar"
    override val viewerSlideshow = "Apresentação"
    override val viewerLive = "AO VIVO"
    override val viewerLiveHint = "AO VIVO"
    override val viewerVideoError =
        "Não é possível reproduzir este vídeo.\nO servidor pode estar offline ou inacessível."
    override val viewerGoToDay = "Ir para este dia"
    override val viewerActionPlayMotion = "Reproduzir foto em movimento"
    override val viewerActionPlay = "Reproduzir"
    override val viewerActionPause = "Pausar"
    override val viewerActionDetails = "Ver detalhes"
    override val viewerActionSlideshow = "Iniciar apresentação"
    override val metaLoading = "Carregando detalhes…"
    override val metaUnavailable = "Detalhes indisponíveis"
    override val metaDynamicRange = "Faixa dinâmica"
    override val metaDate = "Data"
    override val metaLocation = "Local"
    override val metaResolution = "Resolução"
    override val metaCamera = "Câmera"
    override val metaLens = "Lente"
    override val metaExposure = "Exposição"
    override val metaPeople = "Pessoas"
    override val kindPhoto = "Foto"
    override fun kindVideo(playing: Boolean) = "Vídeo · " + if (playing) "reproduzindo" else "pausado"
    override val hdrShown = "exibido em HDR"
    override val hdrToneMapped = "exibido como SDR"
    override val hdrSdr = "SDR"

    override val errUnknownHost =
        "Não foi possível encontrar seu servidor Immich.\nVerifique o endereço do servidor e sua conexão de rede."
    override val errTimeout =
        "Seu servidor Immich não está respondendo.\nEle pode estar offline ou inacessível na sua rede."
    override val errUnreachable =
        "Não foi possível acessar seu servidor Immich.\nVerifique se ele está online e na mesma rede."
    override val errTls =
        "A conexão segura falhou.\nSe seu servidor usa um certificado autoassinado, ative " +
            "\"Permitir conexão insegura\" em Configurações → Alterar servidor."
    override val errApiKey = "Sua chave de API foi rejeitada.\nAtualize-a em Configurações → Alterar servidor."
    override fun errServer(code: Int) =
        "Seu servidor Immich informou um erro (HTTP $code).\nTente novamente em instantes."
    override fun errServerGeneric(code: Int) = "Seu servidor Immich retornou um erro (HTTP $code)."
    override val errGeneric = "Algo deu errado. Tente novamente."

    override val memoryThisYear = "Este ano"
    override fun memoryYearsAgo(years: Int) = if (years == 1) "1 ano atrás" else "$years anos atrás"
}

object It : Strings {
    override val timeline = "Cronologia"
    override val searchHint = "Cerca tra le tue foto e i video"
    override val settings = "Impostazioni"
    override val retry = "Riprova"
    override val railOnThisDay = "In questo giorno"
    override val railRecent = "Recenti"
    override val railAlbums = "Album"
    override val railPeople = "Persone"
    override val railPlaces = "Luoghi"
    override val railVideos = "Video"
    override val homeLoading = "Caricamento della libreria…"
    override val homeEmptyTitle = "La tua libreria è vuota"
    override val emptyLibrarySubtitle = "Le foto e i video aggiunti in Immich appariranno qui."
    override fun items(n: Int) = if (n == 1) "1 elemento" else "$n elementi"

    override val menuChangeServer = "Cambia server"
    override val menuRefresh = "Aggiorna"
    override val menuAppearance = "Aspetto"
    override val menuLanguage = "Lingua"
    override val menuSlideshow = "Impostazioni presentazione"
    override val menuLogout = "Esci"
    override val menuAbout = "Informazioni"

    override val appearanceTitle = "Aspetto"
    override val themeLabel = "Tema"
    override val themeDark = "Scuro"
    override val themeLight = "Chiaro"
    override val textSizeLabel = "Dimensione testo"
    override val textSizeSmall = "Piccolo"
    override val textSizeNormal = "Normale"
    override val textSizeLarge = "Grande"
    override val actionDone = "Fatto"

    override val languageTitle = "Lingua"

    override val slideshowTitle = "Presentazione"
    override val slideshowInterval = "Intervallo foto"
    override val slideshowOrder = "Ordine"
    override val slideshowInOrder = "In ordine"
    override val slideshowShuffle = "Casuale"
    override val slideshowNote = "Si applica alla presentazione del visualizzatore e al salvaschermo."

    override fun version(name: String, code: Int) = "Versione $name ($code)"
    override val aboutDeveloper = "Sviluppatore"
    override val aboutLicense = "Licenza"
    override val aboutGithub = "GitHub"
    override val aboutDescription =
        "Un client Android TV in sola lettura per un server Immich self-hosted. " +
            "Non ufficiale — non affiliato al progetto Immich."
    override val aboutClose = "Chiudi"
    override val aboutScan = "Scansiona per il repository"

    override val setupTitle = "Connetti a Immich"
    override val setupServerUrl = "URL del server"
    override val setupApiKey = "Chiave API"
    override val setupShowKey = "Mostra chiave API"
    override val setupInsecure = "Consenti connessione non sicura"
    override val setupInsecureSubtitle =
        "Considera attendibili i certificati autofirmati e consenti http:// — per server self-hosted senza un " +
            "certificato valido. La chiave API potrebbe viaggiare non criptata."
    override val setupConnect = "Connetti"
    override val setupValidating = "Convalida…"
    override val setupErrorHttps =
        "Sono supportati solo server https:// a meno che tu non abiliti una connessione non sicura qui sotto"
    override val setupErrorBoth = "Inserisci sia l'URL del server sia la chiave API"

    override val timelineLoading = "Caricamento cronologia…"
    override val timelineEmptyTitle = "La tua cronologia è vuota"

    override val collectionLoading = "Caricamento…"
    override val collectionEmptyTitle = "Ancora niente qui"
    override val collectionEmptySubtitle = "Questa raccolta non contiene foto né video."

    override val searchIdleTitle = "Cerca nella tua libreria"
    override val searchIdleSubtitle =
        "Prova “spiaggia”, “torta di compleanno”, “cane”, “tramonto” — " +
            "Immich cerca per significato, non per nome file."
    override val searchLoading = "Ricerca in corso…"
    override val searchNoResultsTitle = "Nessun risultato"
    override fun searchNoResultsSubtitle(query: String) =
        "Nulla corrisponde a “$query”. Prova con altre parole."
    override fun results(n: Int) = if (n == 1) "1 risultato" else "$n risultati"

    override val viewerHints =
        "OK opzioni · ◀ ▶ prec/succ · tieni ◀ ▶ scorri"
    override val viewerSlideshow = "Presentazione"
    override val viewerLive = "LIVE"
    override val viewerLiveHint = "LIVE"
    override val viewerVideoError =
        "Impossibile riprodurre questo video.\nIl server potrebbe essere offline o irraggiungibile."
    override val viewerGoToDay = "Vai a questo giorno"
    override val viewerActionPlayMotion = "Riproduci foto animata"
    override val viewerActionPlay = "Riproduci"
    override val viewerActionPause = "Pausa"
    override val viewerActionDetails = "Mostra dettagli"
    override val viewerActionSlideshow = "Avvia presentazione"
    override val metaLoading = "Caricamento dettagli…"
    override val metaUnavailable = "Dettagli non disponibili"
    override val metaDynamicRange = "Gamma dinamica"
    override val metaDate = "Data"
    override val metaLocation = "Posizione"
    override val metaResolution = "Risoluzione"
    override val metaCamera = "Fotocamera"
    override val metaLens = "Obiettivo"
    override val metaExposure = "Esposizione"
    override val metaPeople = "Persone"
    override val kindPhoto = "Foto"
    override fun kindVideo(playing: Boolean) = "Video · " + if (playing) "in riproduzione" else "in pausa"
    override val hdrShown = "mostrato in HDR"
    override val hdrToneMapped = "mostrato come SDR"
    override val hdrSdr = "SDR"

    override val errUnknownHost =
        "Impossibile trovare il tuo server Immich.\nControlla l'indirizzo del server e la tua connessione di rete."
    override val errTimeout =
        "Il tuo server Immich non risponde.\nPotrebbe essere offline o irraggiungibile sulla tua rete."
    override val errUnreachable =
        "Impossibile raggiungere il tuo server Immich.\nAssicurati che sia online e sulla stessa rete."
    override val errTls =
        "Connessione sicura non riuscita.\nSe il tuo server usa un certificato autofirmato, abilita " +
            "\"Consenti connessione non sicura\" in Impostazioni → Cambia server."
    override val errApiKey = "La tua chiave API è stata rifiutata.\nAggiornala in Impostazioni → Cambia server."
    override fun errServer(code: Int) =
        "Il tuo server Immich ha segnalato un errore (HTTP $code).\nRiprova tra poco."
    override fun errServerGeneric(code: Int) = "Il tuo server Immich ha restituito un errore (HTTP $code)."
    override val errGeneric = "Qualcosa è andato storto. Riprova."

    override val memoryThisYear = "Quest'anno"
    override fun memoryYearsAgo(years: Int) = if (years == 1) "1 anno fa" else "$years anni fa"
}

/** The catalog for a given language — for non-Compose code that has a [Language] but no composition. */
fun stringsFor(language: Language): Strings = when (language) {
    Language.ENGLISH -> En
    Language.SPANISH -> Es
    Language.PORTUGUESE -> Pt
    Language.ITALIAN -> It
}

/** Provided by the app root; every composable reads UI text from here. */
val LocalStrings = staticCompositionLocalOf<Strings> { En }
