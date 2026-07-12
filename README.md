# TV client for Immich

A native **Android TV / Google TV** client for a self-hosted [Immich](https://immich.app) photo & video server. Built for the living-room: fast D‑pad navigation, hardware‑decoded video, and a **strictly read‑only** connection to your library.

> Personal project, sideloaded to a Google TV Streamer — no Play Store. Not affiliated with the Immich project.

## Features

- 🖼️ **Browse your whole library** — a Home screen of memories and rails, a fast month‑by‑month timeline, and album & people views.
- 🔍 **Smart search** — find photos by what's in them, using Immich's semantic (CLIP) search and object recognition — search "beach", "birthday cake", or "dog", not filenames.
- ⚡ **Smooth on huge libraries** — buttery scrolling of tens of thousands of photos, with a year scrubber to jump across the years instantly.
- ▶️ **Photo & video viewer** — full‑screen playback with hardware decoding, **HDR** support, and Motion Photos (Samsung/Apple/Google live photos).
- 🎞️ **Slideshow & screensaver** — turn any view into a slideshow, or let the app be your TV's screensaver.
- 🎨 **Light & dark themes** — styled after Immich's own colors.
- 🌐 **English & Spanish** — switch the interface language in Settings.
- 🔐 **Read‑only & private** — the app can never modify your server; your API key is encrypted on‑device.
- 🏠 **Self‑hosting friendly** — an optional insecure mode for servers with self‑signed certificates or no valid certificate.
- 🕹️ **Made for the remote** — every screen is fully operable with just **D‑pad + OK + Back**.

## Screenshots

The app ships with **dark and light themes**, styled after Immich's own colors.

**Home** — memories, recent, albums, people, and videos rails

| Dark | Light |
| --- | --- |
| ![Home screen, dark](docs/screenshots/home.png) | ![Home screen, light](docs/screenshots/home-light.png) |

**Timeline** — bucketed grid with a year scrubber on the right edge

| Dark | Light |
| --- | --- |
| ![Timeline, dark](docs/screenshots/timeline.png) | ![Timeline, light](docs/screenshots/timeline-light.png) |

**Viewer** — full‑screen photo with the metadata panel (OK on a photo). The viewer stays dark in both themes, as photos and videos look best on black.

![Viewer with metadata panel](docs/screenshots/viewer-metadata.png)

<sub>Screenshots taken against the public <a href="https://demo.immich.app">Immich demo library</a> — no personal data.</sub>

## In detail

- **Home** — an "On this day" memories row plus Recent, Albums, People, and Videos rails, and a Settings menu (change server, refresh, slideshow settings, logout, about).
- **Timeline** — the whole library in one smooth grid, driven by Immich's month "buckets": all month boundaries are known up front, each month's assets load lazily as you scroll, with prefetch and an LRU that evicts far‑off months. A year scrubber on the right edge jumps across a multi‑year library instantly (and always shows the year you're in).
- **Search** — a search bar at the top of Home runs Immich's smart (CLIP) search: results match by meaning and recognized objects/scenes, not filenames. Pick any result to open it in the viewer.
- **Albums & People** — browse an album or a person's photos in the same grid.
- **Viewer** — full‑screen pager for photos and videos:
  - Photos load a cached thumbnail instantly, then the high‑res preview.
  - Videos play via Media3/ExoPlayer (server decides direct‑play vs transcode), with a transient scrub bar, buffering indicator, and duration badges on tiles. HDR (HLG/PQ) plays with true passthrough, and the metadata panel reports the dynamic range.
  - **OK** on a photo opens a metadata panel (date, location, resolution, camera, lens, exposure, people); **←/→** page; **hold ←/→** seeks video. Opened from the timeline, paging continues seamlessly across month boundaries.
  - **Motion Photos** (Samsung/Apple/Google live photos) show a **LIVE** badge and play their motion clip on demand with **OK**.
  - First‑open control hints, and a transient position indicator while paging.
- **Slideshow** — interval slideshow from the viewer (Play/Pause key), with configurable interval and shuffle.
- **Screensaver** — a `DreamService` that crossfades through photos sampled from across the whole library.
- **Insecure mode (opt‑in)** — trust self‑signed certificates / plain `http://` for self‑hosted servers without a valid certificate.

Every screen is fully operable with **D‑pad + OK + Back**.

## Read‑only by design

The app **cannot modify anything** on your server — no upload, delete, favorite, or edit. This is enforced at the network layer by a `ReadOnlyInterceptor` (unit‑tested) that blocks every non‑`GET`/`HEAD` request except two known read‑only `POST`s (token validation, metadata search). Recommended: pair it with a **read‑only Immich API key** for defense in depth.

## Security

The API key is a bearer credential and is handled accordingly:

- **Encrypted at rest** with a non‑exportable Android Keystore AES‑GCM key.
- **Host‑scoped** — the key is attached (via a network interceptor) only to the configured server's origin, never leaked across redirects.
- **Backup disabled**, key field **masked**, auth header **redacted** from logs.

## Tech stack

Kotlin · Jetpack Compose (`androidx.tv:tv-material`) · Media3 ExoPlayer · Coil 3 · Retrofit + kotlinx.serialization · OkHttp (shared by Retrofit, Coil, and ExoPlayer) · Hilt · DataStore · MVVM + repository + Flows. `minSdk 26`, `targetSdk 35`.

The API layer is **hand‑written** for the ~dozen endpoints in use (not generated from the OpenAPI spec). Tested against **Immich v3.x**, whose timeline API returns a compact columnar (structure‑of‑arrays) payload.

## Build & install

Requires the Android SDK and a JDK 17.

```bash
# Debug
./gradlew :app:assembleDebug

# Signed release (needs keystore.properties + a keystore — see below)
./gradlew :app:assembleRelease
```

Sideload to a Google TV device over the network:

```bash
adb connect <device-ip>:5555
adb install -r app/build/outputs/apk/release/app-release.apk
```

Launch **TV client for Immich** and enter your **server URL** and **API key** on the setup screen.

### Release signing

Create a keystore and a non‑committed `keystore.properties` at the repo root:

```properties
storeFile=/absolute/path/to/immich-tv-release.jks
storePassword=…
keyAlias=…
keyPassword=…
```

`keystore.properties`, `local.properties`, and `*.jks` are git‑ignored — never commit them.

## Status

Actively used on a Google TV Streamer 4K against a real ~14k‑asset library. All the core screens — Home, timeline, albums & people, smart search, the photo/video viewer, and slideshow — are built and working, along with light/dark themes and hardware‑decoded playback (HDR included).

It's a personal project, so there's no formal release process, versioned support, or issue triage — expect the occasional rough edge, and note a few things are inherently device‑dependent:

- **Screensaver** — can only be exercised on real hardware (Settings → System → Screensaver → *TV client for Immich*).
- **Smart search** — requires machine learning enabled on your Immich server.
- **HDR passthrough** — depends on your TV and display pipeline.

## License

[GNU AGPL‑3.0](LICENSE) © 2026 Alejandro Cubero — the same license as the [Immich](https://immich.app) project this client is built for.

This is an **unofficial** third‑party client and is **not affiliated with, endorsed by, or sponsored by the Immich project**. "Immich" and the Immich logo are property of their respective owners; this project uses its own icon and only references the Immich name descriptively.

## Acknowledgements

Built on top of the excellent [Immich](https://immich.app) project.
