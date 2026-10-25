# TV client for Immich

A native **Google TV / Android TV** app for browsing your self-hosted [Immich](https://immich.app) photo & video library from the couch. Fast remote-control navigation, hardware-decoded video with HDR, and a **strictly read-only** connection: the app can look at your library, never change it.

> Unofficial, sideloaded app (not on the Play Store). Not affiliated with the Immich project.

![Home screen](docs/screenshots/home.png)

## Features

- **Browse your whole library** — an "On this day" memories row, Recent, Albums, People, Places and Videos, plus a month-by-month timeline of everything.
- **Smart search** — search by what's *in* the photo ("beach", "birthday cake", "dog"), using your Immich server's smart search.
- **Smooth on huge libraries** — tens of thousands of photos scroll smoothly, with a year rail to jump across years instantly.
- **Photo & video viewer** — full-screen, hardware-decoded playback with **HDR**, and Motion Photos (Samsung / Apple / Google live photos).
- **Slideshow & screensaver** — start a slideshow from any photo, or use the app as your TV's screensaver.
- **Light & dark themes**, three text sizes, and **English, Spanish, Portuguese and Italian**.
- **Read-only & private** — cannot modify your server; your API key is encrypted on the device.
- **Made for the remote** — every screen works with just **D-pad, OK and Back**.

## Install on your Google TV

You'll need:

- A **Google TV or Android TV** device running Android 8.0 or newer (developed on a Google TV Streamer 4K).
- An **Immich server** the TV can reach — tested with Immich **v3.x**.
- A **computer on the same network** with Android's free [platform-tools](https://developer.android.com/tools/releases/platform-tools) installed (this provides the `adb` command).
- The app's **APK file** — download `immich-tv-<version>.apk` from the [latest release](https://github.com/ale-cubero/google-tv-client-for-immich/releases/latest).

### 1. Turn on developer mode on the TV

1. On the TV, open **Settings → System → About**.
2. Scroll to **Android TV OS build** and press **OK** on it 7 times, until it says you're a developer.
3. Go back to **Settings → System → Developer options** and turn on **Wireless debugging** (on older devices, **Network debugging** / **ADB debugging**).

### 2. Install the app from your computer

On the **Wireless debugging** screen, choose **Pair device with pairing code**. The TV shows an IP address, a port and a 6-digit code. On your computer, run:

```bash
adb pair <tv-ip>:<pairing-port>        # enter the 6-digit code when asked
adb connect <tv-ip>:<port>             # the IP and port shown under "Wireless debugging"
adb install -r immich-tv-<version>.apk
```

If the TV asks **"Allow debugging?"**, choose **Always allow**. The app then appears in your TV's apps list as **TV client for Immich**.

> To **update** later, download the newest APK from [Releases](https://github.com/ale-cubero/google-tv-client-for-immich/releases/latest) and run the same `adb install -r` — your server settings are kept. To remove it, uninstall it from the TV's app settings like any other app.

### 3. Create an API key in Immich

In the Immich web app, open your **profile picture → Account Settings → API Keys → New API Key**.

**Recommended:** give the key **read permissions only**. The app never writes, and a read-only key means the server will refuse writes too. The app needs read access to assets (including viewing thumbnails and videos), the timeline, albums, memories, people, search and server info. If a section later shows *"Couldn't load this section"*, the key is probably missing that area's read permission.

### 4. Connect

Open the app, then enter:

- **Server URL** — the address you use for Immich in a browser, e.g. `https://photos.example.com`.
- **API key** — the key from step 3.

Choose **Connect**. You can change these later under **Settings → Change server**.

> **Self-signed certificate or no HTTPS?** Turn on **Allow insecure connection** on the setup screen. Only do this on a network you trust, because the API key may then travel unencrypted.

## Using the app

| Where | Button | What it does |
| --- | --- | --- |
| Everywhere | D-pad / **OK** / **Back** | Move, open, go back |
| Timeline | **←** from the left-most column | Jump to the year rail; **↑/↓** scrubs through years, **←** returns to the photos |
| Viewer | **← / →** | Previous / next photo or video (opened from the timeline, it continues across months) |
| Viewer (video) | **Hold ← / →** | Seek backward / forward |
| Viewer | **OK** | Opens an action bar: **Show details** (date, place, camera, people), **Start slideshow**, **Play / Pause** (videos), **Play motion photo**, **Go to this day** |
| Viewer | **↓** | Show the control hints again; scrolls the details panel when it's open |

The **Settings** button on Home has: change server, refresh, appearance (theme & text size), language, slideshow options (interval, shuffle), log out, and about.

## Screenshots

**Home**

| Dark | Light |
| --- | --- |
| ![Home screen, dark](docs/screenshots/home.png) | ![Home screen, light](docs/screenshots/home-light.png) |

**Timeline** — month-by-month grid with the year rail on the right

| Dark | Light |
| --- | --- |
| ![Timeline, dark](docs/screenshots/timeline.png) | ![Timeline, light](docs/screenshots/timeline-light.png) |

**Viewer** — with the details panel open (OK → Show details). The viewer stays dark in both themes, since photos and videos look best on black.

![Viewer with metadata panel](docs/screenshots/viewer-metadata.png)

<sub>Screenshots taken against the public <a href="https://demo.immich.app">Immich demo library</a> — no personal data.</sub>

## Troubleshooting

- **Can't connect** — check the URL opens in a browser on the same network, includes `https://` (or turn on *Allow insecure connection* for `http://` / self-signed servers), and that the API key hasn't been deleted in Immich.
- **A section says "Couldn't load this section"** — press **Retry**. If it keeps failing, the API key may lack read permission for that area, or the server version may be unsupported (the app targets Immich v3.x).
- **"On this day" is missing** — that's normal on days your library has no memories.
- **Smart search finds nothing** — smart search needs machine learning enabled on your Immich server.
- **A video won't play** — the app plays whatever the server sends. If Immich's video transcoding is turned off, your device may not support that video's codec; turning transcoding on in Immich's video settings usually fixes it.
- **Screensaver** — choose **TV client for Immich** under **Settings → System → Screensaver**, where your device lets you pick a third-party screensaver.
- **HDR** — HDR output depends on your TV and its HDMI settings.

## Read-only and private by design

- **Can't modify your server.** No upload, delete, favorite or edit. A network-level guard (`ReadOnlyInterceptor`, unit-tested) blocks every request except reads (`GET`/`HEAD`) and two known read-only `POST`s (token validation, metadata search). Pair it with a read-only API key for defense in depth.
- **Your API key is protected.** It's encrypted at rest with a non-exportable Android Keystore AES-GCM key, sent only to your server's own address (never across redirects), masked on screen, redacted from logs, and excluded from device backups.

## For contributors

Pull requests are welcome. This is a personal project, so reviews are best-effort and there's no formal release process or issue triage.

### Building from source

Requires a **JDK 17** and the **Android SDK** (platform 35).

```bash
./gradlew :app:assembleDebug          # debug build
./gradlew :app:testDebugUnitTest      # unit tests
./gradlew :app:assembleRelease        # signed release → app/build/outputs/apk/release/app-release.apk
```

A release build needs a keystore and a non-committed `keystore.properties` at the repo root:

```properties
storeFile=/absolute/path/to/immich-tv-release.jks
storePassword=…
keyAlias=…
keyPassword=…
```

`keystore.properties`, `local.properties` and `*.jks` are git-ignored — never commit them. To install an update over an existing install, bump `versionCode` in `app/build.gradle.kts`.

For development, the Android TV (1080p) emulator works well; video decoding and the screensaver are best checked on a real device.

### Tech stack

Kotlin · Jetpack Compose for TV (`androidx.tv:tv-material`) · Media3 ExoPlayer · Coil 3 · Retrofit + kotlinx.serialization · one shared OkHttp client (used by Retrofit, Coil and ExoPlayer, so all three carry auth) · Hilt · DataStore · MVVM + repository + Kotlin Flows. `minSdk 26`, `targetSdk 35`.

### How it works

- **Timeline** — built on Immich's month "buckets": the app loads the list of months up front (so scroll length and month headers are known immediately), then loads each month's photos lazily as it comes into view, prefetching ahead and evicting far-off months from a small LRU cache. Immich v3.x returns these as a compact columnar (structure-of-arrays) payload.
- **Viewer** — shows the already-cached thumbnail instantly, then the high-res preview. Videos stream through ExoPlayer from Immich's playback endpoint; the server decides direct-play vs. transcode.
- **API layer** — hand-written for the dozen or so endpoints in use, not generated from Immich's OpenAPI spec.

### Ground rules for changes

- **Read-only is a hard requirement.** Never add an endpoint or UI that writes to the server, and don't weaken `ReadOnlyInterceptor`. A new read-only `POST` needs to be added to its allowlist deliberately, with a justification.
- **Remote-only.** Every screen must work with D-pad + OK + Back; no touch-only controls. Keep ~5% overscan-safe margins.
- **Translations.** User-facing text lives in `ui/i18n/Strings.kt` — add every new string in all four languages.
- **Never commit** a server URL, API key or keystore.

## Status

In daily use on a Google TV Streamer 4K with a ~14k-item library. All core screens are built: Home, timeline, albums, people, places, smart search, the photo/video viewer, slideshow and screensaver. Expect the occasional rough edge.

## License

[GNU AGPL-3.0](LICENSE) © 2026 Alejandro Cubero — the same license as the [Immich](https://immich.app) project this client is built for.

This is an **unofficial** third-party client and is **not affiliated with, endorsed by, or sponsored by the Immich project**. "Immich" and the Immich logo are property of their respective owners; this project uses its own icon and only references the Immich name descriptively.

## Acknowledgements

Built on top of the excellent [Immich](https://immich.app) project.
