# Privacy Policy — TV client for Immich

_Effective date: October 25, 2026_

TV client for Immich ("the app") is an unofficial, open-source Android TV / Google TV app for viewing photos and videos stored on an [Immich](https://immich.app) server that **you** run or have access to. This policy explains what the app does with your data. In short: **the developer collects nothing.** The app talks only to the server you set up, and it can only read from it.

## What the developer collects

**Nothing.** The app contains no analytics, advertising, crash-reporting or tracking code, and it never sends any data to the developer or to any third party. The developer has no server and no way to see who uses the app or how.

## What the app sends, and where

The app connects **only to the Immich server address you enter** during setup. It sends:

- **Your Immich API key**, attached to requests to that server so the server knows the request is yours. The key is sent only to that exact server address, never to any other host — including when the server redirects elsewhere.
- **Requests to view your library**, such as lists of albums or memories, and thumbnails, photos and videos. The app is read-only: it can't upload, delete, edit or favorite anything, and this is enforced in the app's network layer.

Your Immich server is operated by you or by whoever hosts it, and its own policies apply to data stored there. The app sends nothing anywhere else.

## What the app stores on your device

- **Settings:** your server address, your theme, text size, language and slideshow preferences, whether unencrypted (`http://`) connections are allowed, and the fingerprint of a server certificate you chose to trust.
- **Your API key**, encrypted with a key held in the Android Keystore that can't be exported from the device.
- **An image cache** of thumbnails and previews from your library, up to 512 MB, so scrolling is fast.

This data stays on the device. App data backups are disabled, so it isn't copied to cloud backups.

## Security

- Connections use HTTPS by default.
- If your server uses a self-signed certificate, the app shows you the certificate and its fingerprint, and only connects after you explicitly trust it. From then on, it accepts that exact certificate only.
- Unencrypted `http://` connections are only possible if you turn on **Allow unencrypted connection** during setup. In that case, your API key and photos travel unencrypted on your network.

## Permissions

The app uses **network access** (to reach your server and check connectivity). It doesn't request access to your camera, microphone, location, contacts or files. The optional screensaver is a standard Android screensaver that the system starts.

## Deleting your data

- **Settings → Logout** removes your server address, API key, settings and image cache from the device.
- **Uninstalling** the app removes everything it stored.

Since the developer holds no data about you, there's nothing to request deletion of.

## Children

The app isn't directed at children, and it doesn't knowingly collect data from anyone.

## Changes

Changes to this policy will be published in this file in the app's source repository, with a new effective date.

## Contact

Questions about this policy: open an issue at <https://github.com/ale-cubero/google-tv-client-for-immich/issues>, or use the developer contact email shown on the app's Google Play listing.
